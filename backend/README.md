# ScholarAssist submission guide

ScholarAssist has a Spring Boot 4 backend and a static HTML/CSS/JavaScript frontend. The backend uses MySQL, Flyway, JPA, and session-based Spring Security.

## Run locally

1. Start the MySQL `MySQL80` service and make sure the `scholarassist` database is available. Connection settings are in `src/main/resources/application.properties`; Flyway applies the project migrations at startup.
2. In a terminal opened in the backend project folder, run `.\mvnw.cmd spring-boot:run` in PowerShell. The backend is at `http://localhost:8082` by default.
3. In a second terminal, change to the frontend folder and run `python -m http.server 5500` (or start VS Code Live Server on port 5500). Open `http://localhost:5500/`.

Use the exact frontend origin `http://localhost:5500`; `127.0.0.1:5500` is a different origin and is not allowed by the development CORS configuration.

## Submission demo flow

1. Open `pages/register.html` to create a student account, then sign in at `pages/login.html`. If you already have an account, sign in directly instead.
2. The frontend requests CSRF tokens for state-changing requests and sends the browser-managed session cookie. Protected API requests require a signed-in session. Sign out from the workspace when finished.
3. Open Scholarships to browse the active catalog and choose a scholarship.
4. Use its quick eligibility check to answer the listed criteria. If the answers appear to match, choose **Apply now**; ScholarAssist starts or reuses a tracker and opens the provider's official site.
5. Create a student profile and upload supporting documents if you want reviewer-verified facts recorded for the separate verified eligibility workflow.
6. After applying on the provider's site, return to **My applications** and open the tracker to report that you submitted and optionally add the provider reference. This is the student's report, not provider confirmation.
7. Reopen the tracker to see status history and reviewer messages; view **Notifications** for in-app updates. Provider status is entered manually and is not synchronized.
8. Quick-check answers are temporary and self-reported. A preliminary match is not document verification, an award decision, or a replacement for the provider's final review.

The student dashboard combines current scholarship data with the signed-in student's tracked applications, recent application history, document review states, upcoming deadlines for tracked or matching scholarships, saved reviewer-confirmed eligibility results, and unread notifications. Use **Deadline calendar** in the student navigation to browse active scholarship deadlines and deadlines for the student's own trackers by month; select a date to open its scholarship or application. Dates are based on the catalog and tracker records, and students should confirm the final deadline with the official provider. Recent preliminary checks are retained only in the current browser tab's session storage so they can be summarized after reload; they are not persisted by the backend and are cleared when the student signs out. The dashboard labels these separately from reviewer-confirmed eligibility.

The login flow was implemented with an HTTP session and CSRF protection. JWT, password reset, and email verification are not part of this project. The existing Postman collection predates session security; use the frontend for the submission demo unless the collection is updated with a session and CSRF setup.

### Scholarship catalog and application links

Flyway V7 loads the reviewed 20-entry 2026-27 catalog from `output/pdf/ScholarAssist_Real_Scholarship_Catalog_2026-27.pdf`, including the four programs originally seeded by V6. Flyway V8 normalizes SSP portal links and adds specific SSP prompts. Flyway V9 adds scholarship-specific questions where the catalog supports them. Flyway V11–V14 correct official links, make SDEF checks more specific, distinguish qualifying-school location from residence, and prevent variable award ceilings from being displayed as fixed amounts. Older sample rows are retained for application/history relationships but marked `INACTIVE` and hidden from discovery. The sources and known limits are recorded in [the 2026–27 catalog audit](docs/scholarship-catalog-2026-27.md). Verify each current deadline and criteria with the provider before relying on or publicly deploying this cycle-specific data.

For the current catalog, apply pending Flyway migrations normally when starting the backend. For a development database refresh, `scripts/refresh-scholarships-2026-27.sql` can be run repeatedly in MySQL Workbench after reviewing its exact-name `WHERE` clauses. It does not delete records and avoids duplicate rules. For a new academic year, verify the provider call and add a new Flyway migration plus an updated refresh script; never edit an applied migration or delete rows with application history. Sources include the [National Scholarship Portal](https://scholarships.gov.in/All-Scholarships), [Karnataka SSP](https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login), and provider links in the audit.

Eligibility has two separate checks. The quick check compares self-reported answers to published criteria and provides a preliminary result; it does not save or verify those answers. The verified eligibility workflow uses only reviewer-confirmed facts. SSP schemes are grouped under one portal card with a single SSP application link. Each scheme still has its own details and questions because its category, income, course, and other conditions differ. SSP is a shared portal, so students should select the scheme that matches their situation; the portal determines the authoritative eligibility result.

The API uses `http://localhost:8082` by default. Set `SERVER_PORT` to override this when another port is needed. Generated IDs and timestamps depend on the database. Most application validation errors use `{ "timestamp", "status", "error", "message", "fields" }`.

## Session authentication

- `GET /api/auth/csrf` — retrieve the CSRF token before sending a state-changing request.
- `POST /api/auth/register` — create a student account; the server always assigns `STUDENT`.
- `POST /api/auth/login` — verify email and password and create an HTTP session.
- `GET /api/auth/me` — return safe fields for the signed-in account (200); returns 401 without a valid session.
- `POST /api/auth/logout` — invalidate the session (204).

The frontend uses the session cookie and CSRF token automatically. Directly opening `GET /api/scholarships` in a fresh browser tab without first signing in correctly returns 401; sign in through `pages/login.html`, then use the workspace. Student-specific APIs verify that requested records belong to the signed-in account. Reviewer endpoints and scholarship writes require a reviewer/admin role.

The frontend provides separate entry points at `pages/login.html` for students and `pages/reviewer-login.html` for existing reviewer/admin accounts. Both use the same session login API. The reviewer page checks the returned role, signs out accounts without reviewer/admin privileges, and sends authorized users to the reviewer application queue. Public registration still creates only student accounts; reviewer access requires a separately provisioned account.

## API testing notes

Import [the Postman collection](postman/ScholarAssist_API_Collection.json) and [local environment](postman/ScholarAssist_Local_Environment.json) for endpoint reference. The collection currently predates session authentication, so protected calls require a signed-in session cookie and CSRF token; the frontend handles both automatically. Use the browser flow above for the quickest end-to-end demo. Do not insert a plain-text password into the `users` table.

## Student profiles

`POST /api/student-profile/{userId}` — create (201)

```json
{"fullName":"Asha Rao","phone":"9876543210","dateOfBirth":"2004-03-12","gender":"Female","state":"Karnataka","district":"Mysuru","collegeName":"Mysuru University","course":"BSc","yearOfStudy":2,"cgpa":8.4,"annualFamilyIncome":250000,"category":"OBC"}
```

`GET /api/student-profile/{userId}` — retrieve (200); `PUT /api/student-profile/{userId}` — replace profile fields (200), with the same body shape. Example response:

```json
{"id":12,"userId":3,"fullName":"Asha Rao","phone":"9876543210","dateOfBirth":"2004-03-12","gender":"Female","state":"Karnataka","district":"Mysuru","collegeName":"Mysuru University","course":"BSc","yearOfStudy":2,"cgpa":8.4,"annualFamilyIncome":250000,"category":"OBC","createdAt":"2026-11-05T12:00:00","updatedAt":"2026-11-05T12:00:00"}
```

## Scholarships

- `GET /api/scholarships?state=Karnataka&category=OBC&educationLevel=Undergraduate` — list and optional filters (200).
- `GET /api/scholarships/{id}` — retrieve (200).
- `GET /api/scholarships/{id}/rules` — list the eligibility criteria for a scholarship (200).
- `PUT /api/scholarships/{id}/rules` — replace the full rules list (200); send an empty JSON array to clear it.
- `GET /api/scholarships/search?q=merit&state=Karnataka&category=OBC&educationLevel=Undergraduate` — case-insensitive name/provider search and optional filters (200). All parameters are optional.
- `POST /api/scholarships` — create (201); `PUT /api/scholarships/{id}` — replace (200).
- `DELETE /api/scholarships/{id}` — delete (204).

POST/PUT body (the `rules` field is optional; if supplied on update it replaces existing rules):

```json
{"name":"Merit Award","provider":"Example Foundation","description":"Annual student award","applicationUrl":"https://example.org/apply","amount":50000,"deadline":"2027-03-31","educationLevel":"Undergraduate","state":"Karnataka","category":"OBC","status":"ACTIVE","rules":[{"ruleType":"CGPA","operator":">=","ruleValue":"7.0","description":"Minimum CGPA 7"},{"ruleType":"ANNUAL_FAMILY_INCOME","operator":"<=","ruleValue":"500000"}]}
```

Response includes the scholarship `id`, fields above except `rules`, and `createdAt`/`updatedAt`. `applicationUrl` must be an HTTPS destination; API list/search/detail results include only ACTIVE, non-expired scholarships.

## Eligibility

- `POST /api/eligibility/check/{studentId}` — evaluate all scholarships and persist results (200).
- `GET /api/eligibility/results/{studentId}` — list saved results (200).
- `GET /api/eligibility/{studentId}/{scholarshipId}` — get a saved result (200).
- `POST /api/eligibility/quick-check/{scholarshipId}` — compare temporary self-reported answers to the published scholarship criteria without persisting them (200). Status is `PRELIMINARY_MATCH`, `DOES_NOT_MATCH`, or `MORE_INFORMATION_REQUIRED`; it never changes reviewer-verified eligibility results.

POST has no body. Response example: `[ {"studentId":12,"scholarshipId":5,"status":"ELIGIBLE","reason":"All scholarship criteria are met","evaluatedAt":"2026-11-05T12:00:00"} ]`. Missing rule inputs yield `POSSIBLY_ELIGIBLE`; failed known criteria yield `NOT_ELIGIBLE`. Supported types: `CGPA`, `ANNUAL_FAMILY_INCOME`/`INCOME`, `STATE`, `CATEGORY`, and `EDUCATION_LEVEL`/`COURSE`; operators: `=`, `!=`, `>`, `<`, `>=`, `<=`.

The quick-check endpoint accepts only answers needed by the selected scholarship's rules. Depending on its criteria, fields can include gender, category, disability status/percentage, course, education level, income, marks, year of study, admission seat type, and hostel seat status. Numeric bounds are checked, categorical answers are selected from dropdowns, and `IN` rules represent a published list of accepted values. A preliminary match is based on self-report and is not official eligibility.

## Applications and application documents

- `POST /api/applications/{studentId}` — create a DRAFT application (201).
- `GET /api/applications/{studentId}` — list a student's applications (200).
- `GET /api/applications/details/{applicationId}` — retrieve details (200).
- `PUT /api/applications/{applicationId}` — update draft remarks (200).
- `PATCH /api/applications/{applicationId}/submit` — submit (200); creates a notification.
- `PATCH /api/applications/{applicationId}/status` — set a controlled review status (200).
- `POST /api/applications/{applicationId}/documents` — associate an existing document (201).
- `POST /api/applications/{studentId}/external` — create/reuse a tracker before opening the official provider application (201).
- `PATCH /api/applications/{applicationId}/external-submission` — let the owning student confirm submission on the provider site (200).
- `GET /api/applications/{applicationId}/history` — list the application status timeline for its owner or a reviewer/admin (200).
- `POST /api/applications/{applicationId}/support-requests` — request a correction or cancellation of an externally tracked application before the scholarship deadline (201; owning student only).
- `GET /api/applications/{applicationId}/support-requests` — list requests for the application to its owner or an authorized reviewer/admin (200).
- `GET /api/reviewer/applications` — list submitted applications awaiting reviewer action (200; reviewer/admin only).
- `GET /api/reviewer/applications/{applicationId}` — load one tracked application for the reviewer detail page (200; reviewer/admin only).
- `GET /api/reviewer/applications/support-requests` — list pending correction/cancellation requests (reviewer/admin only).
- `PATCH /api/reviewer/applications/support-requests/{requestId}` — approve or decline a request and notify the student (reviewer/admin only).

Create body:

```json
{"scholarshipId":5,"applicationDate":"2026-11-05","remarks":"Application notes"}
```

Update body: `{"remarks":"Updated notes"}`. Associate-document body: `{"applicationId":20,"documentId":31,"required":true}` (the path application ID is authoritative). Duplicate applications are rejected. Only DRAFT applications can be edited or submitted; submission requires every linked required document to be marked submitted. Responses contain application ID, scholarship ID/name, application date, status, remarks, submittedAt, createdAt, and updatedAt. Timestamps and status are server managed.

Status update body: `{"status":"APPROVED","remarks":"Provider confirmed the student was selected."}`. Reviewer/admin status updates support `UNDER_REVIEW`, `NEEDS_INFORMATION`, `APPROVED`, and `REJECTED` for applications awaiting or in review; every change is stored in the status history and creates an in-app notification for the student. The UI displays `APPROVED` as “Selected” and `REJECTED` as “Not selected”.

For scholarships whose official application is hosted externally, the student-facing **Apply now** action first creates or reuses a local tracker with status `EXTERNAL_STARTED`, then opens the official HTTPS link. After finishing on that provider's website, the student can confirm it in ScholarAssist and optionally enter the provider reference; the record becomes `APPLIED`. A student confirmation is not proof of submission, and ScholarAssist does not synchronize application outcomes with external scholarship providers. Reviewers update the tracked status manually; those updates generate student notifications. Flyway V10 adds the optional provider reference and an auditable application status history table. Flyway V15 adds student correction/cancellation requests. Requests are available for `EXTERNAL_STARTED` or `APPLIED` trackers before the scholarship deadline; correction requests require `APPLIED`. Only an authorized reviewer/admin can approve or decline. An approved cancellation marks the ScholarAssist tracker `CANCELLED` and records history; it does not cancel a provider-side application. An approved correction request asks the student to follow the provider's correction process because ScholarAssist cannot edit the external form.

## Documents

- `POST /api/documents/{studentId}` — multipart/form-data with `documentType` text and `file` (PDF/JPEG/PNG, max 10 MB); response 201.
- `GET /api/documents/{studentId}` — list (200).
- `GET /api/documents/details/{documentId}` — retrieve document metadata (200).
- `DELETE /api/documents/{documentId}` — delete an unlinked document (204).
- `PATCH /api/documents/applications/{applicationId}/{documentId}/submitted?submitted=true` — mark associated document submitted (204).

Upload response example: `{"id":31,"documentType":"INCOME_CERTIFICATE","fileName":"income.pdf","verificationStatus":"PENDING","uploadedAt":"2026-11-05T12:00:00"}`. Files are stored under `scholarassist.upload-dir` (default `uploads`) and the internal path is not returned. Upload validation checks the PDF/JPEG/PNG signature as well as the reported MIME type. Configure `scholarassist.max-upload-bytes` to change the per-file limit.

### Document verification

Flyway V3 adds nullable `reviewed_at`, `review_feedback`, and `reviewer_id` audit columns. Existing V1/V2 migrations are unchanged. Student document routes require a signed-in `STUDENT` account and confirm that the requested profile/document belongs to that account. Students can upload documents, list metadata and rejection feedback, replace pending/rejected unlinked documents, and delete pending/rejected unlinked documents. Verified documents and application-linked documents cannot be changed or deleted.

Reviewer operations require `REVIEWER` or `ADMIN`:

- `GET /api/reviewer/documents` — list documents awaiting review.
- `GET /api/reviewer/documents/students/{studentId}/documents` — list all document statuses for one student's reviewer request view (reviewer/admin only; excludes storage paths).
- `GET /api/reviewer/documents/{documentId}/content` — stream the validated PDF/JPEG/PNG inline for review. The filesystem path is never returned.
- `PATCH /api/reviewer/documents/{documentId}/review` — decide the pending document.

Review body examples:

```json
{"status":"VERIFIED","feedback":"Document is legible and matches the submitted type."}
```

```json
{"status":"REJECTED","feedback":"The uploaded image is not legible. Upload a clearer copy."}
```

Only PENDING documents can be reviewed. Rejection requires feedback. Decisions record the authenticated reviewer and review timestamp; students cannot call reviewer routes.

Each verification or rejection also creates an in-app `DOCUMENT` notification for the owning student. Students can read the decision and any reviewer feedback from the Documents page and their Notifications page. Reviewers use the separate **Application review** and **Document review** navigation; these pages query live database queues. Application updates are manually entered from information received from the scholarship provider, and are recorded in the application status history while notifying the student. No provider inbox or status synchronization is available.

Reviewer/admin accounts cannot be self-registered. To allow specific people to sign in, provision their accounts through the opt-in local bootstrap. It uses the configured Spring `PasswordEncoder` and never stores a plain-text password. It can create multiple accounts from a comma-separated email list. Existing student/reviewer accounts are left unchanged by default; set the explicit promote flag only when you intend to change those listed accounts' role and password. Do not put real or reused passwords in source code or commit them:

```powershell
$env:SCHOLARASSIST_LOCAL_REVIEWER_EMAILS = "reviewer1@example.test,reviewer2@example.test"
$env:SCHOLARASSIST_LOCAL_REVIEWER_PASSWORD = "temporary-local-password"
$env:SCHOLARASSIST_LOCAL_REVIEWER_ROLE = "ADMIN"
$env:SCHOLARASSIST_LOCAL_REVIEWER_PROMOTE_EXISTING = "true"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,local-reviewer-bootstrap"
```

After it finishes successfully, clear the environment variables and run normally with the `dev` profile. Bootstrap requires an eight-character minimum; use a unique password for a deployed environment.

Then open `http://localhost:5500/pages/reviewer-login.html` and sign in with one of the exact email/password combinations used for the bootstrap. `Invalid email or password` means the credentials do not match an account in the current database. The reviewer interface accepts only accounts whose stored role is `REVIEWER` or `ADMIN`.

### Eligibility from verified facts

Flyway V4 creates `student_verified_facts`; V5 retains the full confirmation history while indexing each student's fact types. Each fact record stores its value, source document, confirming reviewer, and confirmation time. Eligibility uses the latest confirmation for each fact type. A reviewer can confirm facts only from a document already marked `VERIFIED`:

- `GET /api/reviewer/documents/verified` — list approved documents available as evidence.
- `PUT /api/reviewer/documents/{documentId}/facts` — save one or more confirmed values, using `{ "facts": [{ "factType": "CGPA", "value": "8.4" }] }`.
- `GET /api/reviewer/documents/students/{studentId}/facts` — inspect the active fact audit records.

Supported reviewer-confirmed fact types include `CGPA`, `ANNUAL_FAMILY_INCOME`, `STATE`, `CATEGORY`, `COURSE`, `EDUCATION_LEVEL`, `GENDER`, `DISABILITY_STATUS`, `DISABILITY_PERCENTAGE`, `SEAT_TYPE`, `PREVIOUS_MARKS_PERCENTAGE`, `PUC_MARKS_PERCENTAGE`, `HOSTEL_STATUS`, and `YEAR_OF_STUDY`. Reviewers must inspect the file and enter the value shown; document verification alone does not populate any fact. Eligibility reads only these confirmed facts, never the editable profile fields. Missing required facts produce `POSSIBLY_ELIGIBLE`; any known failed requirement produces `NOT_ELIGIBLE`; all listed requirements must have verified values and pass for `ELIGIBLE`. Scholarship `state`, `category`, and `educationLevel` targeting columns are also treated as equality requirements when no explicit rule of the same type exists.

`POST /api/eligibility/check/{studentId}` reevaluates and upserts the current result for each active, non-expired scholarship using reviewer-confirmed facts. This verified workflow remains separate from the scholarship detail page's quick self-check. Discovery lists active scholarships. The quick check uses student-selected categorical values and self-reported numeric answers; it only shows **Apply now** after a `PRELIMINARY_MATCH`. The button opens the scholarship record's external application destination in a new tab.

## Notifications

- `GET /api/notifications/{userId}` — list newest first (200).
- `GET /api/notifications/{userId}/unread` — list unread newest first (200).
- `PATCH /api/notifications/{notificationId}/read` — mark read (200).
- `GET /api/notifications/preferences/email` — get the signed-in student's email-alert preference and server delivery availability.
- `PUT /api/notifications/preferences/email` — save `{ "enabled": true }` or `{ "enabled": false }` for the signed-in student.

Example response: `[ {"id":8,"title":"Application submitted","message":"Your application for Merit Award was submitted.","type":"APPLICATION","isRead":false,"createdAt":"2026-11-05T12:00:00"} ]`.

ScholarAssist always keeps application and document updates in the in-app notification center. Email alerts are optional and default off for existing and new accounts. Students can save their opt-in choice from the Notifications page even before SMTP is configured; the page clearly indicates that email delivery begins only after the server has a mail provider. Flyway V16 adds the preference and a durable email outbox; queued alerts are retried up to five times and contain only a generic update prompt and a link to sign in, not application numbers, reviewer remarks, or document details. Turning email alerts off prevents queued messages that have not yet been sent from being delivered.

Email is disabled by default. To enable it locally without changing Windows-wide environment settings or project source files, open IntelliJ **Run → Edit Configurations**, select the Spring Boot configuration for `ScholarAssistApplication`, and add these values in that configuration's **Environment variables** editor: `SCHOLARASSIST_EMAIL_ENABLED=true`, `SPRING_MAIL_HOST=<provider SMTP host>`, `SPRING_MAIL_PORT=<provider SMTP port>`, `SPRING_MAIL_USERNAME=<sender email>`, `SPRING_MAIL_PASSWORD=<provider-issued SMTP/app password>`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`, and `SCHOLARASSIST_EMAIL_FROM=<approved sender email>`. Use the mail provider's SMTP credential, not a ScholarAssist account password; do not paste secrets in chat, source files, or screenshots. `SCHOLARASSIST_FRONTEND_URL` sets the sign-in destination and defaults to `http://localhost:5500`. Save the run configuration, restart only the backend through IntelliJ, then refresh Notifications: the page will show delivery as **Configured** when the backend sees the SMTP host and email setting. If SMTP is not configured, in-app notifications continue to work and the email preference can still be saved, but no email is sent. The project does not send messages during build or tests.

## Development notes

Flyway migrations V1 through V16 define the current schema and catalog state; V6 seeds scholarship data, V10 adds external application tracking metadata and status history, V11–V14 make additive catalog corrections, V15 adds student correction/cancellation requests, and V16 adds opt-in email notification preferences and the delivery outbox. `StudentProfile` has a separate generated profile ID and unique `user_id`. Hibernate schema validation was last confirmed against the configured local MySQL database; rerun the application after applying pending migrations to validate a fresh local setup. The static frontend is in the sibling `../../ScholarAssist` folder and is served separately during development.
# Accessibility and language options

The frontend includes a language selector for English, Hindi, and Kannada. The chosen language is saved in that browser and applies to shared navigation and common interface labels. Scholarship/provider content, backend-provided messages, and some page-specific text may remain in English. Dates and amounts use the selected Indian locale.

Keyboard users can use the “Skip to main content” link, visible focus outlines, and the workspace navigation landmark. The pages also respect the operating system’s reduced-motion preference. These improvements do not change authentication, API contracts, or stored data.
