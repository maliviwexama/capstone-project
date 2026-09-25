# New Feature Pack - Updated

This version keeps the working UFH Residence Maintenance system and adds the requested improvements.

## Added / Updated

### Registration
- Duplicate email registration now returns a clear message instead of a server error.
- The message tells the user to sign in or use another email address.
- Registration still leads to a dedicated **Sign in** link after success.

### Emergency / Immediate Services
- Emergency contacts use real `tel:` call links.
- UFH Protection Services numbers are included from UFH's published emergency information.
- Added a dedicated **GBV / Sexual Violence** section.
- Added UFH GBV support and the national GBV Command Centre.
- Added psychological/mental-health support contacts.
- Added a residence-specific manager/duty phone field.
- Administrators can configure each residence's manager/duty number from **Manage Zones**.
- If no residence manager number is configured, the page falls back to UFH Protection Services.

### Complaint priority
- Every complaint displays a **HIGH / MEDIUM / LOW** priority level.
- The level is derived from the existing category severity score plus waiting time.
- Admin can sort the active queue as **High → Medium → Low**.
- Resolved complaint priority is frozen at the resolution time.

### Complaint archive / retrieval
- Clearing a resolved complaint is now a **soft archive**, not a database delete.
- Archived complaints remain in the database.
- Admin can view, export and restore archived complaints.
- Complaint photos and feedback are not destroyed when a complaint is archived.

### Complaint reporting / exports
- Export active complaints as CSV.
- Export resolved complaints as CSV.
- Export archived complaints as CSV.
- Resolved/archived exports include the technician who attended the complaint.
- Exports also include verification status, priority, elapsed time, resolution time and admin response.

### Complaint timing
- Active complaints show elapsed waiting time.
- Statistics show average resolution time by category.
- Statistics also show current average waiting time by category for unresolved complaints.
- The statistics page explains exactly what the timing values mean.

### Admin visibility
- Admin complaint records now display the student's verification status.
- Complaints remain visible to administrators even when the student has not yet been approved.
- Active, resolved and archived queues are separated.

### Existing features preserved
- Daily bulletin.
- Student updates.
- Admin responses to students.
- Complaint photographs.
- Technician assignment.
- Residence activation zones.
- Student verification workflow.
- Feedback.
- UFH branding and logo.

## Database note
The project uses `spring.jpa.hibernate.ddl-auto=update`, so new fields such as complaint archive information and the residence manager phone can be added to the existing database automatically when the application starts.

## Setup
1. Copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties`.
2. Enter the local MySQL username/password on the database laptop.
3. Do not commit or share `application.properties`.
4. Start MySQL.
5. Run `UfhResidenceMaintenanceApplication`.

## Important
The residence manager number is intentionally configurable rather than invented. An administrator should enter the verified number for each residence under **Manage Zones** before the final demonstration.
