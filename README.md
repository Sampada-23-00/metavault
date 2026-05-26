# MetaVault

Metadata Backup & Version Control Platform built using Java, Spring Boot, PostgreSQL, JWT Authentication, and Docker.

## Features
- Immutable metadata versioning
- Rollback recovery
- JSON diff tracking
- JWT authentication
- Audit logging
- Soft delete support
- Ownership enforcement

## Tech Stack
- Java 17
- Spring Boot
- PostgreSQL
- Spring Security
- JWT
- Docker
- Hibernate/JPA

## Architecture
Client
 ↓
REST Controllers
 ↓
Service Layer
 ↓
Repository Layer
 ↓
PostgreSQL

## API Features
- Register/Login
- Create metadata snapshots
- View version history
- Restore previous versions
- Compare JSON diffs
- Audit activity tracking

Swagger UI

http://localhost:8080/swagger-ui/index.html

<img width="917" height="868" alt="swagger" src="https://github.com/user-attachments/assets/f33d75fa-17b6-4e3a-af79-1493d69b5ec3" />

<img width="888" height="851" alt="swagger1" src="https://github.com/user-attachments/assets/c8cdd230-3527-44c3-9e5a-3d5370181c44" />

<img width="1167" height="838" alt="JWT auth2" src="https://github.com/user-attachments/assets/392b52db-7a90-414b-a739-1ba047acbbeb" />

<img width="901" height="867" alt="111" src="https://github.com/user-attachments/assets/3e329b9c-409a-4517-abfa-8b147bdd5ee6" />

<img width="898" height="814" alt="222" src="https://github.com/user-attachments/assets/610bbda6-0e35-407c-a635-fb573b30f035" />

