# PlaceTrack

A campus placement drive tracker built with **Spring Boot**, **Spring MVC**, **Thymeleaf**, **Spring Data JPA /
Hibernate** and a second **eligibility microservice**. Mini project for IT23502 Web Programming (Units IV and V).

Coordinators manage companies and placement drives (minimum CGPA, maximum backlogs, allowed departments, deadline).
Students register, browse open drives, check their eligibility and apply. Eligibility is decided by a separate
Spring Boot service on its own port. If that service is down, the student is told to try later and nothing is saved.

---

## Contents

1. [Project status](#1-project-status)
2. [How it works (architecture)](#2-how-it-works-architecture)
3. [Prerequisites](#3-prerequisites)
4. [Get the code](#4-get-the-code)
5. [Run the project](#5-run-the-project)
6. [Use the application](#6-use-the-application)
7. [The database (H2)](#7-the-database-h2)
8. [Demos for the viva](#8-demos-for-the-viva)
9. [Run the tests](#9-run-the-tests)
10. [Test the APIs with Postman](#10-test-the-apis-with-postman)
11. [Project structure](#11-project-structure)
12. [Developing on this project](#12-developing-on-this-project)
13. [Configuration](#13-configuration)
14. [Troubleshooting](#14-troubleshooting)
15. [Further documentation](#15-further-documentation)

---

## 1. Project status

| Part | Owner | Status |
| --- | --- | --- |
| Data layer, inheritance, HQL, company and drive pages, Session demo, eligibility microservice | M1 | ✅ Done |
| Login and roles, check eligibility, apply, withdraw, AOP audit log, BeanFactory demo | M2 | ✅ Done |
| Applicants and status changes, student profile, dashboard, REST API, Postman, report | M3 | ⏳ In progress |

The detailed plan for every member is in [`docs/PLAN.md`](docs/PLAN.md).

---

## 2. How it works (architecture)

```text
 Browser / Postman
        │  HTTP
        ▼
┌──────────────────────────── placetrack-app  (port 8080) ────────────────────────────┐
│  LoginInterceptor ─► Controllers ─► Services ─► Repositories ─► H2 database (file)  │
│  (role check)        (pages, MVC)   (rules,      (Spring Data    data/placetrack    │
│                                      @Transactional)  JPA/HQL)                      │
│                                        │                                            │
│                       ApplyAuditAspect │ (AOP: logs every apply attempt)            │
│                                        │                                            │
│                              EligibilityClient (RestTemplate, 2 s / 3 s timeouts)   │
└────────────────────────────────────────┼────────────────────────────────────────────┘
                                         │  POST /eligibility/check   (JSON)
                                         ▼
                    ┌──────── eligibility-service  (port 8081) ────────┐
                    │  EligibilityController ─► EligibilityService      │
                    │  3 rules: min CGPA, max backlogs, department      │
                    │  No database. Stateless.                          │
                    └───────────────────────────────────────────────────┘
```

- **placetrack-app** is the main website. It owns all the data and pages.
- **eligibility-service** is a small independent microservice. The two share no Java code, only a JSON contract.

**Tech stack:** Java 17, Spring Boot 3.5.16, Spring MVC, Thymeleaf, Bootstrap 5 (CDN), Spring Data JPA, Hibernate,
H2 database, Bean Validation, Spring AOP, BCrypt (`spring-security-crypto`), JUnit 5, Mockito, Maven (wrapper included).

---

## 3. Prerequisites

You only need to install **a JDK** and **Git**. Maven and the database are **not** needed: each project contains a
Maven wrapper (`mvnw`) that downloads Maven by itself, and the H2 database comes in as a library.

| Tool | Needed? | Version | Check it |
| --- | --- | --- | --- |
| **JDK** (Java Development Kit) | ✅ Required | 17 or newer (17, 21 …) | `java -version` |
| **Git** | ✅ Required | any | `git --version` |
| Internet (first build only) | ✅ Required once | – | Downloads Maven and libraries (~150 MB) into `~/.m2` |
| Spring Tool Suite 4 **or** IntelliJ IDEA | Recommended | latest | The spec asks for the demo to run in **STS** |
| Postman | Recommended | latest | For API testing and report screenshots |
| Free ports **8080** and **8081** | ✅ Required | – | See [Troubleshooting](#14-troubleshooting) |

### Install a JDK

- **Windows:** download **Eclipse Temurin 17 or 21** (`.msi`) from https://adoptium.net and, in the installer, enable
  **"Set JAVA_HOME variable"** and **"Add to PATH"**.
- **macOS:** `brew install --cask temurin@21` (or the `.pkg` from https://adoptium.net).
- **Ubuntu / Debian:** `sudo apt install openjdk-21-jdk`
- **Arch / CachyOS / Manjaro:** `sudo pacman -S jdk21-openjdk`

Open a **new** terminal afterwards and check:

```bash
java -version
```

It must say version 17 or higher. A JRE alone is not enough; it must be a JDK.

### Install an IDE (optional but recommended)

- **Spring Tool Suite 4:** https://spring.io/tools (unzip and run).
- **IntelliJ IDEA Community:** https://www.jetbrains.com/idea/download (free edition is enough).

---

## 4. Get the code

```bash
git clone https://github.com/Selvavignesh-S/placetrack2.git
```
```bash
cd placetrack2
```

The folder contains **two separate Maven projects**: `placetrack-app` and `eligibility-service`.

---

## 5. Run the project

Always start the **eligibility service first**, then the **main app**. Each one needs its own terminal (or its own
Run button in the IDE), and both keep running until you stop them with `Ctrl + C`.

The very first start takes a few minutes while Maven downloads everything. Later starts take about 10 seconds.

### Option A: from a terminal

**Linux / macOS**

Terminal 1:
```bash
cd eligibility-service
```
```bash
./mvnw spring-boot:run
```

Terminal 2:
```bash
cd placetrack-app
```
```bash
./mvnw spring-boot:run
```

**Windows** (Command Prompt; in PowerShell write `.\mvnw.cmd`)

Terminal 1:
```bash
cd eligibility-service
```
```bash
mvnw.cmd spring-boot:run
```

Terminal 2:
```bash
cd placetrack-app
```
```bash
mvnw.cmd spring-boot:run
```

Each app is ready when its log shows `Started ...Application in X seconds`.

### Option B: from Spring Tool Suite (STS)

1. **File → Import… → Maven → Existing Maven Projects → Next.**
2. **Root Directory:** browse to the cloned `placetrack2` folder. STS finds both `pom.xml` files. Tick both,
   click **Finish**, and wait for the build (progress bar at the bottom right).
3. Right-click **eligibility-service → Run As → Spring Boot App**.
4. Right-click **placetrack-app → Run As → Spring Boot App**.
5. Both now appear in the **Boot Dashboard** view, where you can start, stop and restart them.

### Option C: from IntelliJ IDEA

1. **File → Open** and choose the cloned `placetrack2` folder. Click **Trust Project** if asked.
2. In the Project panel, right-click `eligibility-service/pom.xml` → **Add as Maven Project**. Do the same for
   `placetrack-app/pom.xml`. Wait for the indexing and download to finish.
3. **File → Project Structure → Project → SDK:** choose your JDK (17 or newer).
4. Open `eligibility-service/src/main/java/com/placetrack/eligibility/EligibilityServiceApplication.java` and click
   the green ▶ next to `main`.
5. Open `placetrack-app/src/main/java/com/placetrack/PlacetrackApplication.java` and click ▶.

### Check that both are running

| URL | Expected |
| --- | --- |
| http://localhost:8081/eligibility/ping | `{"status":"UP"}` |
| http://localhost:8080 | The PlaceTrack login page |

### Build a runnable jar (optional)

```bash
./mvnw package
```
This also runs the tests. Then start it with `java -jar target/placetrack-app-0.0.1-SNAPSHOT.jar` (inside
`placetrack-app`) or `java -jar target/eligibility-service-0.0.1-SNAPSHOT.jar` (inside `eligibility-service`).

---

## 6. Use the application

On the first start the app fills an empty database with demo data. **Every demo password is `Pass@123`.**

| Email | Role | Department | CGPA | Backlogs | Note |
| --- | --- | --- | --- | --- | --- |
| `coord1@placetrack.edu` | Coordinator | – | – | – | Dr. Meera Nair |
| `coord2@placetrack.edu` | Coordinator | – | – | – | Prof. Arjun Rao |
| `student1@placetrack.edu` | Student | IT | 8.6 | 0 | Eligible for most drives |
| `student2@placetrack.edu` | Student | IT | 7.2 | 1 | Not eligible for Nimbus Cloud (2 reasons) |
| `student3@placetrack.edu` | Student | CSE | 8.0 | 0 | Already placed, cannot apply |
| `student4@placetrack.edu` | Student | MECH | 7.9 | 0 | Wrong department for Nimbus Cloud |
| `student5@placetrack.edu` | Student | ECE | 9.1 | 0 | Shortlisted at Zephyr |
| `student6@placetrack.edu` | Student | CSE | 6.4 | 2 | Low CGPA, 2 backlogs |

Seeded drives: Zephyr Software Engineer Trainee, Nimbus Cloud Engineer, Quanta Data Scientist, Orion Graduate
Engineer Trainee, and Zephyr Support Engineer (deadline already passed, so it is **Closed**).

### As a coordinator (`coord1@placetrack.edu`)

1. Open http://localhost:8080 and log in.
2. **Companies:** add, edit or delete companies. A company that has drives cannot be deleted; names must be unique.
3. **Drives → Create drive:** pick a company, enter the role, package (LPA), minimum CGPA, maximum backlogs,
   tick the allowed departments and choose a deadline.
   Company missing from the list? Click **Add a new company**: after saving you come back with it selected.
4. A drive that already has applications cannot be deleted.

### As a student (`student1@placetrack.edu`, or register a new account)

1. **Register here** on the login page creates a new student account.
2. **Drives** lists every drive whose deadline is today or later. Click **View**.
3. **Check eligibility** shows "You are eligible" or the exact reasons, for example
   "CGPA 7.2 is below the required 7.5". It saves nothing.
4. **Apply** saves the application if every rule passes; otherwise the reasons are shown and nothing is saved.
5. **My applications** shows each application's status. **Withdraw** is possible while it is APPLIED or SHORTLISTED.

Try `student2@placetrack.edu` on the Nimbus Cloud Engineer drive to see a refusal with two reasons.

### What happens when the eligibility service is down

Stop the eligibility service and click **Apply** or **Check eligibility**: the page shows
**"Eligibility check unavailable, try later"** and nothing is saved. Start the service again and it works.

### Audit log (AOP)

Every Apply attempt prints a line in the **main app's console**, for example:
```text
AUDIT apply student=3 drive=2 outcome=SUCCESS time=292ms
AUDIT apply student=4 drive=2 outcome=BLOCKED reason="You are not eligible for this drive" time=19ms
```

---

## 7. The database (H2)

H2 is a real SQL database written in Java. It runs **inside** the main app, so there is nothing to install, and
stores its data in a file in the `data/` folder.

### Open the H2 console

1. With the main app running, open http://localhost:8080/h2-console
2. **JDBC URL:** `jdbc:h2:file:./data/placetrack`  **User Name:** `sa`  **Password:** (leave empty)
3. Click **Connect**. Example query (shows the inheritance join):
   ```sql
   SELECT U.ID, U.FULL_NAME, U.EMAIL, S.REG_NO, S.DEPARTMENT, S.CGPA
   FROM USERS U JOIN STUDENTS S ON U.ID = S.ID;
   ```

**Where is `data/`?** It is created in the folder the app was **started from**:

| Started from | Database folder |
| --- | --- |
| Terminal inside `placetrack-app` | `placetrack-app/data/` |
| STS | `placetrack-app/data/` |
| IntelliJ (usually) | `placetrack2/data/` (the repository root) |

If the console says *"Database … not found"*, use the full path instead, for example
`jdbc:h2:file:/home/you/placetrack2/placetrack-app/data/placetrack`.

### Reset the data

1. Stop the main app.
2. Delete the `data/` folder (see the table above for where it is).
3. Start the app again. The tables are recreated from the entities and the demo data is inserted again.

Do this before every demo or rehearsal so everyone sees the same starting data.

### Tables

`users`, `students`, `coordinators` (JOINED inheritance: `students.id` and `coordinators.id` point to `users.id`),
`companies`, `drives`, `applications`. The full schema and sample rows are in
[`docs/report/schema.sql`](docs/report/schema.sql) and
[`docs/report/schema-and-sample-data.sql`](docs/report/schema-and-sample-data.sql).

---

## 8. Demos for the viva

### Hibernate SessionFactory / Session / Transaction (rollback proof)

Stop the normal main app first (it uses port 8080), then inside `placetrack-app`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Look for the block starting with `===== Hibernate demo` in the console. It inserts a company, rolls back, and shows
the count going 5 → 6 → 5.

In an IDE: edit the run configuration of `PlacetrackApplication` and add the program argument
`--spring.profiles.active=demo` (IntelliJ: **Run → Edit Configurations… → Program arguments**; STS: **Run As → Run
Configurations… → Spring Boot App → Profile**: `demo`). Remove it again afterwards.

### BeanFactory vs ApplicationContext

Open `placetrack-app/src/main/java/com/placetrack/demo/BeanFactoryDemo.java` and run its `main` method
(IDE: ▶ next to `main`, or **Run As → Java Application** in STS). It does not start the web app. The output shows the
BeanFactory creating a bean only when asked, and the ApplicationContext creating it at startup.

---

## 9. Run the tests

Inside each project folder:

```bash
./mvnw test
```
(Windows: `mvnw.cmd test`)

In the IDE: right-click `src/test/java` → **Run 'All Tests'** (IntelliJ) or **Run As → JUnit Test** (STS).

| Project | Tests | What they check |
| --- | --- | --- |
| `eligibility-service` | 7 | The 3 rules; the REST endpoint returns 200 for valid JSON and 400 for a missing field |
| `placetrack-app` | 14 | HQL queries, seed data and inheritance, and the apply / withdraw rules (Mockito) |

The tests use their own temporary in-memory database, so they never touch your `data/` folder.

To build and test everything the way a CI server would:
```bash
./mvnw clean verify
```

---

## 10. Test the APIs with Postman

1. Start both apps.
2. In Postman: **Import** → choose both files in the [`postman/`](postman/) folder:
   `PlaceTrack.postman_collection.json` and `PlaceTrack.local.postman_environment.json`.
3. Top right, select the environment **"PlaceTrack local"** (sets `{{baseUrl}}` = `http://localhost:8080` and
   `{{eligUrl}}` = `http://localhost:8081`).
4. Open folder **00 Eligibility service** and **Send** each request, or right-click the collection → **Run** to run
   them all and see the green test results.

### Eligibility service API (port 8081)

| Method + path | Body | Response |
| --- | --- | --- |
| `GET /eligibility/ping` | – | `{"status":"UP"}` |
| `POST /eligibility/check` | see below | `200 {"eligible": …, "reasons": [...]}`, or `400` if a field is missing or invalid |

```json
{ "cgpa": 7.2, "backlogs": 1, "department": "IT",
  "minCgpa": 7.5, "maxBacklogs": 0, "allowedDepartments": ["IT", "CSE"] }
```
gives
```json
{ "eligible": false,
  "reasons": ["CGPA 7.2 is below the required 7.5", "Backlogs 1 exceed the limit of 0"] }
```

The main app's REST API (`/api/drives`, `/api/applications`, …) is being built by M3; see `docs/PLAN.md` section 8.4.

---

## 11. Project structure

```text
placetrack2/
├── README.md                     this file
├── docs/
│   ├── PLAN.md                   scope, contract, every member's step-by-step plan, demo scenarios
│   ├── m1.md, m2.md              what each member built + concepts + viva answers
│   ├── team.md                   team number, names, register numbers (fill in)
│   ├── report/                   schema.sql, schema-and-sample-data.sql, later the report
│   └── screenshots/              screenshots for the report
├── postman/                      Postman collection and environment
├── eligibility-service/          microservice, port 8081
│   ├── pom.xml, mvnw, mvnw.cmd
│   └── src/main/java/com/placetrack/eligibility/
│       ├── EligibilityServiceApplication.java   main
│       ├── EligibilityController.java           POST /eligibility/check, GET /eligibility/ping
│       ├── EligibilityService.java              the 3 rules
│       └── EligibilityRequest.java, EligibilityResponse.java   JSON records
└── placetrack-app/               main web app, port 8080
    ├── pom.xml, mvnw, mvnw.cmd
    └── src/
        ├── main/java/com/placetrack/
        │   ├── PlacetrackApplication.java      main
        │   ├── model/        entities: User → Student, Coordinator; Company, Drive, Application
        │   ├── repository/   Spring Data repositories + HQL queries
        │   ├── service/      business rules (CompanyService, DriveService, AuthService, ApplicationService)
        │   ├── controller/   MVC controllers (pages)
        │   ├── client/       EligibilityClient: calls the microservice
        │   ├── aspect/       ApplyAuditAspect (AOP)
        │   ├── exception/    ApplyException, EligibilityUnavailableException
        │   ├── config/       AppConfig, LoginInterceptor, SessionUser, DataSeeder
        │   └── demo/         HibernateSessionDemo, BeanFactoryDemo
        ├── main/resources/
        │   ├── application.properties
        │   └── templates/    Thymeleaf pages: fragments/, coordinator/, student/, login, register, error
        └── test/java/com/placetrack/   JUnit + Mockito tests
```

---

## 12. Developing on this project

### 12.1 Team Git workflow

Each member works on **their own branch** and merges into `main` through a **pull request**.

| Member | Branch |
| --- | --- |
| M1 | `m1-foundation` |
| M2 | `m2-apply-flow` |
| M3 | `m3-api-delivery` |

**Before you start working (every day):**
```bash
git switch main
```
```bash
git pull
```
```bash
git switch -c m3-api-delivery
```
(Use `git switch m3-api-delivery` instead of the last line if the branch already exists, then
`git merge main` to bring in the latest work.)

**While working, commit small steps:**
```bash
git add .
```
```bash
git commit -m "feat(m3): add applicants page"
```
```bash
git push -u origin m3-api-delivery
```

Commit message format: `type(member): what changed`, with types `feat` (new feature), `fix` (bug fix),
`test`, `docs`, `chore` (setup). Small commits by each member are evidence for the DevOps part of the marks.

**When a part is finished:**
1. Run `./mvnw test` in both projects. Everything must pass.
2. Push your branch.
3. On GitHub: **Pull requests → New pull request**, base `main` ← compare your branch → **Create pull request**.
4. Another member reads it, runs it, and clicks **Merge pull request**.
5. Everyone runs `git switch main` and `git pull`.

### 12.2 Shared data rule

Each laptop has its **own** H2 database; data is **not** shared through Git (the `data/` folder is in `.gitignore`).
What is shared is the **code that creates the data**:

- Need demo data everyone should have? Add it to
  [`config/DataSeeder.java`](placetrack-app/src/main/java/com/placetrack/config/DataSeeder.java) and commit.
- After pulling someone's seeder change: stop the app, delete `data/`, start again.
- New entity fields create new columns automatically (`ddl-auto=update`). If you change a column's type or add a value
  to an enum, delete `data/` and restart.

### 12.3 How to add a feature (the usual path)

Follow the layers from the database up. Example: "show a coordinator the applicants of a drive".

1. **Entity** (`model/`): add fields or a new `@Entity` class if new data is needed.
2. **Repository** (`repository/`): add a derived method (`countByDriveId`) or an HQL `@Query`.
3. **Service** (`service/`): put the business rules here, `@Transactional` if it changes data.
   Throw `ApplyException` (or `IllegalArgumentException`) with a clear message when a rule fails.
4. **Controller** (`controller/`): a `@GetMapping` that puts data into the `Model` and returns a template name;
   a `@PostMapping` that calls the service and ends with `return "redirect:/..."` (Post-Redirect-Get).
   Get the logged-in user with `@SessionAttribute(SessionUser.KEY) SessionUser user`.
5. **Template** (`templates/`): copy an existing page (for example `coordinator/companies.html`) so you get the
   shared head, navbar and message box.
6. **Navbar link**: add it inside the right role block in `templates/fragments/layout.html`.
7. **Test**: add a JUnit/Mockito test for the new rule, run `./mvnw test`.
8. **Restart** the app to see Java changes. (Template changes also need a restart unless you use an IDE hot-reload.)

### 12.4 Coding rules

- **Constructor injection only** (no `@Autowired` on fields).
- **Controllers call services, services call repositories.** Controllers never use repositories directly.
- **No Lombok.** Plain getters and setters, so every line can be explained in the viva.
- **Every class starts with a one-line `/** … */` comment** saying what it is for.
- **Pages under `/student/**` and `/coordinator/**`** are automatically protected by `LoginInterceptor`.
- **Forms that bind to an entity** must limit the bound fields with `@InitBinder` + `setAllowedFields(...)`
  (see `AuthController`), so nobody can post fields like `placed` or `id`.
- **Explain-it-back rule:** before committing, make sure you can explain every annotation you wrote.

---

## 13. Configuration

Main app: `placetrack-app/src/main/resources/application.properties`

| Property | Value | Meaning |
| --- | --- | --- |
| `server.port` | `8080` | Port of the website |
| `spring.datasource.url` | `jdbc:h2:file:./data/placetrack` | H2 database file location |
| `spring.datasource.username` / `password` | `sa` / empty | H2 login |
| `spring.h2.console.enabled` | `true` | Enables `/h2-console` |
| `spring.jpa.hibernate.ddl-auto` | `update` | Hibernate creates or updates tables from the entities |
| `spring.jpa.show-sql` | `true` | Prints every SQL statement (useful for learning) |
| `eligibility.url` | `http://localhost:8081` | Where the eligibility service runs |
| `server.servlet.session.tracking-modes` | `cookie` | Session id only in a cookie, never in URLs |
| `server.error.include-message` | `always` | Lets the error page show messages like "Drive 99 not found" |

Eligibility service: `eligibility-service/src/main/resources/application.properties` sets `server.port=8081`.

Override any property when starting, without editing files:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
```

---

## 14. Troubleshooting

| Problem | Fix |
| --- | --- |
| `Port 8080 was already in use` (or 8081) | Another copy is still running. Stop it in the IDE, or find it: Linux/macOS `ss -ltnp \| grep 8080` (or `lsof -i :8080`) then `kill <pid>`; Windows `netstat -ano \| findstr :8080` then `taskkill /PID <pid> /F` |
| `./mvnw: Permission denied` | `chmod +x mvnw` |
| `JAVA_HOME is not set` / `java: command not found` | Install a JDK (section 3) and open a new terminal. Windows: set `JAVA_HOME` to the JDK folder in *Environment Variables* |
| `release version 17 not supported` | Your JDK is older than 17. Install 17 or newer and select it in the IDE |
| Every Apply / Check says "Eligibility check unavailable, try later" | The eligibility service is not running. Start it and check http://localhost:8081/eligibility/ping |
| H2 console: "Database not found" | Wrong `data/` folder: see section 7 and use the full path |
| H2 console: "Database may be already in use" | Another process holds the file (for example a second copy of the app). Stop it |
| Login page keeps coming back | The session was lost: log in again. Restarting the app logs everyone out |
| My code change does not show | Restart the app. In IntelliJ make sure you run the module you edited |
| Demo data looks wrong or old | Reset the data (section 7) |
| Error after changing an enum or a column type | Delete `data/` and restart (`ddl-auto=update` does not change existing column types) |
| First build is very slow | Normal: Maven downloads ~150 MB once. Later builds are fast and work offline |
| IntelliJ shows only one project | Right-click the other `pom.xml` → **Add as Maven Project** |

---

## 15. Further documentation

| File | Contents |
| --- | --- |
| [`docs/PLAN.md`](docs/PLAN.md) | Scope decisions, data model, business rules, all routes, each member's step-by-step plan, demo scenarios |
| [`docs/m1.md`](docs/m1.md) | Data layer, ER diagram, inheritance, HQL, SessionFactory/Session/Transaction, eligibility service, viva answers |
| [`docs/m2.md`](docs/m2.md) | Login, interceptor, apply flow, `@Transactional`, AOP, DI and BeanFactory demo, viva answers |
| [`docs/team.md`](docs/team.md) | Team members and register numbers |
