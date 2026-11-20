# ScholarAssist

## Overview

ScholarAssist is a full-stack scholarship management platform designed to help students discover scholarships, check eligibility, manage applications, and track their progress.

The project consists of a static HTML, CSS, and JavaScript frontend and a Spring Boot 4 backend. It uses MySQL for database management, Flyway for database migrations, Spring Data JPA for database operations, and Spring Security for session-based authentication.

## Problem Statement

Many students miss scholarship opportunities because they are unaware of available schemes, eligibility requirements, application deadlines, and required documents.

ScholarAssist aims to simplify this process by providing a centralized platform where students can explore scholarships, check preliminary eligibility, maintain application records, and receive updates from reviewers.

## Features

### 1. Student Authentication

- Student registration and login.
- Session-based authentication using Spring Security.
- CSRF protection for state-changing requests.
- Secure logout functionality.
- Separate login page for reviewers and administrators.

### 2. Scholarship Discovery

- Browse the available scholarship catalog.
- View scholarship details, eligibility criteria, and application deadlines.
- Search scholarships by name or provider.
- Filter scholarships based on state, category, and education level.
- Access official scholarship application websites.

### 3. Eligibility Checking

ScholarAssist provides two separate eligibility-checking workflows.

**Quick Eligibility Check**
- Students answer scholarship-specific questions.
- The system compares their answers with the published eligibility criteria.
- Displays a preliminary eligibility result.
- Answers are self-reported and are not saved as verified information.

**Verified Eligibility Check**
- Uses student information and reviewer-confirmed document facts.
- Stores eligibility evaluation results.
- Keeps verified results separate from preliminary quick-check results.

### 4. Student Profile Management

- Create and update student profiles.
- Store academic and personal information.
- Maintain details such as course, year of study, CGPA, family income, category, and location.

### 5. Application Tracking

- Create and manage scholarship application trackers.
- Track application statuses.
- Record external application submission details.
- Store optional provider reference numbers.
- View application status history.
- Submit correction or cancellation requests for eligible external application trackers.

ScholarAssist tracks applications locally. It does not automatically submit forms or synchronize application statuses with external scholarship providers.

### 6. Document Management

- Upload supporting documents in PDF, JPEG, and PNG formats.
- View uploaded document details.
- Track document verification status.
- Receive reviewer feedback.
- Replace or delete eligible unlinked documents.

### 7. Reviewer Dashboard

- Separate reviewer and administrator login.
- View applications awaiting review.
- Review student-submitted documents.
- Verify or reject documents with feedback.
- Update application statuses.
- Approve or decline eligible correction and cancellation requests.

### 8. Notifications

- Receive in-app notifications about document verification.
- Receive updates when application statuses change.
- View reviewer feedback and application-related updates.

### 9. Student Dashboard and Deadline Calendar

- View scholarship information and tracked applications.
- Monitor recent application activity.
- Check document review status.
- View upcoming scholarship deadlines.
- Browse deadlines using the calendar.
- Access unread notifications.

## Technology Stack

### Frontend
- HTML5
- CSS3
- JavaScript

### Backend
- Java
- Spring Boot 4
- Spring Security
- Spring Data JPA
- Hibernate
- Maven

### Database
- MySQL
- Flyway Database Migration

### Development Tools
- IntelliJ IDEA
- Visual Studio Code
- MySQL Workbench
- Postman
- Git and GitHub

## System Architecture

ScholarAssist follows a frontend-backend architecture.

1. The frontend provides the user interface for students and reviewers.
2. The frontend communicates with the backend through REST APIs.
3. The Spring Boot backend handles authentication, business logic, eligibility evaluation, application tracking, and document management.
4. MySQL stores student profiles, scholarship information, applications, documents, and related records.
5. Flyway manages database schema changes through versioned migrations.

## Project Structure

    ScholarAssist/
    |
    |-- frontend/
    |   |-- pages/
    |   |-- css/
    |   |-- js/
    |   |-- images/
    |
    |-- backend/
    |   |-- src/
    |   |-- pom.xml
    |   |-- mvnw
    |   |-- mvnw.cmd
    |   |-- docs/
    |   |-- scripts/
    |   |-- postman/
    |   |-- README.md
    |
    |-- .gitignore
    |-- README.md

## How to Run the Project Locally

### Prerequisites

Install the following before running the project:

- Java (compatible with the project's Spring Boot version).
- MySQL Server.
- Python or Visual Studio Code Live Server.
- Git.

### Step 1: Clone the Repository

    git clone https://github.com/manojachari2006-sys/ScholarAssist.git

### Step 2: Configure the Database

1. Start the MySQL service.
2. Create a database named `scholarassist`.
3. Configure the database username and password using environment variables.

    DB_USERNAME=root
    DB_PASSWORD=your_password

The backend uses these environment variables to establish the MySQL connection.

### Step 3: Run the Backend

Open a terminal inside the backend folder.

Run:

    .\mvnw.cmd spring-boot:run

The backend runs by default at:

    http://localhost:8082

Flyway automatically applies the pending database migrations when the application starts.

### Step 4: Run the Frontend

Open another terminal inside the frontend folder.

Run:

    python -m http.server 5500

Open the following address in your browser:

    http://localhost:5500/

Use this exact frontend origin because the development CORS configuration allows `http://localhost:5500`.

### Step 5: Access the Application

Student registration:

    http://localhost:5500/pages/register.html

Student login:

    http://localhost:5500/pages/login.html

Reviewer login:

    http://localhost:5500/pages/reviewer-login.html

Reviewer and administrator accounts must be provisioned separately. Public registration creates student accounts only.

## Application Workflow

1. A student registers and logs in.
2. The student creates a profile.
3. The student explores available scholarships.
4. The student checks preliminary eligibility.
5. The student opens the official scholarship application website.
6. The student maintains the application tracker in ScholarAssist.
7. The student uploads supporting documents.
8. A reviewer verifies or rejects submitted documents.
9. The reviewer updates application statuses and provides feedback.
10. The student receives notifications and tracks application progress.

## Important Project Limitations

- Quick eligibility results are preliminary and based on self-reported information.
- Verified eligibility depends on reviewer-confirmed facts.
- Scholarship applications are completed on external provider websites.
- External application submission is confirmed manually by the student.
- Application status updates are entered manually by authorized reviewers.
- ScholarAssist does not synchronize application outcomes with external providers.
- JWT authentication, password reset, and email verification are not implemented.

## Future Scope

The project can be enhanced further through:

- Integration with official scholarship provider APIs.
- Automatic application status synchronization where providers support it.
- Automated deadline reminders.
- Advanced scholarship recommendation algorithms.
- Improved analytics and reporting.
- Additional document verification capabilities.
- Mobile application development.

## Documentation

For detailed backend setup instructions, API endpoints, authentication details, database migrations, and testing information, refer to:

[Backend Documentation](backend/README.md)

For scholarship catalog sources and verification details, refer to:

[Scholarship Catalog Audit](backend/docs/scholarship-catalog-2026-27.md)

## Repository

GitHub Repository:

https://github.com/manojachari2006-sys/ScholarAssist

## Conclusion

ScholarAssist provides a centralized platform for scholarship discovery, preliminary eligibility checking, document management, and application tracking. By bringing these features together, the project aims to make scholarship information and application management easier for students while providing reviewers with tools to verify documents and monitor applications.
