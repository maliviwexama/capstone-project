# Features Added — 8 October 2026

- Added safe first-run residence data for a fresh database. Existing residence records are never overwritten. The seed uses residence names published on UFH's Alice Campus facilities page.
- Added server-side and browser-side full-name validation for both normal registration and administrator-created members.
- Added automatic safety-language detection for reports, including combined descriptions such as a broken door and broken bulb. The existing safety checkbox remains available.
- Added a clearer priority explanation and shows the initial priority after a report is submitted.
- Reworked administrator open-time display into plain language such as “2 days, 4 hours” or “4 hours, 12 minutes”, while retaining the logged date.
- Improved the report-success page and responsive dashboard illustration sizing.
- Cinematic cover now remembers that it has been shown using local browser storage, so it behaves as a first-time introduction rather than replaying on every new tab.
- Existing login lockout and forgot-password reset-attempt protection remain intact.


## Cinematic UFH Landing Page Update
- Replaced the abstract SVG cover scene with a real UFH campus hero photograph supplied for the project.
- Removed the previous slogan from the cinematic cover and made the page focus on the UFH Residence Maintenance Portal identity.
- Kept the poem `Every home has a story — every repair keeps it going.` and upgraded it to a typewriter effect with a blinking cursor.
- The Enter Portal button becomes active after the poem finishes typing and then opens the login page.
- The cinematic cover remains a first-visit experience using browser localStorage; after the user enters the portal once, future visits go directly to login.
- Added responsive mobile styling and a reduced-motion fallback.
