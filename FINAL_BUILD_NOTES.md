# UFH Residence Maintenance - Complete Integrated Build

This package is based on the complete final project contained in the team's FINAL MASTER SUBMISSION, not the smaller feature-only package.

## Preserved complete project areas
- Student portal
- Residence Updates / Daily Bulletin
- Admin dashboard
- Active, resolved and archived complaint queues
- Zones and residence management
- Student verification workflow and audit history
- Feedback
- Admin member creation
- Complaint photos and CSV exports
- Residence manager emergency contact
- Emergency / Immediate Services

## UI improvements
- Replaced decorative emoji-based navigation/dashboard artwork with consistent navy/gold illustrations.
- Added strict illustration sizing so artwork cannot expand to full-screen height.
- Dashboard cards use a responsive 4/2/1 column layout.
- Emergency controls and call buttons are phone-friendly.
- Student Updates remains available from the portal and dashboard.
- Admin navigation retains all complete admin modules.
- Feedback rating uses accessible numbered 1-5 controls instead of decorative star characters.

## Account protection
- Five incorrect login attempts temporarily lock an account for 15 minutes.
- Forgot Password permits at most three failed identity checks in a 30-minute window.
- Passwords must contain at least 8 characters.
- Successful password recovery clears the login lock.
- Verification audit fields from the complete project were preserved.

## Important local setup
The real `application.properties` is intentionally not included. Create it locally using `application.properties.example` and your own MySQL password.

Do not commit real database passwords to GitHub.

## Maven wrapper
The Maven wrapper properties are included so the project can be launched with:

`mvnw.cmd spring-boot:run`

or from IntelliJ using the normal Spring Boot run configuration.


## Latest integrated improvements

- Added a cinematic first-session landing screen at `/`.
- The opening scene uses the UFH navy/gold visual language and a residence maintenance illustration.
- The closing poetic line is typed on screen before the portal button becomes available.
- The cover is shown once per browser session; direct `/login` access remains available.
- Added server-side and browser-side full-name validation.
- Added a student-facing explanation of maintenance priority.
- Added an explicit Safety Concern flag for issues such as broken doors or dangerous electrical faults. Safety concerns receive higher priority and can be processed even when a normal request zone is inactive.
- Admin active complaints now display elapsed time in a compact day/hour/minute format instead of decimal hours.
- Added `server.address=0.0.0.0` to the application configuration example to support later LAN/phone testing.
