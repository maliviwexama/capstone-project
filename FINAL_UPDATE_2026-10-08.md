# Final Update — 8 October 2026

This version is based on the **CINEMATIC-MOBILE-AUTH-FORM-FIX-2026-10-08** project ZIP and adds the final group-requested corrections.

## Final corrections

### 1. Student proof of residence is mandatory
- A student cannot create an account unless a proof-of-residence registration document is uploaded.
- The server checks this requirement before saving the account.
- The existing administrator verification workflow remains in place after registration.

### 2. No duplicate maintenance requests
- The maintenance-report form now receives a one-time submission token.
- The token is consumed only after the request passes validation.
- Double-clicking the submit button is also prevented in the browser.
- Repeated POST/browser resubmission is redirected to **My Requests** instead of creating another complaint.
- The existing complaint workflow is otherwise unchanged.

### 3. Logout + browser Back protection
- Added a no-cache filter that sends `no-store`, `no-cache`, `Pragma: no-cache` and expired-cache headers.
- After logout, authenticated pages should not remain available from the browser cache when Back is pressed.
- The protected controllers already check the session and redirect unauthenticated users to `/login`.

### 4. Cleaner maintenance report page
- Removed the client-side safety-warning pop-up/banner.
- Safety detection still happens on the server and still affects priority.
- Fixed the duplicated/malformed photo label in the report form.
- Submit button changes to `Submitting...` after the first valid submit click.

### 5. Internet accessibility
- The existing LAN configuration keeps `server.address=0.0.0.0`.
- The final public deployment will use Railway with HTTPS, so the deployed application will not depend on the university Wi-Fi/LAN.
- Railway deployment/database configuration is intentionally a separate final step so local MySQL credentials are not committed to GitHub.

## Verification checklist before presentation

1. Student registration without proof → account must NOT be created.
2. Student registration with proof → account is created as PENDING.
3. Submit one maintenance request → exactly one complaint appears.
4. Double-click/refresh submission → no duplicate complaint.
5. Logout → press browser Back → protected page must not be usable.
6. Open the deployed URL on mobile data with Wi-Fi OFF → application must load over HTTPS.
