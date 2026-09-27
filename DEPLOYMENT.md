# Deploy StudyFlow for access from anywhere

This deployment creates one public HTTPS website for administrators, mentors, and students. Users can open it from a phone, tablet, or computer without being on your Wi-Fi.

## Before deployment

- Push this project to `https://github.com/MuhammedIhsan07/StudyFlow`.
- Create a Render account at `https://dashboard.render.com` and connect the GitHub account that owns the repository.
- Review Render's price before confirming. StudyFlow uses a paid `0.5c-512mb` web service and a 1 GB persistent disk so accounts and study progress survive restarts and deployments.

Do not remove the persistent disk. A free web service has an ephemeral filesystem and would eventually lose the embedded database.

## Create the website

1. Open the Render Dashboard.
2. Choose **New > Blueprint**.
3. Select the `MuhammedIhsan07/StudyFlow` repository.
4. Keep `render.yaml` as the Blueprint path.
5. Review the web-service and persistent-disk charges, then create the Blueprint.
6. Wait until the deployment status becomes **Live**.
7. Open the HTTPS address shown by Render, such as `https://studyflow-xxxx.onrender.com`.

Render supplies the public port and HTTPS address automatically. The application binds to `0.0.0.0`, uses Render's assigned port, and stores its H2 database under `/var/data` on the persistent disk.

## First administrator

On the first deployment, open the service's **Logs** tab and find:

```text
First run: open the app and create your administrator account.
Setup key: ...
```

1. Copy the setup key.
2. Open the public StudyFlow URL.
3. Enter your name, email, a strong password, and the setup key.
4. Store the administrator password securely.

The setup screen is disabled permanently after the first administrator is created.

## Add users

1. Sign in as the administrator.
2. Add mentors and students from the dashboard.
3. Assign each student to a mentor.
4. Copy each generated temporary password and share it privately with that person.
5. Users must choose a new password at their first sign-in.

Send students and mentors only the public HTTPS link. Never send them the setup key or administrator credentials.

## Updates and backups

Render rebuilds StudyFlow after new commits are pushed to the connected branch. Application data remains on the persistent disk.

Render creates persistent-disk snapshots, but important academic data should also have an independent backup policy before production use. Never commit the `data` folder or a database file to GitHub.

## Custom domain

The generated `onrender.com` address works immediately. A custom domain can be added later in Render. When using a custom domain, set the `STUDYFLOW_ORIGIN` environment variable to the exact HTTPS origin, for example:

```text
https://studyflow.yourschool.edu
```

Redeploy the service after changing that setting.
