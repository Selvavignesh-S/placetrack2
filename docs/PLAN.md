# PlaceTrack (Simple Edition): Team Implementation Plan

Team plan for the IT23502 mini project. Demo and viva: 21 Oct 2026 (23 Oct is the fallback).
This replaces the earlier "PlaceTrack Team Implementation Plan". That plan had 111 classes, 55 routes and 21 HQL queries,
which is too much to build and learn in 3 days. This one covers **every graded item in the spec** with about 45 small classes.

---

## 1. Scope analysis (why we cut)

The spec says: *"A small, working application that you understand thoroughly is better than a complex application
that you cannot explain."* and *"Do not include technologies or features that you cannot explain."*

### 1.1 Verdict on the old plan

| Old plan item | Verdict | Why |
| --- | --- | --- |
| Core idea (drives, students apply, separate eligibility service) | **Keep** | Fits the spec perfectly: CRUD + HQL + inheritance + REST + a 2nd microservice with a real reason to exist |
| Custom `@RequireRole` annotation + `HandlerInterceptor` per method | **Simplify** | One interceptor that checks the URL prefix (`/student/**`, `/coordinator/**`) |
| Tiers (NORMAL / DREAM / SUPER_DREAM), tier rule T2 | **Cut** | Replace with one boolean `placed`: a placed student cannot apply again |
| Draft / publish drives | **Cut** | A drive is open while its deadline is today or later |
| `@ElementCollection` department set | **Simplify** | `allowedDepartments` is a plain string `"CSE,IT"` |
| Numeric gaps object, failedRules codes, bulk endpoint | **Cut** | The service returns `{eligible, reasons[]}`; the reason text already contains the gap ("CGPA 7.2 is below 7.5") |
| T3 pool preview, T4 timeline + events table, T5 interview wall, T6 stats API | **Cut** | None is graded. Each adds entities, pages and tests |
| 2 aspects writing to an events table with `REQUIRES_NEW` | **Simplify** | 1 aspect that logs every apply attempt to the console (spec: "AOP ... such as logging") |
| 12 REST endpoints, PATCH, error-code table | **Simplify** | 5 REST endpoints, GET and POST only |
| 28-request Postman collection, GitHub Actions | **Simplify** | ~8 Postman requests; CI is optional |
| `LAZY` everywhere + `join fetch` everywhere + `open-in-view=false` | **Cut** | Default fetching + default `open-in-view`. Removes the #1 beginner crash (`LazyInitializationException`). One `join fetch` query is kept to explain it in the viva |
| Separate Form DTO classes | **Cut** | Forms bind straight to the entity |
| Custom exception classes + error page per type | **Simplify** | 2 exception classes in total |

### 1.2 Verdict on the 25 use cases

| ID | Use case | Verdict | Simple version |
| --- | --- | --- | --- |
| UC1 | Register and log in | Keep | Student registers; both roles log in; role decides pages |
| UC2 | Update academic profile | Keep | Student edits CGPA, backlogs, department |
| UC3 | Browse open drives | Simplify | Drives with deadline >= today. No filters |
| UC4 | "Am I eligible?" | Keep | Shows pass/fail + reason list (reason text holds the gap) |
| UC5 | Apply, happy path | Keep | Saved as APPLIED. `@Transactional`, DI, AOP log |
| UC6 | Apply but ineligible | Keep | Nothing saved, reasons shown, aspect logs BLOCKED |
| UC7 | Apply twice / after deadline | Keep | Clear message |
| UC8 | Apply below placed tier | Simplify | "Already placed students cannot apply" |
| UC9 | My applications + timeline | Simplify | List with status. Timeline cut |
| UC10 | Withdraw | Keep | Only from APPLIED or SHORTLISTED |
| UC11, UC12 | Interview experiences | **Cut** | Not graded |
| UC13 | Company CRUD | Keep | |
| UC14 | Create drive with criteria | Simplify | No tier, no draft |
| UC15 | Eligible-pool preview | **Cut** | |
| UC16 | View applicants of a drive | Keep | HQL `join fetch`, sorted by CGPA |
| UC17 | Update applicant status | Keep | APPLIED → SHORTLISTED/REJECTED, SHORTLISTED → SELECTED/REJECTED |
| UC18 | Mark SELECTED atomically | Keep | Status + `student.placed = true` in one transaction. Best transaction demo |
| UC19 | Dashboard + CSV | Simplify | Applications per drive (HQL `GROUP BY`). CSV cut |
| UC20 | GET /api/drives | Keep | |
| UC21 | POST /api/applications | Keep | 201 or 400 with reasons |
| UC22 | GET /api/stats | **Cut** | |
| UC23 | POST /eligibility/check directly | Keep | Proves the service is independent |
| UC24 | Eligibility service down | Keep | "Eligibility check unavailable, try later", nothing saved. **Likely viva question** |
| UC25 | Every apply attempt audited | Simplify | Aspect logs student, drive, outcome, time to the console |

**Result: 19 use cases kept (8 of them simplified), 6 cut.**

### 1.3 Abstract (final, paste into the report)

PlaceTrack is a web-based campus placement tracker built with Spring Boot, Spring MVC, Thymeleaf, Spring Data JPA and
Hibernate. Placement coordinators manage companies and create placement drives with eligibility criteria: minimum CGPA,
maximum backlogs, allowed departments and an application deadline. Students register, keep their academic profile up to
date, browse open drives, check whether they are eligible and apply online. Eligibility is decided by a separate Spring
Boot microservice running on its own port; the main application calls it over REST before saving an application and
shows the student the reasons when a rule fails. If that service is down, the application is not saved and the student
is told to try later. Coordinators review applicants and move them through Applied, Shortlisted, Selected and Rejected;
selecting a student marks the student as placed in the same database transaction. Every application attempt is logged
by an aspect, and REST APIs tested in Postman expose drives and applications. Data is stored in an H2 database through
JPA entities that use JOINED inheritance for users and HQL queries for listings and reports.

### 1.4 Spec coverage (every graded item, and who demos it)

| Spec item | Where in PlaceTrack | Owner |
| --- | --- | --- |
| A. Spring Boot app run in STS | Both projects imported into STS, Run As → Spring Boot App | All |
| A. MVC app | Thymeleaf pages for companies, drives, applications | All |
| A. JPA + Hibernate, annotations, CRUD | `Company`, `Drive` CRUD screens | M1 |
| A. ORM mapping + one ID strategy | `@ManyToOne`, unique constraint, `GenerationType.IDENTITY` | M1 |
| B. ≥ 2 HQL queries | `findOpenDrives`, `findApplicantsOfDrive` (join fetch), `countApplicationsPerDrive` (GROUP BY), plus the demo query | M1 writes, M3 uses the GROUP BY |
| B. Inheritance mapping | `User` → `Student`, `Coordinator` with `InheritanceType.JOINED` | M1 |
| B. SessionFactory, Session, Transaction | `HibernateSessionDemo` (insert then rollback, printed proof) | M1 |
| B. DI + BeanFactory vs ApplicationContext | Constructor injection everywhere + `BeanFactoryDemo` | M2 |
| B. AOP practical use | `ApplyAuditAspect` logs every apply attempt + time taken | M2 |
| B. `@Transactional` | `ApplicationService.apply`, `updateStatus` (SELECTED + placed) | M2, M3 |
| C. REST APIs, GET + POST, Postman | `ApiController` (5 endpoints) + Postman collection | M3 |
| C. Web services, REST vs SOAP | Report chapter | M3 |
| D. Monolith vs microservices, 2nd service, different ports | `eligibility-service` on 8081, called from 8080 | M1 (service), M2 (client) |
| D. Spring Cloud purpose + one feature | Report: OpenFeign would replace our `RestTemplate` client; Resilience4j circuit breaker for UC24 | M3 |
| E. Git, GitHub, Maven | Repo, branches, `./mvnw` builds | All, M3 writes it up |

---

## 2. Tech stack

| Thing | Choice | Note |
| --- | --- | --- |
| Java | 17 (JDK 17 or newer installed) | `java.version` is 17 in both poms |
| Spring Boot | 3.5.16 in both projects | 3.x line: most tutorials match it; imports use `jakarta.*` |
| Build | Maven via wrapper `./mvnw` (Windows: `mvnw.cmd`) | Nobody needs to install Maven |
| IDE | Spring Tool Suite 4 | File → Import → Existing Maven Projects |
| UI | Thymeleaf + Bootstrap 5 from CDN | No JavaScript |
| DB | H2 file DB `./data/placetrack` | Zero setup; browser console at `/h2-console` |
| Passwords | `spring-security-crypto` (BCrypt only) | Not full Spring Security |
| Service call | `RestTemplate` with timeouts | OpenFeign is explained, not used |
| Not used | Lombok, Spring Security, JWT, Docker | Everything must be explainable |

### 2.1 Folder layout

```text
Java Mini project./                 (the GitHub repo root)
  docs/PLAN.md                      this file
  placetrack-app/                   main app, port 8080, package com.placetrack
    src/main/java/com/placetrack/
      PlacetrackApplication.java
      model/        User, Student, Coordinator, Company, Drive, Application, ApplicationStatus      (M1)
      repository/   UserRepository, StudentRepository, CompanyRepository, DriveRepository,
                    ApplicationRepository                                                       (M1)
      service/      CompanyService, DriveService (M1); AuthService, ApplicationService (M2);
                    StudentService (M3)
      controller/   HomeController, CompanyController, DriveController (M1);
                    AuthController, StudentController (M2); ApplicantController,
                    ProfileController, DashboardController (M3)
      api/          ApiController, ApiErrorHandler                                              (M3)
      client/       EligibilityClient, EligibilityResult                                        (M2)
      aspect/       ApplyAuditAspect                                                            (M2)
      exception/    ApplyException, EligibilityUnavailableException                             (M2)
      config/       AppConfig, DataSeeder (M1); SessionUser, LoginInterceptor (M2)
      demo/         HibernateSessionDemo (M1); BeanFactoryDemo (M2)
    src/main/resources/
      application.properties
      templates/fragments/layout.html, coordinator/*.html, student/*.html, login.html ...
  eligibility-service/              2nd microservice, port 8081, package com.placetrack.eligibility
    EligibilityServiceApplication, EligibilityController, EligibilityService,
    EligibilityRequest, EligibilityResponse                                                    (M1)
  postman/PlaceTrack.postman_collection.json                                                   (M3)
  README.md                                                                                     (M3)
```

### 2.2 Coding rules for all three

1. Constructor injection only (no `@Autowired` on fields). It is the DI demo.
2. Controllers call services. Services call repositories. Controllers never touch repositories.
3. No Lombok: plain getters and setters so every line is explainable.
4. Every class starts with a one-line `/** ... */` comment saying what it is for. That line is your viva answer.
5. Before you commit, read your code and make sure you can explain every annotation. If you cannot, delete it or ask.
6. Run `./mvnw test` before every push.

---

## 3. The contract (frozen: everyone codes against this)

### 3.1 Entities (package `com.placetrack.model`, all ids `Long` + `GenerationType.IDENTITY`)

| Entity → table | Fields | Mapping notes |
| --- | --- | --- |
| `User` (abstract) → `users` | `id`, `fullName`, `email` (unique), `password` (BCrypt hash) | `@Inheritance(strategy = JOINED)` |
| `Student` → `students` | `regNo` (unique), `department`, `cgpa` (0–10), `backlogs` (≥0), `placed` (boolean) | extends `User`; `students.id` is also a FK to `users.id` |
| `Coordinator` → `coordinators` | `staffId` (unique) | extends `User` |
| `Company` → `companies` | `id`, `name` (unique), `sector` | |
| `Drive` → `drives` | `id`, `company`, `roleTitle`, `packageLpa`, `minCgpa`, `maxBacklogs`, `allowedDepartments` (`"CSE,IT"`), `deadline` (`LocalDate`) | `@ManyToOne` company. Helpers `getDepartmentList()` and `isOpen()` |
| `Application` → `applications` | `id`, `student`, `drive`, `status`, `appliedAt` | `@ManyToOne` student and drive; unique (`student_id`, `drive_id`) |
| `ApplicationStatus` (enum) | `APPLIED, SHORTLISTED, SELECTED, REJECTED, WITHDRAWN` | stored as text (`EnumType.STRING`) |

Departments are plain strings from this fixed list: `CSE, IT, ECE, EEE, MECH, CIVIL, AIDS` (constant `Drive.DEPARTMENTS`).

### 3.2 Repository methods (M1 writes all of them, M2 and M3 only call them)

| Repository | Method | Kind | Used by |
| --- | --- | --- | --- |
| `UserRepository` | `Optional<User> findByEmail(email)` | derived | M2 login |
| `UserRepository` | `boolean existsByEmail(email)` | derived | M2 register |
| `StudentRepository` | `boolean existsByRegNo(regNo)` | derived | M2 register |
| `CompanyRepository` | `Optional<Company> findByNameIgnoreCase(name)` | derived | M1 duplicate-name check |
| `DriveRepository` | `findOpenDrives(today)` | **HQL** `select d from Drive d where d.deadline >= :today order by d.deadline` | M2 student drive list, M3 API |
| `DriveRepository` | `long countByCompanyId(companyId)` | derived | M1 "company with drives cannot be deleted" |
| `ApplicationRepository` | `boolean existsByStudentIdAndDriveId(studentId, driveId)` | derived | M2 duplicate check |
| `ApplicationRepository` | `long countByDriveId(driveId)` | derived | M1 "drive with applications cannot be deleted" |
| `ApplicationRepository` | `findByStudent(studentId)` | **HQL**, newest first | M2 My Applications, M3 API |
| `ApplicationRepository` | `findApplicantsOfDrive(driveId)` | **HQL** `join fetch a.student`, highest CGPA first | M3 applicants page |
| `ApplicationRepository` | `countApplicationsPerDrive()` | **HQL** `left join` + `GROUP BY`, returns `List<Object[]>` = `[companyName, roleTitle, count]` | M3 dashboard |

### 3.3 Business rules

| # | Rule | Where |
| --- | --- | --- |
| R1 | CGPA >= drive `minCgpa` | eligibility-service |
| R2 | backlogs <= drive `maxBacklogs` | eligibility-service |
| R3 | student department is in `allowedDepartments` | eligibility-service |
| R4 | deadline is today or later | `ApplicationService.apply` (M2) |
| R5 | one application per student per drive | `ApplicationService.apply` + DB unique constraint |
| R6 | a placed student cannot apply | `ApplicationService.apply` |
| R7 | status moves: APPLIED → SHORTLISTED / REJECTED / WITHDRAWN; SHORTLISTED → SELECTED / REJECTED / WITHDRAWN; others are final | `ApplicationService` (M2 withdraw, M3 updateStatus) |
| R8 | SELECTED also sets `student.placed = true`, in the same transaction | `ApplicationService.updateStatus` (M3) |
| R9 | a drive with applications cannot be deleted | `DriveService.delete` (M1) |
| R10 | a company with drives cannot be deleted; company names are unique | `CompanyService` (M1) |

### 3.4 Eligibility service JSON (port 8081)

`POST /eligibility/check`

```json
{ "cgpa": 7.2, "backlogs": 1, "department": "IT",
  "minCgpa": 7.5, "maxBacklogs": 0, "allowedDepartments": ["IT", "CSE"] }
```

Response `200`:

```json
{ "eligible": false,
  "reasons": ["CGPA 7.2 is below the required 7.5", "Backlogs 1 exceed the limit of 0"] }
```

Missing or invalid field → `400` (Spring's default error body). `GET /eligibility/ping` → `{"status":"UP"}`.

### 3.5 All routes

**Eligibility service (M1)**: `POST /eligibility/check`, `GET /eligibility/ping`.

**MVC pages**

| Method + path | Who | Owner |
| --- | --- | --- |
| `GET /` | anyone | M1 placeholder → M2 (redirect by role or to `/login`) |
| `GET /coordinator/companies`, `GET .../new`, `GET .../{id}/edit`, `POST /coordinator/companies`, `POST .../{id}/delete` | coordinator | M1 |
| `GET /coordinator/drives`, `GET .../new`, `GET .../{id}/edit`, `POST /coordinator/drives`, `POST .../{id}/delete` | coordinator | M1 |
| `GET/POST /login`, `GET/POST /register`, `POST /logout` | public | M2 |
| `GET /student/drives`, `GET /student/drives/{id}`, `GET /student/drives/{id}/check`, `POST /student/drives/{id}/apply` | student | M2 |
| `GET /student/applications`, `POST /student/applications/{id}/withdraw` | student | M2 |
| `GET /student/profile`, `POST /student/profile` | student | M3 |
| `GET /coordinator/drives/{id}/applicants`, `POST /coordinator/applications/{id}/status` | coordinator | M3 |
| `GET /coordinator/dashboard` | coordinator | M3 |

**REST API (M3)**

| Method + path | Body | Success | Errors |
| --- | --- | --- | --- |
| `GET /api/drives` | – | 200 list of open drives | – |
| `GET /api/drives/{id}` | – | 200 one drive | 404 |
| `POST /api/companies` | `{name, sector}` | 201 company | 400 |
| `POST /api/applications` | `{studentId, driveId}` | 201 application | 400 `{error, reasons}`, 503 if eligibility service is down |
| `GET /api/students/{id}/applications` | – | 200 list | – |

---

## 4. Prerequisites (do these first, all three members)

- [ ] **JDK 17 or newer** on every laptop: `java -version`. (Maven is NOT needed: use `./mvnw`.)
- [ ] **Spring Tool Suite 4** installed (spec requires STS for running the app).
- [ ] **Git** installed, and one **GitHub repo** created by one member with the other two added as collaborators.
- [ ] **Postman** installed (M3 needs it; others for testing).
- [ ] Internet on the first build (Maven downloads Spring once, ~150 MB).
- [ ] Ports **8080** and **8081** free.
- [ ] Team number, names and register numbers written in `docs/team.md` (report and file names need them).

---

## 5. How work is split (parallel, not a relay)

M1's foundation (entities, repositories, seed data, eligibility service) is the contract. **It is already built** (see
Section 6). Once it is pushed to GitHub, M2 and M3 work **in parallel** on their own branches. The only dependency:
M3's `POST /api/applications` calls M2's `ApplicationService.apply`, so M3 builds that endpoint last.

| | M1: Data + Eligibility service | M2: Login + Apply flow + AOP | M3: Coordinator workflow + REST + Delivery |
| --- | --- | --- | --- |
| Builds | Entities, inheritance, repositories + HQL, seed data, company + drive CRUD, eligibility microservice, SessionFactory demo | Login/register, role guard, student pages, eligibility client, apply/withdraw, `@Transactional`, AOP aspect, DI demo | Applicants + status change (SELECTED transaction), profile, dashboard, REST API, Postman, README, report, Git evidence |
| Viva topics | A: entities, ORM, ID strategy, CRUD. B: inheritance, HQL, SessionFactory/Session/Transaction. D: monolith vs microservices, the 2nd service | A: MVC request flow. B: DI, BeanFactory vs ApplicationContext, AOP, `@Transactional`. D: calling the service, what happens when it is down | C: REST, GET/POST, Postman, web services, REST vs SOAP. D: Spring Cloud. E: Git, GitHub, Maven, DevOps |

**Git workflow**: branches `m1-foundation`, `m2-apply-flow`, `m3-api-delivery`. Small commits like
`feat(m2): add login page`. Merge to `main` via pull request after `./mvnw test` passes. Each member needs visible commits.

**Schedule**

| When | M1 | M2 | M3 |
| --- | --- | --- | --- |
| Sat 17 Oct night | Push foundation, walk team through it (30 min call) | Read Sections 3 + 7, set up STS | Read Sections 3 + 8, set up STS + Postman |
| Sun 18 Oct | Learn own code, screenshots, H2 schema export, report chapters 5–7 | Steps 7.1–7.5 | Steps 8.1–8.4 |
| Mon 19 Oct | Help M2/M3, write `docs/m1.md` | Steps 7.6–7.8, merge | Steps 8.5–8.8 |
| Tue 20 Oct | Full rehearsal on a fresh DB, mock viva | same | Report assembly, final merge, mock viva |
| Wed 21 Oct | **Demo + viva** | | |

**If late, cut in this order**: dashboard (M3 8.4) → profile page (M3 8.3) → withdraw (M2) → Postman tests tab.
**Never cut**: inheritance, ≥2 HQL, SessionFactory demo, DI demo, AOP, GET + POST in Postman, the 2nd service, Git, Maven.

---

## 6. Member 1 plan: Data layer + Eligibility service (BUILT)

Everything below exists in the repo. M1's job now is to **learn it line by line**, take screenshots, and write the
report chapters. Steps are listed in build order so M1 can explain how it was made.

### 6.1 Project skeletons
- `placetrack-app/pom.xml`: parent `spring-boot-starter-parent 3.5.16`, Java 17. Dependencies: `web`, `thymeleaf`,
  `data-jpa`, `validation`, `aop` (for M2), `h2` (runtime), `spring-security-crypto` (BCrypt), `test`.
- `eligibility-service/pom.xml`: `web`, `validation`, `test`. No database.
- `application.properties` (main app): port 8080, H2 file URL `jdbc:h2:file:./data/placetrack`, `ddl-auto=update`
  (Hibernate creates/updates tables from the entities), `show-sql=true` (prints SQL for learning), H2 console on,
  `eligibility.url=http://localhost:8081`.
- `eligibility-service/application.properties`: `server.port=8081`.

### 6.2 Entities (`model/`)
- `User`: `@Entity @Table(name="users") @Inheritance(strategy = InheritanceType.JOINED)`, `abstract`.
  `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`. `email` is `@Column(unique = true)`.
- `Student extends User` (`students` table), `Coordinator extends User` (`coordinators` table).
  JOINED = parent table + one table per child, joined on `id`. Reading a `Student` makes Hibernate run
  `select ... from students s join users u on s.id = u.id`.
- `Company`: `@NotBlank` name (unique), sector.
- `Drive`: `@ManyToOne(optional = false) Company company`; `@DateTimeFormat(iso = DATE) LocalDate deadline` so the
  HTML `<input type="date">` binds; `getDepartmentList()` splits `"CSE,IT"`; `isOpen()` compares deadline with today.
- `Application`: `@ManyToOne` student and drive, `@Enumerated(EnumType.STRING) status`,
  `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"student_id","drive_id"}))`.
- Check: start the app, open `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/placetrack`, user `sa`,
  empty password) and see tables `USERS, STUDENTS, COORDINATORS, COMPANIES, DRIVES, APPLICATIONS`.

### 6.3 Repositories (`repository/`)
- All extend `JpaRepository<Entity, Long>` which gives `save, findById, findAll, deleteById, count` for free (CRUD).
- Methods exactly as Section 3.2. Derived methods are generated from the method name; `@Query` methods are HQL
  (written against **entity and field names**, not table and column names).

### 6.4 Seed data (`config/DataSeeder`)
- `CommandLineRunner`, runs on startup only when `users` is empty. Password for every account: `Pass@123` (BCrypt).
- See Section 9 for the exact records.
- Reset the data any time: stop the app, delete `placetrack-app/data/`, start again.

### 6.5 Layout (`templates/fragments/layout.html`)
- Fragment `head(title)`: Bootstrap CSS from CDN. Fragment `nav`: links. Fragment `flash`: shows `message` (green) and
  `error` (red). Every page uses:
  `<head th:replace="~{fragments/layout :: head('Title')}"></head>` and
  `<nav th:replace="~{fragments/layout :: nav}"></nav>` and `<div th:replace="~{fragments/layout :: flash}"></div>`.

### 6.6 Company CRUD
- `CompanyService`: `findAll`, `getById` (404 if missing), `save` (create and update; duplicate name → error),
  `delete` (refused when the company has drives, rule R10).
- `CompanyController`: list, new form, edit form, save, delete. Uses **Post-Redirect-Get**: every POST ends with
  `redirect:` so a browser refresh does not resubmit. Messages travel through `RedirectAttributes.addFlashAttribute`.
- Templates `coordinator/companies.html`, `coordinator/company-form.html`.

### 6.7 Drive CRUD
- `DriveService`: `findAll`, `findOpenDrives`, `getById`, `save(drive)`, `delete` (refused with applications, R9). The controller loads the company with `companyService.getById(companyId)` and sets it on the drive before saving.
- `DriveController`: same 5 routes as companies. The form posts `companyId` as a separate parameter and
  `allowedDepartments` as checkboxes, which Spring joins into `"CSE,IT"`.
- Templates `coordinator/drives.html` (Open/Closed badge), `coordinator/drive-form.html`.

### 6.8 Hibernate SessionFactory demo (`demo/HibernateSessionDemo`)
- Runs only with the `demo` profile:
  `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` (in `placetrack-app`).
- Gets the `SessionFactory` from JPA (`entityManagerFactory.unwrap(SessionFactory.class)`), opens a `Session`, begins
  a `Transaction`, runs an HQL GROUP BY, inserts a company, counts, **rolls back**, counts again → proves nothing was saved.
- Viva: **SessionFactory** = one per app, heavy, thread-safe, built from the mappings, makes Sessions.
  **Session** = one unit of work, not thread-safe, holds the first-level cache, wraps a JDBC connection.
  **Transaction** = all-or-nothing; `commit` writes, `rollback` undoes. Spring's `@Transactional` does
  begin/commit/rollback for us.

### 6.9 Eligibility service (`eligibility-service/`)
- `EligibilityRequest` (Java `record` with `@NotNull`, `@DecimalMin/@DecimalMax`, `@NotBlank`, `@NotEmpty`),
  `EligibilityResponse(boolean eligible, List<String> reasons)`.
- `EligibilityService.check(request)`: three `if`s (R1–R3), each failure adds a reason.
- `EligibilityController`: `POST /eligibility/check` with `@Valid @RequestBody`, `GET /eligibility/ping`.
- Why separate (viva): it has its own port, own process, no database, and can be changed, scaled or restarted without
  touching the main app. The main app only knows its URL and JSON shape. That is the microservice idea.

### 6.10 Tests
- `eligibility-service`: `EligibilityServiceTest` (equal CGPA passes, 7.2 vs 7.5 fails, backlogs, wrong department,
  all three fail together), `EligibilityControllerTest` (`@WebMvcTest`: valid → 200, missing field → 400).
- `placetrack-app`: `RepositoryTest` (`@DataJpaTest`: open drives exclude expired, applicants sorted by CGPA,
  count per drive includes drives with 0 applications), `PlacetrackAppTests` (whole app starts).
- Run: `./mvnw test` in each folder.

### 6.11 M1 remaining tasks
Done:
- [x] Schema and sample data exported to `docs/report/schema.sql` and `docs/report/schema-and-sample-data.sql`.
- [x] `docs/m1.md`: file list, run steps, ER diagram, inheritance with the real SQL, HQL explained, Session demo
      output, eligibility contract, monolith vs microservices, tests, notes for M2/M3, viva answers.
- [x] Postman collection `postman/PlaceTrack.postman_collection.json` (folder "00 Eligibility service", 5 requests
      with tests) and environment `postman/PlaceTrack.local.postman_environment.json`.
- [x] `docs/team.md` template.

Only a person can do these:
- [ ] Fill in `docs/team.md` (names, register numbers, team number).
- [ ] Take the 11 screenshots listed in `docs/m1.md` section 10.
- [ ] Walk M2 and M3 through the code (30 min screen share), using `docs/m1.md`.
- [ ] Create the GitHub repo and push.

**M1 viva questions to rehearse**: JOINED vs SINGLE_TABLE vs TABLE_PER_CLASS (and the SQL JOINED produces);
IDENTITY vs SEQUENCE vs AUTO; what each annotation on `Drive` does; EAGER vs LAZY and what `join fetch` solves;
HQL vs SQL (HQL uses class/field names); SessionFactory vs Session vs Transaction; what `ddl-auto=update` does;
monolith vs microservice; what happens when the eligibility service is down.

---

## 7. Member 2 plan: Login, role guard, apply flow, AOP, DI demo

Branch `m2-apply-flow`. About 8 hours. Files are listed with every method you write.

**Status: BUILT.** See `docs/m2.md` for what exists, how it was tested and the viva notes. The steps below are
kept as the build order. Differences from this text: `/check` is a GET, `AuthController` also has an
`@InitBinder` allow-list against overposting, and `LoginInterceptor` checks `request.getSession(false)`.

### 7.1 Session user + login (≈ 90 min)
1. `config/SessionUser.java`: `public record SessionUser(Long id, String name, String role) {}` with a constant
   `public static final String KEY = "user";`. `role` is `"STUDENT"` or `"COORDINATOR"`.
2. `service/AuthService.java` (constructor-injects `UserRepository`, `StudentRepository`, `PasswordEncoder`):
   - `Optional<SessionUser> login(String email, String password)`: `findByEmail`, then
     `passwordEncoder.matches(password, user.getPassword())`; role = `user instanceof Student ? "STUDENT" : "COORDINATOR"`.
   - `void register(Student s, String rawPassword)`: throw `IllegalArgumentException("Email already registered")` if
     `existsByEmail`, same for `existsByRegNo`; `s.setPassword(passwordEncoder.encode(rawPassword))`; save.
3. `controller/AuthController.java`:
   - `GET /login` → view `login`.
   - `POST /login(@RequestParam email, @RequestParam password, HttpSession session, RedirectAttributes ra)`:
     on success `session.setAttribute(SessionUser.KEY, user)` and `redirect:/`; else flash error, `redirect:/login`.
   - `GET /register` → model `student` = `new Student()`, `departments` = `Drive.DEPARTMENTS`, view `register`.
   - `POST /register(@Valid @ModelAttribute Student student, BindingResult result, @RequestParam String rawPassword)`:
     validation errors → re-show form; `IllegalArgumentException` → show its message; else flash "Registered, please
     log in", `redirect:/login`.
   - `POST /logout(HttpSession session)` → `session.invalidate()`, `redirect:/login`.
4. **Delete** M1's `controller/HomeController` and put `GET /` in `AuthController`: no user → `redirect:/login`;
   student → `redirect:/student/drives`; coordinator → `redirect:/coordinator/drives`.
5. Templates `login.html` (email, password, link to register; show "Demo: student1@placetrack.edu / Pass@123"),
   `register.html` (fullName, email, password, regNo, department dropdown, cgpa, backlogs).
6. Update `fragments/layout.html` `nav`: show the user's name and a logout form when `session.user` exists;
   student links (Drives, My Applications, Profile) or coordinator links (Companies, Drives, Dashboard) by role.
   In Thymeleaf: `th:if="${session.user != null and session.user.role == 'STUDENT'}"`.
   Note: no separate `RegisterForm` class is needed: the `Student` entity is the form. Name the HTML password input
   `rawPassword` (a separate `@RequestParam`), because `Student.password` holds the hash and is set in `AuthService`.
   **Check**: log in as `student1@placetrack.edu` / `Pass@123` and as `coord1@placetrack.edu` / `Pass@123`.

### 7.2 Role guard (≈ 30 min)
1. `config/LoginInterceptor implements HandlerInterceptor`, method `preHandle`:
   - `SessionUser user = (SessionUser) request.getSession().getAttribute(SessionUser.KEY);`
   - path starts with `/student/` and role is not STUDENT, or `/coordinator/` and role is not COORDINATOR:
     if user is null `response.sendRedirect("/login")` else `response.sendError(403)`; return `false`.
   - otherwise return `true`.
2. In M1's `config/AppConfig` add `implements WebMvcConfigurer` and
   `addInterceptors(registry) { registry.addInterceptor(new LoginInterceptor()).addPathPatterns("/student/**", "/coordinator/**"); }`
3. **Check**: logged out, `/coordinator/drives` → login page; as student → 403 page.

### 7.3 Exceptions + eligibility client (≈ 60 min)
1. `exception/ApplyException extends RuntimeException`: field `List<String> reasons` (empty unless the student is
   ineligible); constructors `ApplyException(String message)` and `ApplyException(String message, List<String> reasons)`.
2. `exception/EligibilityUnavailableException extends RuntimeException`: message
   `"Eligibility check unavailable, try later"`.
3. `client/EligibilityResult.java`: `public record EligibilityResult(boolean eligible, List<String> reasons) {}`.
   (A copy of the service's response, on purpose: microservices do not share classes.)
4. `client/EligibilityClient.java` (`@Component`):
   - Constructor takes `RestTemplateBuilder builder` and `@Value("${eligibility.url}") String baseUrl`; builds
     `builder.connectTimeout(Duration.ofSeconds(2)).readTimeout(Duration.ofSeconds(3)).build()`.
   - `EligibilityResult check(Student s, Drive d)`: builds a `Map.of("cgpa", s.getCgpa(), "backlogs", ...,
     "allowedDepartments", d.getDepartmentList())`, calls
     `restTemplate.postForObject(baseUrl + "/eligibility/check", body, EligibilityResult.class)`.
   - Wrap the call in `try { ... } catch (RestClientException e) { throw new EligibilityUnavailableException(); }`
     (covers connection refused, timeout and 5xx).
   **Check**: with the service stopped, the call throws `EligibilityUnavailableException` (UC24).

### 7.4 ApplicationService (≈ 90 min) — the heart of the project
`service/ApplicationService.java` (constructor-injects `StudentRepository`, `DriveService`, `ApplicationRepository`,
`EligibilityClient`):

```text
@Transactional
Application apply(Long studentId, Long driveId):
  1. student = studentRepository.findById(studentId) (404 if missing)
  2. drive = driveService.getById(driveId)
  3. if student.isPlaced()                      -> throw ApplyException("You are already placed")          R6
  4. if !drive.isOpen()                         -> throw ApplyException("The deadline for this drive has passed")  R4
  5. if existsByStudentIdAndDriveId(...)        -> throw ApplyException("You have already applied to this drive") R5
  6. result = eligibilityClient.check(student, drive)   (may throw EligibilityUnavailableException)
  7. if !result.eligible()                      -> throw ApplyException("You are not eligible", result.reasons())
  8. new Application(student, drive), status APPLIED, appliedAt now -> save and return

EligibilityResult checkEligibility(Long studentId, Long driveId):   steps 1, 2, 6 only. Saves nothing.

List<Application> myApplications(Long studentId):   applicationRepository.findByStudent(studentId)

@Transactional
void withdraw(Long applicationId, Long studentId):
  application = findById (404 if missing); if it does not belong to studentId -> 404 as well
  status must be APPLIED or SHORTLISTED else throw ApplyException("Cannot withdraw at this stage")    R7
  set WITHDRAWN (no save() call needed: the entity is managed inside the transaction = dirty checking)
```
Viva: `@Transactional` makes Spring wrap the method in a proxy that begins a transaction before and commits after;
any `RuntimeException` rolls it back. All our exceptions are `RuntimeException`, so a failed apply saves nothing.

### 7.5 Student pages (≈ 90 min)
`controller/StudentController.java` (all methods read `@SessionAttribute(SessionUser.KEY) SessionUser user`):
- `GET /student/drives` → model `drives` = `driveService.findOpenDrives()`, view `student/drives`.
- `GET /student/drives/{id}` → model `drive`, view `student/drive-detail`.
- `GET /student/drives/{id}/check` (a GET because it only reads) → model `drive` + `result` (`checkEligibility`), same view. Catch
  `EligibilityUnavailableException` → model `error`.
- `POST /student/drives/{id}/apply` → try `apply`, flash "Applied successfully", `redirect:/student/applications`;
  catch `ApplyException e` → flash `error` = message and `reasons` = `e.getReasons()`, `redirect:/student/drives/{id}`;
  catch `EligibilityUnavailableException e` → flash `error` = message, same redirect.
- `GET /student/applications` → model `applications`, view `student/applications`.
- `POST /student/applications/{id}/withdraw` → withdraw, flash, `redirect:/student/applications`.

Templates: `student/drives.html` (table: company, role, package, min CGPA, deadline, "View" link),
`student/drive-detail.html` (criteria card, two forms: "Check eligibility", "Apply"; a green/red panel for `result`;
a red list for flash `reasons`), `student/applications.html` (table with status badge; Withdraw button only when status is
APPLIED or SHORTLISTED).

### 7.6 AOP: `aspect/ApplyAuditAspect` (≈ 45 min)
```java
@Aspect @Component
public class ApplyAuditAspect {
    private static final Logger log = LoggerFactory.getLogger(ApplyAuditAspect.class);

    @Around("execution(* com.placetrack.service.ApplicationService.apply(..))")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();           // [studentId, driveId]
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("AUDIT apply student={} drive={} outcome=SUCCESS time={}ms", args[0], args[1], System.currentTimeMillis() - start);
            return result;
        } catch (RuntimeException e) {
            log.info("AUDIT apply student={} drive={} outcome=BLOCKED reason=\"{}\" time={}ms", args[0], args[1], e.getMessage(), System.currentTimeMillis() - start);
            throw e;
        }
    }
}
```
Viva terms: **aspect** (this class), **advice** (`@Around` method), **pointcut** (the `execution(...)` expression),
**join point** (each call of `apply`), **weaving** (Spring creates a proxy around `ApplicationService`).
The service has no logging code: that is the "cross-cutting concern" AOP removes.
**Check**: apply once successfully and once blocked; the console shows two `AUDIT` lines (UC25).

### 7.7 DI demo: `demo/BeanFactoryDemo` (≈ 30 min)
A class with a `main` method (Run As → Java Application), plus a tiny static nested `Greeter` class whose constructor
prints `"Greeter created"`:
1. `DefaultListableBeanFactory factory = new DefaultListableBeanFactory();`
   `factory.registerBeanDefinition("greeter", new RootBeanDefinition(Greeter.class));`
   print "BeanFactory ready" → nothing created yet; then `factory.getBean("greeter")` → "Greeter created" prints now
   (**lazy**).
2. `AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(); ctx.registerBean(Greeter.class);`
   `ctx.refresh();` → "Greeter created" prints during `refresh()`, before any `getBean` (**eager**). Close the context.
Viva table: BeanFactory = basic container, lazy, DI only. ApplicationContext = extends BeanFactory, eager singletons,
annotations, AOP, events, i18n, environment. Spring Boot creates an ApplicationContext for us.

### 7.8 Test + handoff (≈ 45 min)
- `ApplicationServiceTest` (JUnit 5 + Mockito, `@ExtendWith(MockitoExtension.class)`): happy path saves APPLIED;
  placed student throws; expired drive throws; duplicate throws; ineligible throws with reasons and `save` never called;
  client throws `EligibilityUnavailableException` → nothing saved.
- Write `docs/m2.md`: the apply flow above, `@Transactional`, AOP terms, the BeanFactory demo output, UC24.
- **M2 done when**: scenarios S1–S10 in Section 9 work in the browser; tests green; PR merged.

**M2 viva questions**: MVC flow (DispatcherServlet → controller → service → view resolver → Thymeleaf); Post-Redirect-Get;
constructor vs setter vs field injection; bean scopes (singleton, prototype, request, session); BeanFactory vs
ApplicationContext; AOP terms; how `@Transactional` works and when it rolls back; why BCrypt; what happens when the
eligibility service is slow or down (timeouts → exception → message, nothing saved); `RestTemplate` vs OpenFeign.

---

## 8. Member 3 plan: Coordinator workflow, REST API, delivery

Branch `m3-api-delivery`. About 8 hours plus the report.

**Status: CODE BUILT.** See `docs/m3.md` for what exists, how it was tested, the concept notes and viva answers.
Still to do by people: screenshots, the architecture image, the report PDF, `docs/team.md`, the optional CI workflow.
Differences from the text below: the applicants page uses one form per row with one button per allowed move;
the profile uses a `@ModelAttribute` loader plus an `@InitBinder` allow-list; `POST /api/companies` ignores any `id`.

### 8.1 Applicants + status update (≈ 90 min)
1. Add to M2's `ApplicationService`:
   - `List<Application> applicantsOfDrive(Long driveId)` → `applicationRepository.findApplicantsOfDrive(driveId)`.
   - `@Transactional void updateStatus(Long applicationId, ApplicationStatus next)`:
     ```text
     app = findById (404); current = app.getStatus()
     allowed = (current == APPLIED     && next in {SHORTLISTED, REJECTED})
            || (current == SHORTLISTED && next in {SELECTED, REJECTED})
     if !allowed -> throw ApplyException("Cannot move from " + current + " to " + next)        R7
     app.setStatus(next)
     if next == SELECTED -> app.getStudent().setPlaced(true)                                  R8
     ```
     Both changes are in one transaction: either both are saved or neither (UC18).
2. `controller/ApplicantController.java`:
   - `GET /coordinator/drives/{id}/applicants` → model `drive`, `applications`, view `coordinator/applicants`.
   - `POST /coordinator/applications/{id}/status(@RequestParam ApplicationStatus status, @RequestParam Long driveId)`
     → `updateStatus`; catch `ApplyException` into flash `error`; `redirect:/coordinator/drives/{driveId}/applicants`.
3. Template `coordinator/applicants.html`: name, reg no, department, CGPA, backlogs, status badge, and buttons
   Shortlist / Select / Reject (shown only when allowed). Add an "Applicants" link per row in M1's `drives.html`.
4. Transaction demo for the report: temporarily add `if (next == SELECTED) throw new RuntimeException("demo")`
   **after** `setPlaced(true)`, select someone, show in the H2 console that neither the status nor `placed` changed.
   Remove the line again.

### 8.2 Student profile (≈ 45 min)
1. `service/StudentService`: `Student getById(id)`, `void updateProfile(Long id, Student form)` → copy `fullName`,
   `department`, `cgpa`, `backlogs` onto the loaded student and save (never copy `email`, `regNo`, `password`, `placed`).
2. `controller/ProfileController`: `GET /student/profile` (model `student`, `departments`),
   `POST /student/profile(@Valid @ModelAttribute("student") Student form, BindingResult result, ...)`.
   Note: the form has no password/email fields, so `@NotBlank email` would fail: put `email` as a read-only hidden field,
   or validate only the fields you edit by hand. Simplest: include `email` and `regNo` as `readonly` inputs.
   **Security:** copy M2's `@InitBinder` from `AuthController` and allow only
   `fullName`, `department`, `cgpa`, `backlogs`, so nobody can post `placed=true` or another `id`.
   After saving, put a new `SessionUser` (with the new name) into the session so the navbar updates.
3. Template `student/profile.html`.

### 8.3 Coordinator dashboard (≈ 45 min)
1. `controller/DashboardController`: `GET /coordinator/dashboard` → model `rows` =
   `applicationRepository.countApplicationsPerDrive()` through a one-line method in `DriveService`
   (`List<Object[]> applicationCounts()`), plus `totalStudents`, `placedStudents` (add
   `long countByPlacedTrue()` to `StudentRepository`).
2. Template `coordinator/dashboard.html`: two number cards + table (company, role, applications). In Thymeleaf,
   `row[0]`, `row[1]`, `row[2]`.
3. Change M2's `GET /` so coordinators land on `/coordinator/dashboard`.

### 8.4 REST API (≈ 120 min)
1. `api/ApiController` (`@RestController @RequestMapping("/api")`), endpoints from Section 3.5:
   - `GET /drives` → `driveService.findOpenDrives()`.
   - `GET /drives/{id}` → `driveService.getById(id)`.
   - `POST /companies(@Valid @RequestBody Company c)` → `ResponseEntity.status(201).body(companyService.save(c))`.
   - `POST /applications(@RequestBody ApplyRequest req)` where `record ApplyRequest(Long studentId, Long driveId)` →
     201 with the saved application.
   - `GET /students/{id}/applications` → `applicationService.myApplications(id)`.
   - Entities serialize straight to JSON. `Student` must not leak the password hash: M1 already put
     `@JsonIgnore` on `User.password`.
2. `api/ApiErrorHandler` (`@RestControllerAdvice(assignableTypes = ApiController.class)`):
   - `ApplyException` → 400 `{"error": message, "reasons": [...]}`.
   - `EligibilityUnavailableException` → 503 `{"error": message}`.
   - `MethodArgumentNotValidException` → 400 `{"error": "Validation failed"}`.
   - `ResponseStatusException` → its status with `{"error": reason}`.
3. Shortcut to say out loud in the viva: the API takes `studentId` from the body and has no login. A real system would
   take it from the session or a token (JWT). We left it out to keep the API easy to test.

### 8.5 Postman (≈ 60 min)
Environment: `baseUrl = http://localhost:8080`, `eligUrl = http://localhost:8081`. Collection
`postman/PlaceTrack.postman_collection.json`, each request with a Tests tab checking the status code:
1. `GET {{eligUrl}}/eligibility/ping` → 200
2. `POST {{eligUrl}}/eligibility/check` eligible body → 200, `eligible == true`
3. same, ineligible body (Section 3.4) → 200, 2 reasons
4. same, missing `cgpa` → 400
5. `GET {{baseUrl}}/api/drives` → 200
6. `POST {{baseUrl}}/api/companies` → 201
7. `POST {{baseUrl}}/api/applications` `{studentId: <student1 id>, driveId: <Nimbus id>}` → 201; again → 400
8. `POST {{baseUrl}}/api/applications` with student2 → 400 with reasons
9. Stop the eligibility service, repeat 7 for another drive → 503
Run the collection with the Runner on a fresh DB and screenshot the summary.

### 8.6 Tests (≈ 45 min)
- `ApiControllerTest` (`@WebMvcTest(ApiController.class)`, services mocked with `@MockitoBean`): GET drives 200;
  POST application 201; `ApplyException` → 400; `EligibilityUnavailableException` → 503.
- `StatusUpdateTest` (Mockito): APPLIED→SELECTED refused; SHORTLISTED→SELECTED sets `placed = true`.

### 8.7 README, report, Git evidence (≈ 3 h)
- `README.md`: what it is, architecture sketch, prerequisites (Section 4), run steps
  (1. `cd eligibility-service && ./mvnw spring-boot:run`, 2. `cd placetrack-app && ./mvnw spring-boot:run`,
  3. open `http://localhost:8080`), seeded logins, H2 console URL, how to reset data, how to run tests, Postman import.
- Architecture diagram `docs/report/architecture.png`: Browser/Postman → placetrack-app (controllers → services →
  repositories → H2; `EligibilityClient` → eligibility-service :8081).
- Concept notes for the report: REST vs SOAP table (JSON vs XML envelope, no contract vs WSDL, light vs heavy);
  monolith vs microservices table; Spring Cloud (OpenFeign, Eureka, Gateway, Config Server, Resilience4j: which one
  would replace our client and our hand-written timeout handling); DevOps (CI/CD, tools table: Git, GitHub, Maven used;
  Jenkins, Docker, GitHub Actions named).
- Optional: `.github/workflows/build.yml` running `./mvnw -B test` in both folders.
- Report `TeamNo_PlaceTrack_Report.pdf` assembled from `docs/m1.md`, `docs/m2.md`, `docs/m3.md` + screenshots.
- Check `git shortlog -sn` shows commits from all three.

**M3 viva questions**: REST principles (resources, HTTP verbs, stateless, status codes); GET vs POST and idempotency;
`@RestController` vs `@Controller`; how Jackson turns JSON into a record; what `@Valid` does; web service, REST vs SOAP;
Postman environments and tests; Spring Cloud features; Maven lifecycle (`clean`, `compile`, `test`, `package`) and
`pom.xml`; Git branch, commit, pull request, merge; what DevOps means.

---

## 9. Seed data and demo scenarios

Every password is `Pass@123`.

| Email | Name | Role | Dept | CGPA | Backlogs | Placed |
| --- | --- | --- | --- | --- | --- | --- |
| `coord1@placetrack.edu` | Dr. Meera Nair | Coordinator (STF001) | | | | |
| `coord2@placetrack.edu` | Prof. Arjun Rao | Coordinator (STF002) | | | | |
| `student1@placetrack.edu` | Aarav Kumar (23IT001) | Student | IT | 8.6 | 0 | no |
| `student2@placetrack.edu` | Diya Menon (23IT002) | Student | IT | 7.2 | 1 | no |
| `student3@placetrack.edu` | Karthik Raman (23CS003) | Student | CSE | 8.0 | 0 | **yes** |
| `student4@placetrack.edu` | Sneha Iyer (23ME004) | Student | MECH | 7.9 | 0 | no |
| `student5@placetrack.edu` | Rohan Das (23EC005) | Student | ECE | 9.1 | 0 | no |
| `student6@placetrack.edu` | Priya Natarajan (23CS006) | Student | CSE | 6.4 | 2 | no |

| Code | Company | Role | LPA | Min CGPA | Max backlogs | Departments | Deadline |
| --- | --- | --- | --- | --- | --- | --- | --- |
| D1 | Zephyr Systems | Software Engineer Trainee | 4.5 | 6.0 | 2 | CSE, IT, ECE | today + 10 |
| D2 | Nimbus Cloud | Cloud Engineer | 9.0 | 7.5 | 0 | IT, CSE | today + 7 |
| D3 | Quanta Analytics | Data Scientist | 14.0 | 8.5 | 0 | CSE, IT, ECE | today + 14 |
| D4 | Zephyr Systems | Support Engineer | 3.8 | 6.0 | 3 | all | today − 3 (expired) |
| D5 | Orion Manufacturing | Graduate Engineer Trainee | 4.0 | 6.5 | 1 | MECH, ECE, EEE, CIVIL | today + 9 |

Companies: Zephyr Systems, Nimbus Cloud, Quanta Analytics, Orion Manufacturing, Helio Power (no drives: can be deleted).

Applications: A1 student3 → D1 **SELECTED** (that is why student3 is placed), A2 student5 → D1 SHORTLISTED,
A3 student5 → D3 APPLIED, A4 student4 → D5 APPLIED.

Expected rows after first start: users 8, students 6, coordinators 2, companies 5, drives 5, applications 4.

| # | Action | Expected | UC |
| --- | --- | --- | --- |
| S1 | student1 applies to D2 | Saved APPLIED; console `AUDIT ... SUCCESS` | UC5, UC25 |
| S2 | student2 checks, then applies to D2 | Not eligible: "CGPA 7.2 is below the required 7.5", "Backlogs 1 exceed the limit of 0"; nothing saved; `AUDIT ... BLOCKED` | UC4, UC6 |
| S3 | student4 applies to D2 | Only the department reason | UC6 |
| S4 | student3 (placed) applies to D2 | "You are already placed" | UC8 |
| S5 | student1 applies to D2 again | "You have already applied" | UC7 |
| S6 | student1 opens D4 by URL `/student/drives/{id}` and applies | "Deadline has passed"; D4 not in the list | UC7 |
| S7 | Stop eligibility-service; student1 applies to D1 | "Eligibility check unavailable, try later"; nothing saved | UC24 |
| S8 | student1 withdraws the D2 application | WITHDRAWN, no more buttons | UC10 |
| S9 | Logged out → `/student/drives`; student → `/coordinator/drives` | Login page; 403 | UC1 |
| S10 | Register with an existing email | Error, no account | UC1 |
| S11 | coord1 selects A2 (SHORTLISTED → SELECTED) | A2 SELECTED and student5 placed, one transaction | UC17, UC18 |
| S12 | coord1 moves A4 APPLIED → SELECTED | Refused (must shortlist first) | UC17 |
| S13 | coord1 deletes Zephyr Systems / deletes D1 | Both refused (has drives / has applications) | UC13 |
| S14 | coord1 deletes Helio Power | Deleted | UC13 |
| S15 | Postman `GET /api/drives`, `POST /api/applications` | 200 list; 201 or 400 with reasons | UC20, UC21 |
| S16 | Postman `POST :8081/eligibility/check` | Same reasons as the website | UC23 |

### Demo script (≈ 10 min)
1. **M3**: architecture diagram, both services running on 8080 and 8081.
2. **M1**: log in as coord1, add a company, create a drive; show H2 console with `users` + `students` tables; run the
   Session demo output; Postman call to `:8081/eligibility/check`.
3. **M2**: student2 checks D2 and sees reasons (S2); student1 applies (S1); show the `AUDIT` console lines; stop the
   eligibility service and apply (S7).
4. **M3**: coord1 shortlists then selects (S11); Postman GET + POST; GitHub commit history.
