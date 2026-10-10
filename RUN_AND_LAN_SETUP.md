# UFH Residence Maintenance — Run & LAN Setup

## 1. Database configuration

Copy:

`src/main/resources/application.properties.example`

to:

`src/main/resources/application.properties`

Then enter the MySQL username and password for the computer running MySQL.

The example configuration already includes:

`server.address=0.0.0.0`

This allows the Spring Boot server to listen for connections from other devices on the local network.

## 2. Run without IntelliJ

From the project root on Windows PowerShell:

`.\mvnw.cmd spring-boot:run`

The project includes the Maven Wrapper, so a separate Maven installation is not required.

## 3. Open on the same computer

Open:

`http://localhost:8080`

The cinematic introduction appears at the application root.

## 4. Open from a phone on the same Wi-Fi

Find the Windows PC's IPv4 address with:

`ipconfig`

Then, after allowing Java/Spring Boot through Windows Firewall if prompted, open:

`http://YOUR-PC-IP:8080`

on the phone while both devices are connected to the same local network.

## 5. Cinematic cover

The `/` page is the cinematic first-session cover. The poetic line types onto the screen and the portal button becomes available when the animation finishes.

The cover is shown once per browser session. Direct access to `/login` remains available.

## Security note

Do not commit a real `application.properties` containing a database password to GitHub. The distributable project intentionally contains the safe `.example` configuration instead.


## 6. Logout / browser Back behavior

The application sends no-cache headers for responses and invalidates the session on logout. After logging out, pressing Back should request the protected page again; because the session is gone, the controller redirects to `/login`.

## 7. Public internet deployment

The LAN address (`http://YOUR-PC-IP:8080`) is only for the same local network. For a public demonstration, deploy the Spring Boot application and a cloud MySQL database to a hosting platform such as Railway. The public deployment should be tested with Wi-Fi disabled and mobile data enabled.
