# Phase 1 Setup Guide — Install Everything & Run

## Step 1: Install Java 17 (JDK)

1. Go to: https://adoptium.net/temurin/releases/
2. Select: **Version = 17**, **OS = Windows**, **Architecture = x64**, **Package Type = JDK**
3. Download the `.msi` installer and run it
4. During install, CHECK the box **"Add to PATH"** and **"Set JAVA_HOME"**
5. Open a NEW PowerShell window and verify:
   ```
   java -version
   ```
   You should see: `openjdk version "17.x.x"`

---

## Step 2: Install Maven

1. Go to: https://maven.apache.org/download.cgi
2. Download **apache-maven-3.9.x-bin.zip**
3. Extract it to `C:\Program Files\Maven\`
4. Add Maven to PATH:
   - Press Win+S → search "Environment Variables" → "Edit the system environment variables"
   - Click "Environment Variables..."
   - Under "System Variables", find `Path` → Edit → New
   - Add: `C:\Program Files\Maven\apache-maven-3.9.x\bin`
5. Open a NEW PowerShell window and verify:
   ```
   mvn -version
   ```
   You should see: `Apache Maven 3.9.x`

---

## Step 3: Install Docker Desktop

1. Go to: https://www.docker.com/products/docker-desktop/
2. Download **Docker Desktop for Windows**
3. Run the installer (requires WSL2 — it will guide you if not installed)
4. After install, launch Docker Desktop from the Start menu
5. Wait until it says **"Docker Desktop is running"** (green icon in taskbar)
6. Verify in PowerShell:
   ```
   docker --version
   docker-compose --version
   ```

---

## Step 4: Start PostgreSQL with Docker

Navigate to the project directory:
```powershell
cd C:\Users\HP\metavault
```

Start ONLY the PostgreSQL container (not the app yet, since the app JAR doesn't exist yet):
```powershell
docker-compose up -d postgres
```

Verify Postgres is running:
```powershell
docker ps
```
You should see `metavault-postgres` with status `Up`.

Test the connection:
```powershell
docker exec -it metavault-postgres psql -U metavault -d metavault -c "SELECT version();"
```
You should see the PostgreSQL version string.

---

## Step 5: Build & Run the Spring Boot App

From `C:\Users\HP\metavault`, run:

```powershell
mvn clean package -DskipTests
```

This compiles your Java code and produces `target/metavault-1.0.0.jar`.

Then run it:
```powershell
java -jar target/metavault-1.0.0.jar
```

---

## Step 6: Verify Everything Works

Open a browser and go to:
- **Health check**: http://localhost:8080/api/health
  Should return: `{"status":"UP","service":"MetaVault","timestamp":"..."}`

- **Swagger UI**: http://localhost:8080/swagger-ui.html
  Should show the interactive API documentation.

Or use PowerShell:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health"
```

---

## Quick Reference Commands

| Task | Command |
|------|---------|
| Start Postgres only | `docker-compose up -d postgres` |
| Start everything | `docker-compose up -d` |
| Stop everything | `docker-compose down` |
| View logs | `docker-compose logs -f` |
| Build app | `mvn clean package -DskipTests` |
| Run app | `java -jar target/metavault-1.0.0.jar` |
| Run with dev profile | `java -Dspring.profiles.active=dev -jar target/metavault-1.0.0.jar` |
| Run tests | `mvn test` |
