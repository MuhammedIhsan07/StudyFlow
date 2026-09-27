# StudyFlow

StudyFlow is a complete adaptive study-planning application built with Java. Its main interface is the original professional Java Swing desktop design with the open-book theme. It includes persistent student, mentor, and administrator accounts and private planner data for every student.

The browser version is backed by a Java HTTP server and H2 SQL database. It can be deployed with HTTPS so students and mentors can sign in from anywhere.

## Public website deployment

The repository includes a production `Dockerfile` and `render.yaml` Blueprint. Follow [DEPLOYMENT.md](DEPLOYMENT.md) to create a permanent Render website with an HTTPS `onrender.com` address and persistent student data.

## Start the app

Open PowerShell in this folder and run:

```powershell
.\run.ps1
```

On the first run, the launcher downloads a private portable Microsoft OpenJDK 21 and the two verified Java libraries used by the app. Nothing needs to be installed system-wide.

The original Swing login screen opens automatically. Use one of the included accounts, then add real student and mentor accounts from the staff dashboard.

| Role | Email | Password |
| --- | --- | --- |
| Student | `student@studyflow.com` | `student123` |
| Mentor | `mentor@studyflow.com` | `mentor123` |
| Administrator | `admin@studyflow.com` | `admin123` |

The administrator can add mentors and students. Mentors can add students, but cannot create other staff accounts.

## Accounts and permissions

| Role | Access |
| --- | --- |
| Student | Uses their own subjects, study sessions, adaptive schedule, progress, shared mentor notes, and profile. |
| Mentor | Adds students to their cohort, views only assigned students, works with their plans, and leaves shared or private notes. |
| Administrator | Adds students, mentors, or other administrators; assigns mentors; manages access; resets passwords; and reviews account activity. |

New accounts receive a generated temporary password. It is shown once to the staff member creating the account, and the new user must replace it at first sign-in.

## Main features

- Secure role-based sign-in with salted PBKDF2 password hashes
- HTTP-only sessions, CSRF protection, login throttling, and security headers
- Persistent account and per-student planner storage
- Student, mentor, and administrator account management
- Mentor-to-student assignment and permission isolation
- Subjects with individual weekly study goals
- Study, revision, assignment, and exam sessions
- Completion tracking, overdue warnings, weekly analytics, and progress charts
- Adaptive rescheduling around daily goals, occupied times, and deadlines
- Shared and staff-only mentor notes
- Password reset, forced first-login password change, and account deactivation
- Responsive professional interface in brown, charcoal, beige, white, and sage green
- Open-book visual theme throughout the login and workspace experience

## Useful commands

Compile without starting the server:

```powershell
.\run.ps1 -CompileOnly
```

Use another port:

```powershell
.\run.ps1 -Web -Port 9090
```

Start the optional browser/server version:

```powershell
.\run.ps1 -Web
```

To use the responsive website on phones and laptops connected to the same Wi-Fi, double-click `start-lan.bat` or run:

```powershell
.\run.ps1 -Lan
```

The launcher detects the computer's Wi-Fi address and prints the exact link to open on every device. Keep the server window open while StudyFlow is in use.

For a trusted local network demonstration, bind to all interfaces and set the URL that users will open:

```powershell
.\run.ps1 -Web -BindAddress 0.0.0.0 -PublicUrl http://192.168.1.20:8080
```

Replace the example IP with the computer's LAN address. Windows Firewall may ask for permission. Do not expose the built-in server directly to the public internet; deploy behind HTTPS and a production reverse proxy for public use.

## Data and backup

The Swing app stores accounts and individual planner files in the current Windows user's `.studyflow` folder. The optional web application stores its database in `data/studyflow.mv.db`. The web data folder is excluded from Git so real accounts and passwords are never committed.

To back up the app, stop the server and copy the `data` folder to a secure location. You can select another storage folder with:

```powershell
.\run.ps1 -Web -DataDirectory D:\StudyFlowData
```

## Project structure

```text
src/com/studyflow/server/   Java HTTP API, security, business rules, and SQL storage
src/com/studyflow/model/    OOP domain models and polymorphic task types
src/com/studyflow/service/  Desktop application services and persistence
src/com/studyflow/ui/       Optional Java Swing desktop interface
web/                        Responsive browser interface
Dockerfile                  Reproducible production container
render.yaml                 Render service and persistent-disk configuration
run.ps1                     Dependency setup, compilation, and launcher
```

## OOP design

- Encapsulation keeps database access, authentication, scheduling, and UI behavior in separate components.
- Inheritance is demonstrated by `ExamTask`, `AssignmentTask`, and `RevisionTask` extending `StudyTask`.
- Polymorphism lets each task type contribute its own urgency behavior.
- Abstraction keeps HTTP routing, business rules, persistence, and presentation independent.
- Role objects and service boundaries enforce student, mentor, and administrator responsibilities.

Dependencies are downloaded from Maven Central and verified against the SHA-256 hashes in `dependencies.json`.
