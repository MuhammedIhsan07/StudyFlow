# StudyFlow - Adaptive Study Planner (Java)

StudyFlow is a professional Java Swing desktop frontend based on the Adaptive Study Planner abstract. It covers subjects, study tasks, weekly schedules, adaptive rescheduling, reminders, profile management, and progress analysis.

The application now has two role-separated workspaces:

- **Student:** creates an adaptive study plan, manages subjects and tasks, and tracks personal progress.
- **Mentor / Admin:** selects students, reviews progress and subject performance, checks engagement, and records mentor notes.

## Requirements

- Windows PowerShell
- Internet access on the first run if a Java JDK is not already installed

## Compile and run

From the project folder in PowerShell:

```powershell
.\run.ps1
```

The launcher first looks for Java on `PATH`, in `JAVA_HOME`, in common IDE/JDK locations, and in the project's `.tools` directory. If Java is missing, it downloads the official portable Microsoft OpenJDK 21, verifies its SHA-256 checksum, and keeps it inside this project. No system-wide Java installation or manual `PATH` configuration is required.

The script then compiles all Java source files with UTF-8 encoding and launches the application. If you already have a JDK, the equivalent manual commands are:

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object FullName)
java -cp out com.studyflow.AdaptiveStudyPlanner
```

To compile without opening the GUI, use `./run.ps1 -CompileOnly`.

## Features

- Separate Student and Mentor/Admin login flows
- Dedicated cohort and student-progress monitoring dashboard for mentors
- Modern Java Swing dashboard with professional custom styling
- Neutral visual palette using brown, charcoal, beige, white, and soft green
- Open-book workspace background with warm paper pages, a center spine, page shadows, and a sage bookmark
- Subject management with weekly study goals
- Task creation, completion, filtering, and deletion
- Adaptive one-click rescheduling for missed tasks
- Seven-day schedule and deadline views
- Weekly performance chart and subject progress analysis
- Editable student profile and study-hour preferences
- Local Java object persistence

## Demo logins

The login screen is pre-filled for quick demonstration. You can switch roles before signing in.

| Role | Email | Password |
| --- | --- | --- |
| Student | `student@studyflow.com` | `student123` |
| Mentor / Admin | `mentor@studyflow.com` | `mentor123` |

These are demonstration credentials for the frontend prototype, not production authentication.

## OOP concepts demonstrated

- **Classes and objects:** `StudentProfile`, `Subject`, `StudyTask`, and UI components
- **Encapsulation:** model fields are private and persistence is hidden behind `PlannerRepository`
- **Inheritance:** specialized `ExamTask`, `AssignmentTask`, and `RevisionTask` classes extend `StudyTask`
- **Polymorphism:** each task type overrides urgency and adaptive recommendation behavior
- **Abstraction:** `AdaptivePlannerService` exposes planner operations while hiding scheduling and persistence details
- **Modular design:** models, services, reusable UI components, and screens are separated by package

No third-party libraries or frameworks are required.
