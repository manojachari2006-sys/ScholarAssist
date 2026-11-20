# ScholarAssist frontend setup

The frontend is a static HTML, CSS, and JavaScript site. It has no package manifest or configured lint/build script.

## Run locally

Serve this folder at `http://localhost:5500` (for example, with VS Code Live Server). The backend defaults to `http://localhost:8082` to avoid conflicting with existing services on 8080 and 8081; `js/config.js` is configured for that port. Edit `apiBaseUrl` there if you intentionally run the backend elsewhere. You can override the Spring Boot port with the `SERVER_PORT` environment variable.

## Authentication

The frontend uses the backend's session-based authentication. Registration and login are available at `pages/register.html` and `pages/login.html`.

The shared API client sends browser session cookies and fetches a CSRF token before every state-changing request. It refreshes the CSRF token after login and logout. The browser manages the HTTP-only session cookie; the frontend does not store credentials or tokens.

On each student page load, the frontend validates the existing session with GET /api/auth/me and loads the profile ID from the authenticated user profile. The frontend does not use temporary development IDs.

## Browser origin and CORS

The backend development CORS configuration allows `http://localhost:5500` with credentials and the methods and headers used by the frontend. Use that exact origin; `http://127.0.0.1:5500` is a different origin and is not included.

## Frontend behavior

- Scholarship search, filters, detail, rules, and eligibility use the existing API routes.
- Scholarship eligibility is evaluated automatically from reviewer-confirmed facts; the browser does not use the profile's editable values as proof. Reviewers confirm those facts in the Document review page after approving a document.
- The main student flow does not require a manual shortlist before applying.
- Profile, application, document, and notification features require a signed-in session and any relevant student profile.
- Authentication uses the backend session cookie and CSRF protection; no JWT or browser-stored credential is used.
- The API exposes no per-scholarship required-document catalogue or document expiry field, so the UI does not invent either.
