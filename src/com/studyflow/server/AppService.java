package com.studyflow.server;

import com.google.gson.JsonObject;
import com.studyflow.model.*;
import java.sql.SQLException;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import static com.studyflow.server.ApiException.require;
import static com.studyflow.server.Inputs.*;

/** All authorization and business rules live on the server, independent of the UI. */
final class AppService {
    record Reply(Object body, int status, String sessionToken) {
        static Reply ok(Object body) { return new Reply(body, 200, null); }
    }
    private static final String USER_FIELDS = "id,name,email,role,active,must_change_password,program,daily_goal,preferred_time,mentor_id,created_at";
    private final Database db;
    private final String setupToken;
    private final String dummyHash = Passwords.hash(Passwords.token());
    AppService(Database db, String setupToken) { this.db = db; this.setupToken = setupToken; }

    Reply handle(String method, String path, JsonObject body, String token, String csrf) throws Exception {
        return db.transaction(() -> route(method, path, body, token, csrf));
    }
    boolean setupRequired() throws Exception {
        return db.transaction(() -> db.one("SELECT id FROM users LIMIT 1") == null);
    }
    private Reply route(String method, String path, JsonObject body, String token, String csrf) throws Exception {
        if (method.equals("GET") && path.equals("/api/status"))
            return Reply.ok(Map.of("setup_required", db.one("SELECT id FROM users LIMIT 1") == null,
                    "today", LocalDate.now().toString(), "timezone", ZoneId.systemDefault().getId()));
        if (method.equals("POST") && path.equals("/api/setup")) {
            require(db.one("SELECT id FROM users LIMIT 1") == null, 409, "Setup is already complete. Sign in instead.");
            require(Passwords.equal(optional(body, "setup_token", ""), setupToken), 403, "The setup key is incorrect. Copy it from the server terminal.");
            String password = optional(body, "password", "");
            Passwords.validate(password);
            String id = createAccount(text(body, "name", 100), email(body), password, "ADMIN", "", null, false);
            audit(id, "workspace.created", id);
            return newSession(id, token);
        }
        if (method.equals("POST") && path.equals("/api/auth/login")) {
            String email = email(body);
            String password = optional(body, "password", "");
            require(password.length() <= 128, 400, "Password is too long.");
            Map<String, Object> account = db.one("SELECT id,password_hash,active FROM users WHERE email=?", email);
            boolean valid = Passwords.verify(password, account == null ? dummyHash : str(account, "password_hash"));
            require(valid && account != null && yes(account, "active"), 401, "Email or password is incorrect, or the account is inactive.");
            return newSession(str(account, "id"), token);
        }
        Map<String, Object> session = token == null ? null : db.one(
                "SELECT * FROM sessions WHERE token_hash=? AND expires_at>?", Passwords.digest(token), System.currentTimeMillis());
        require(session != null, 401, "Please sign in to continue.");
        Map<String, Object> actor = user(str(session, "user_id"));
        require(actor != null && yes(actor, "active"), 401, "Your session has ended. Please sign in again.");
        String actorId = str(actor, "id");
        String role = str(actor, "role");
        if (!method.equals("GET")) require(Passwords.equal(csrf, str(session, "csrf_token")), 403, "Refresh the page and try again.");
        if (method.equals("GET") && path.equals("/api/me"))
            return Reply.ok(Map.of("user", actor, "csrf_token", session.get("csrf_token"), "today", LocalDate.now().toString()));
        if (method.equals("POST") && path.equals("/api/auth/logout")) {
            db.execute("DELETE FROM sessions WHERE token_hash=?", Passwords.digest(token));
            return new Reply(Map.of("ok", true), 200, "");
        }
        if (method.equals("POST") && path.equals("/api/me/password")) {
            String old = optional(body, "current_password", "");
            String password = optional(body, "password", "");
            require(old.length() <= 128, 400, "Password is too long.");
            require(Passwords.verify(old, str(db.one("SELECT password_hash FROM users WHERE id=?", actorId), "password_hash")),
                    400, "Your current password is incorrect.");
            Passwords.validate(password);
            require(!old.equals(password), 400, "Choose a different password.");
            db.execute("UPDATE users SET password_hash=?,must_change_password=FALSE WHERE id=?", Passwords.hash(password), actorId);
            db.execute("DELETE FROM sessions WHERE user_id=?", actorId);
            audit(actorId, "password.changed", actorId);
            return newSession(actorId, token);
        }
        require(!yes(actor, "must_change_password"), 403, "Set your own password before using your workspace.");
        if (method.equals("PUT") && path.equals("/api/me")) {
            String program = optional(body, "program", "").trim();
            require(program.length() <= 120, 400, "Program must be at most 120 characters.");
            db.execute("UPDATE users SET name=?,program=?,daily_goal=?,preferred_time=? WHERE id=?",
                    text(body, "name", 100), program, number(body, "daily_goal", 15, 720),
                    choice(body, "preferred_time", "MORNING", "AFTERNOON", "EVENING"), actorId);
            return Reply.ok(user(actorId));
        }
        if (path.equals("/api/users")) {
            require(!role.equals("STUDENT"), 403, "Only staff can manage accounts.");
            if (method.equals("GET")) {
                List<Map<String, Object>> accounts = role.equals("ADMIN")
                        ? db.query("SELECT " + USER_FIELDS + " FROM users ORDER BY name")
                        : db.query("SELECT " + USER_FIELDS + " FROM users WHERE role='STUDENT' AND mentor_id=? ORDER BY name", actorId);
                for (Map<String, Object> account : accounts) if (str(account, "role").equals("STUDENT")) account.put("stats", stats(str(account, "id")));
                return Reply.ok(accounts);
            }
            if (method.equals("POST")) {
                String newRole = choice(body, "role", "STUDENT", "MENTOR", "ADMIN");
                require(role.equals("ADMIN") || newRole.equals("STUDENT"), 403, "Mentors can add students only.");
                String mentorId = role.equals("MENTOR") ? actorId : mentor(body, newRole);
                String password = optional(body, "password", "");
                if (password.isBlank()) password = Passwords.token().substring(0, 20);
                Passwords.validate(password);
                String program = optional(body, "program", "").trim();
                require(program.length() <= 120, 400, "Program must be at most 120 characters.");
                String id = createAccount(text(body, "name", 100), email(body), password, newRole, program, mentorId, true);
                audit(actorId, "account.created", id);
                return new Reply(Map.of("user", user(id), "temporary_password", password), 201, null);
            }
        }
        if (path.startsWith("/api/users/")) {
            require(role.equals("ADMIN"), 403, "Only an administrator can change accounts.");
            String[] parts = path.split("/");
            require(parts.length == 4 || parts.length == 5, 404, "Page not found.");
            String id = parts[3];
            Map<String, Object> target = user(id);
            require(target != null, 404, "Account not found.");
            if (parts.length == 5 && parts[4].equals("reset-password") && method.equals("POST")) {
                require(!id.equals(actorId), 400, "Change your own password in Settings.");
                String password = Passwords.token().substring(0, 20);
                db.execute("UPDATE users SET password_hash=?,must_change_password=TRUE WHERE id=?", Passwords.hash(password), id);
                db.execute("DELETE FROM sessions WHERE user_id=?", id);
                audit(actorId, "password.reset", id);
                return Reply.ok(Map.of("temporary_password", password, "user", user(id)));
            }
            if (parts.length == 4 && method.equals("PUT")) {
                boolean active = bool(body, "active");
                require(active || !id.equals(actorId), 400, "You cannot deactivate your own account.");
                String email = email(body);
                require(db.one("SELECT id FROM users WHERE email=? AND id<>?", email, id) == null, 409, "This email is already in use.");
                String mentorId = mentor(body, str(target, "role"));
                String program = optional(body, "program", "").trim();
                require(program.length() <= 120, 400, "Program must be at most 120 characters.");
                db.execute("UPDATE users SET name=?,email=?,program=?,active=?,mentor_id=? WHERE id=?",
                        text(body, "name", 100), email, program, active, mentorId, id);
                if (!active || !email.equals(str(target, "email"))) db.execute("DELETE FROM sessions WHERE user_id=?", id);
                audit(actorId, "account.updated", id);
                return Reply.ok(user(id));
            }
        }
        if (method.equals("GET") && path.equals("/api/audit")) {
            require(role.equals("ADMIN"), 403, "Only an administrator can view account activity.");
            return Reply.ok(db.query("SELECT a.*,u.name AS actor_name,t.name AS target_name FROM audit_events a LEFT JOIN users u ON u.id=a.actor_id LEFT JOIN users t ON t.id=a.target_id ORDER BY a.created_at DESC LIMIT 100"));
        }
        if (path.startsWith("/api/students/")) return planner(method, path, body, actor);
        throw new ApiException(404, "Page not found.");
    }

    private String createAccount(String name, String email, String password, String role, String program, String mentor, boolean change) throws SQLException {
        require(db.one("SELECT id FROM users WHERE email=?", email) == null, 409, "This email is already in use.");
        String id = UUID.randomUUID().toString();
        db.execute("INSERT INTO users(id,name,email,password_hash,role,program,mentor_id,must_change_password,created_at) VALUES(?,?,?,?,?,?,?,?,?)",
                id, name, email, Passwords.hash(password), role, program, mentor, change, Instant.now().toString());
        return id;
    }
    private String mentor(JsonObject body, String role) throws SQLException {
        String id = optional(body, "mentor_id", "");
        if (!role.equals("STUDENT") || id.isBlank()) return null;
        require(db.one("SELECT id FROM users WHERE id=? AND role='MENTOR' AND active=TRUE", id) != null,
                400, "Choose an active mentor.");
        return id;
    }
    private Map<String, Object> user(String id) throws SQLException { return db.one("SELECT " + USER_FIELDS + " FROM users WHERE id=?", id); }
    private Reply newSession(String id, String previous) throws SQLException {
        if (previous != null) db.execute("DELETE FROM sessions WHERE token_hash=?", Passwords.digest(previous));
        db.execute("DELETE FROM sessions WHERE expires_at<?", System.currentTimeMillis());
        // Keep at most five active sessions per account.
        List<Map<String, Object>> existing = db.query("SELECT token_hash FROM sessions WHERE user_id=? ORDER BY expires_at DESC", id);
        for (int i = 4; i < existing.size(); i++) db.execute("DELETE FROM sessions WHERE token_hash=?", existing.get(i).get("token_hash"));
        String token = Passwords.token();
        String csrf = Passwords.token();
        db.execute("INSERT INTO sessions(token_hash,user_id,csrf_token,expires_at) VALUES(?,?,?,?)",
                Passwords.digest(token), id, csrf, System.currentTimeMillis() + Duration.ofHours(12).toMillis());
        return new Reply(Map.of("user", user(id), "csrf_token", csrf, "today", LocalDate.now().toString()), 200, token);
    }
    private void audit(String actor, String action, String target) throws SQLException {
        db.execute("INSERT INTO audit_events(id,actor_id,action,target_id,created_at) VALUES(?,?,?,?,?)",
                UUID.randomUUID().toString(), actor, action, target, Instant.now().toString());
    }
    static String str(Map<String, Object> row, String key) { return Objects.toString(row.get(key), ""); }
    static boolean yes(Map<String, Object> row, String key) { return Boolean.TRUE.equals(row.get(key)); }
    static int num(Map<String, Object> row, String key) { return ((Number) row.get(key)).intValue(); }

    // Planner methods are below. All are called within the same request transaction.
    private Reply planner(String method, String path, JsonObject body, Map<String, Object> actor) throws Exception {
        String[] parts = path.split("/");
        require(parts.length >= 5 && parts.length <= 7, 404, "Page not found.");
        String studentId = parts[3];
        Map<String, Object> student = user(studentId);
        String role = str(actor, "role");
        String actorId = str(actor, "id");
        require(student != null && str(student, "role").equals("STUDENT")
                && (role.equals("ADMIN") || role.equals("STUDENT") && actorId.equals(studentId)
                || role.equals("MENTOR") && actorId.equals(str(student, "mentor_id"))), 404, "Student not found.");
        if (parts[4].equals("planner") && parts.length == 5 && method.equals("GET")) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("student", student);
            result.put("subjects", subjects(studentId));
            result.put("tasks", tasks(studentId));
            result.put("stats", stats(studentId));
            result.put("notes", db.query("SELECT n.*,u.name AS author_name FROM notes n JOIN users u ON u.id=n.author_id WHERE n.student_id=?"
                    + (role.equals("STUDENT") ? " AND n.shared=TRUE" : "") + " ORDER BY n.created_at DESC", studentId));
            Map<String, Object> mentor = student.get("mentor_id") == null ? null : user(str(student, "mentor_id"));
            result.put("mentor", mentor == null ? null : Map.of("name", mentor.get("name"), "email", mentor.get("email"), "active", mentor.get("active")));
            result.put("today", LocalDate.now().toString());
            return Reply.ok(result);
        }
        require(yes(student, "active"), 409, "Reactivate this student before changing their planner.");
        String id = parts.length >= 6 ? parts[5] : "";
        if (parts[4].equals("subjects") && parts.length <= 6) {
            if (method.equals("POST") && parts.length == 5 || method.equals("PUT") && parts.length == 6) {
                if (method.equals("PUT")) require(db.one("SELECT id FROM subjects WHERE id=? AND student_id=?", id, studentId) != null, 404, "Subject not found.");
                else id = UUID.randomUUID().toString();
                String code = text(body, "code", 20).toUpperCase(Locale.ROOT);
                require(db.one("SELECT id FROM subjects WHERE student_id=? AND code=? AND id<>?", studentId, code, id) == null, 409, "You already have a subject with this code.");
                String color = text(body, "color", 7);
                require(color.matches("#[0-9a-fA-F]{6}"), 400, "Choose a valid subject color.");
                String name = text(body, "name", 100);
                int goal = number(body, "weekly_goal", 15, 3000);
                if (method.equals("POST")) db.execute("INSERT INTO subjects(id,student_id,name,code,color,weekly_goal) VALUES(?,?,?,?,?,?)", id, studentId, name, code, color, goal);
                else db.execute("UPDATE subjects SET name=?,code=?,color=?,weekly_goal=? WHERE id=? AND student_id=?", name, code, color, goal, id, studentId);
                return Reply.ok(Map.of("id", id));
            }
            if (method.equals("DELETE") && parts.length == 6) {
                require(db.one("SELECT id FROM tasks WHERE subject_id=? AND student_id=? LIMIT 1", id, studentId) == null, 409,
                        "This subject has tasks. Move or delete those tasks before deleting the subject.");
                require(db.execute("DELETE FROM subjects WHERE id=? AND student_id=?", id, studentId) == 1, 404, "Subject not found.");
                return Reply.ok(Map.of("ok", true));
            }
        }
        if (parts[4].equals("tasks")) {
            if (method.equals("POST") && parts.length == 5) return saveTask(body, studentId, actorId, null);
            Map<String, Object> task = db.one("SELECT * FROM tasks WHERE id=? AND student_id=?", id, studentId);
            require(task != null, 404, "Task not found.");
            if (parts.length == 6 && method.equals("PUT")) return saveTask(body, studentId, actorId, task);
            if (parts.length == 6 && method.equals("DELETE")) {
                db.execute("DELETE FROM tasks WHERE id=? AND student_id=?", id, studentId);
                return Reply.ok(Map.of("ok", true));
            }
            if (parts.length == 7 && method.equals("POST")) {
                require(number(body, "version", 1, Integer.MAX_VALUE) == num(task, "version"), 409, "This task changed. Refresh before trying again.");
                if (parts[6].equals("complete")) {
                    boolean completed = bool(body, "completed");
                    if (completed != str(task, "status").equals("COMPLETED")) {
                        db.execute("UPDATE tasks SET status=?,completed_date=?,version=version+1 WHERE id=?",
                                completed ? "COMPLETED" : "SCHEDULED", completed ? LocalDate.now().toString() : null, id);
                    }
                    return Reply.ok(Map.of("ok", true));
                }
                if (parts[6].equals("reschedule")) return reschedule(task, student);
            }
        }
        if (parts[4].equals("notes")) {
            require(!role.equals("STUDENT"), 403, "Only staff can write mentor notes.");
            if (method.equals("POST") && parts.length == 5) {
                String noteId = UUID.randomUUID().toString();
                db.execute("INSERT INTO notes(id,student_id,author_id,body,shared,created_at) VALUES(?,?,?,?,?,?)", noteId, studentId,
                        actorId, text(body, "body", 3000), bool(body, "shared"), Instant.now().toString());
                return new Reply(Map.of("id", noteId), 201, null);
            }
            if (method.equals("DELETE") && parts.length == 6) {
                Map<String, Object> note = db.one("SELECT author_id FROM notes WHERE id=? AND student_id=?", id, studentId);
                require(note != null && (role.equals("ADMIN") || actorId.equals(str(note, "author_id"))), 404, "Note not found.");
                db.execute("DELETE FROM notes WHERE id=? AND student_id=?", id, studentId);
                return Reply.ok(Map.of("ok", true));
            }
        }
        throw new ApiException(404, "Page not found.");
    }

    private Reply saveTask(JsonObject body, String studentId, String actorId, Map<String, Object> old) throws SQLException {
        String subjectId = text(body, "subject_id", 36);
        require(db.one("SELECT id FROM subjects WHERE id=? AND student_id=?", subjectId, studentId) != null, 400, "Choose one of this student's subjects.");
        String title = text(body, "title", 180);
        String type = choice(body, "task_type", "STUDY", "REVISION", "ASSIGNMENT", "EXAM");
        String priority = choice(body, "priority", "LOW", "MEDIUM", "HIGH");
        String scheduled = date(body, "scheduled_date");
        String time = time(body);
        int duration = number(body, "duration", 15, 480);
        require(LocalTime.parse(time).toSecondOfDay() / 60 + duration <= 1440, 400, "A study session must finish before midnight.");
        String deadline = optional(body, "deadline", "").isBlank() ? null : date(body, "deadline");
        require(deadline == null || scheduled.compareTo(deadline) <= 0, 400, "Schedule this session on or before its deadline.");
        boolean adaptive = bool(body, "adaptive");
        String id = old == null ? UUID.randomUUID().toString() : str(old, "id");
        if (old == null) db.execute("INSERT INTO tasks(id,student_id,subject_id,title,task_type,priority,scheduled_date,start_time,duration,deadline,adaptive,status,created_by,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,'SCHEDULED',?,?)",
                id, studentId, subjectId, title, type, priority, scheduled, time, duration, deadline, adaptive, actorId, Instant.now().toString());
        else {
            require(number(body, "version", 1, Integer.MAX_VALUE) == num(old, "version"), 409, "This task changed. Refresh before saving your changes.");
            db.execute("UPDATE tasks SET subject_id=?,title=?,task_type=?,priority=?,scheduled_date=?,start_time=?,duration=?,deadline=?,adaptive=?,version=version+1 WHERE id=? AND student_id=?",
                    subjectId, title, type, priority, scheduled, time, duration, deadline, adaptive, id, studentId);
        }
        return new Reply(Map.of("id", id), old == null ? 201 : 200, null);
    }
    private Reply reschedule(Map<String, Object> task, Map<String, Object> student) throws SQLException {
        require(yes(task, "adaptive"), 409, "Adaptive scheduling is off for this task. Edit it to move the session manually.");
        require(str(task, "status").equals("SCHEDULED"), 409, "Completed tasks cannot be rescheduled.");
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(14);
        if (task.get("deadline") != null && LocalDate.parse(str(task, "deadline")).isBefore(end)) end = LocalDate.parse(str(task, "deadline"));
        int duration = num(task, "duration");
        require(duration <= num(student, "daily_goal"), 409, "This task is longer than your daily goal. Shorten it or adjust your goal first.");
        int preferred = switch (str(student, "preferred_time")) { case "MORNING" -> 8 * 60; case "AFTERNOON" -> 13 * 60; default -> 17 * 60; };
        List<Map<String, Object>> all = tasks(str(student, "id"));
        for (LocalDate day = today; !day.isAfter(end); day = day.plusDays(1)) {
            List<Map<String, Object>> scheduled = new ArrayList<>();
            int planned = 0;
            for (Map<String, Object> item : all) if (!str(item, "id").equals(str(task, "id")) && str(item, "scheduled_date").equals(day.toString())) {
                planned += num(item, "duration");
                scheduled.add(item);
            }
            if (planned + duration > num(student, "daily_goal")) continue;
            List<Integer> starts = new ArrayList<>();
            for (int start = preferred; start + duration <= 22 * 60; start += 15) starts.add(start);
            for (int start = 7 * 60; start < preferred && start + duration <= 22 * 60; start += 15) starts.add(start);
            for (int start : starts) {
                if (day.equals(today) && start <= LocalTime.now().toSecondOfDay() / 60) continue;
                if (day.toString().equals(str(task, "scheduled_date")) && start == LocalTime.parse(str(task, "start_time")).toSecondOfDay() / 60) continue;
                boolean overlap = false;
                for (Map<String, Object> item : scheduled) {
                    int occupied = LocalTime.parse(str(item, "start_time")).toSecondOfDay() / 60;
                    if (start < occupied + num(item, "duration") && occupied < start + duration) { overlap = true; break; }
                }
                if (!overlap) {
                    String time = String.format(Locale.ROOT, "%02d:%02d", start / 60, start % 60);
                    db.execute("UPDATE tasks SET scheduled_date=?,start_time=?,version=version+1 WHERE id=?", day.toString(), time, task.get("id"));
                    return Reply.ok(Map.of("scheduled_date", day.toString(), "start_time", time));
                }
            }
        }
        throw new ApiException(409, "No free slot fits your daily goal before the deadline (within 14 days). Adjust the task or your study goal manually.");
    }
    private List<Map<String, Object>> subjects(String studentId) throws SQLException {
        List<Map<String, Object>> result = db.query("SELECT * FROM subjects WHERE student_id=? ORDER BY name", studentId);
        String monday = monday().toString();
        String sunday = monday().plusDays(6).toString();
        for (Map<String, Object> subject : result) {
            Map<String, Object> amount = db.one("SELECT COALESCE(SUM(duration),0) AS minutes FROM tasks WHERE subject_id=? AND student_id=? AND status='COMPLETED' AND completed_date BETWEEN ? AND ?",
                    subject.get("id"), studentId, monday, sunday);
            subject.put("weekly_minutes", amount.get("minutes"));
        }
        return result;
    }
    private List<Map<String, Object>> tasks(String studentId) throws SQLException {
        List<Map<String, Object>> result = db.query("SELECT t.*,s.name AS subject_name,s.code AS subject_code,s.color AS subject_color,u.name AS creator_name FROM tasks t JOIN subjects s ON s.id=t.subject_id JOIN users u ON u.id=t.created_by WHERE t.student_id=? ORDER BY t.scheduled_date,t.start_time,t.id", studentId);
        for (Map<String, Object> task : result) {
            // Reuse the original project's task polymorphism for recommendations.
            StudyTask model = TaskFactory.create(TaskType.valueOf(str(task, "task_type")), str(task, "title"), str(task, "subject_id"),
                    LocalDate.parse(str(task, "scheduled_date")), LocalTime.parse(str(task, "start_time")), num(task, "duration"),
                    TaskPriority.valueOf(str(task, "priority")), yes(task, "adaptive"));
            task.put("urgency", model.getUrgencyScore(LocalDate.now()));
            task.put("overdue", str(task, "status").equals("SCHEDULED") && model.getDueDateTime().isBefore(LocalDateTime.now()));
        }
        return result;
    }
    private Map<String, Object> stats(String studentId) throws SQLException {
        List<Map<String, Object>> tasks = db.query("SELECT status,duration,completed_date,scheduled_date,start_time,deadline FROM tasks WHERE student_id=?", studentId);
        int complete = 0, weekly = 0, overdue = 0, todayPlanned = 0;
        List<Integer> daily = new ArrayList<>(Collections.nCopies(7, 0));
        LocalDate today = LocalDate.now(), monday = monday();
        for (Map<String, Object> task : tasks) {
            int minutes = num(task, "duration");
            if (str(task, "scheduled_date").equals(today.toString())) todayPlanned += minutes;
            if (str(task, "status").equals("COMPLETED")) {
                complete++;
                LocalDate completed = LocalDate.parse(str(task, "completed_date"));
                long offset = java.time.temporal.ChronoUnit.DAYS.between(monday, completed);
                if (offset >= 0 && offset < 7) { weekly += minutes; daily.set((int) offset, daily.get((int) offset) + minutes); }
            } else if (LocalDateTime.of(LocalDate.parse(str(task, "scheduled_date")), LocalTime.parse(str(task, "start_time"))).isBefore(LocalDateTime.now())) overdue++;
        }
        return Map.of("total", tasks.size(), "completed", complete, "open", tasks.size() - complete, "overdue", overdue,
                "completion_rate", tasks.isEmpty() ? 0 : Math.round(complete * 100.0 / tasks.size()), "weekly_minutes", weekly,
                "today_planned", todayPlanned, "daily_minutes", daily, "week_start", monday.toString());
    }
    private static LocalDate monday() { return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); }
}
