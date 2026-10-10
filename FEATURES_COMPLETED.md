# Features Completed / Current

The current working project includes:

- UFH-branded student and admin interfaces with logo.
- Student/staff registration and login.
- Duplicate-email registration handling.
- Student residence and verification workflow.
- Maintenance complaint submission.
- Complaint photographs with validation and preview.
- Admin complaint responses.
- Technician assignment and complaint status updates.
- High / Medium / Low priority levels.
- Priority sorting and overdue sorting.
- Priority frozen at resolution.
- Active, resolved and archived complaint queues.
- Soft archive instead of permanent database deletion.
- Restore archived complaints.
- CSV exports for active, resolved and archived complaints.
- Resolved exports include the technician/attended-by field.
- Complaint timing and category statistics.
- Daily bulletin and student updates.
- Admin member creation.
- Residence-zone activation.
- Residence-specific manager/duty phone configuration.
- Emergency / Immediate Services page.
- Real click-to-call emergency links.
- GBV / sexual-violence support information.
- Psychological and medical emergency contacts.
- Student feedback.

## Database

`spring.jpa.hibernate.ddl-auto=update` is enabled. When the project is started against the existing database, Hibernate can add the new complaint archive columns and residence manager phone column.

## Important demonstration setup

Before demonstrating:
1. Ensure MySQL is running.
2. Ensure `application.properties` exists locally with the correct database credentials.
3. Open Admin → Manage Zones and enter the verified residence manager/duty number.
4. Test duplicate registration.
5. Test a complaint with High priority.
6. Test assigning/resolving a complaint.
7. Test exporting resolved complaints.
8. Test archiving and restoring a resolved complaint.
9. Test Emergency → GBV and the call buttons.


## Additional integrated features

- Cinematic first-session cover page with typed poetic line and UFH-themed residence illustration.
- Full-name validation using letters, spaces and common name punctuation only.
- Student-facing explanation of HIGH / MEDIUM / LOW maintenance priority.
- Safety Concern reporting flag for potentially dangerous faults.
- Safety concerns contribute to priority and are treated as urgent for inactive-zone rules.
- Administrator-friendly elapsed-time display (days/hours/minutes).
- LAN-ready server binding configuration (`server.address=0.0.0.0`).


## Final group-requested corrections — 8 October 2026

- Student proof of residence registration is mandatory before a student account can be created.
- Maintenance report submissions use a one-time server-side form token plus client-side submit protection to prevent duplicate requests.
- Added no-cache response headers so logged-out authenticated pages are not retained for browser Back/Forward navigation.
- Removed the client-side safety warning banner from the report form while keeping server-side safety detection.
- Fixed the duplicated photo label in the report form.
