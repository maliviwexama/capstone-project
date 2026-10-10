# Final fixes - 2026-10-10

Implemented before deployment:

1. Proof-of-residence validation is now PDF-only and limited to 5 MB. The backend checks the `.pdf` extension and PDF magic header (`%PDF-`), not only the browser file picker.
2. Rejected student verification now shows an in-system message on the student dashboard and provides a corrected-PDF re-upload workflow. Resubmission returns the verification to PENDING for admin review.
3. Maintenance complaints now support a REJECTED status. Admin can choose `Reject & Archive`; the request is archived immediately and remains in the database/statistics instead of cluttering the active complaints queue.
4. Added simple automated business-rule tests covering valid/invalid PDF uploads, the five-attempt login lock rule, rejected complaint archiving, and complaint severity weights.

No email/SMS notification integration was added; rejection messaging is handled inside the application, as planned.
