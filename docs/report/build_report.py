#!/usr/bin/env python3
"""
Builds the PlaceTrack project report PDF.

Reads the team details from docs/team.md and the images from docs/screenshots,
and writes docs/report/<TeamNo>_PlaceTrack_Report.pdf.

Run (needs the reportlab and pillow Python packages):
    python3 -m venv /tmp/report-venv && /tmp/report-venv/bin/pip install reportlab pillow
    /tmp/report-venv/bin/python docs/report/build_report.py
"""
import os
import re

from PIL import Image as PILImage
from reportlab.graphics.shapes import Drawing, Line, Polygon, Rect, String
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import cm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (BaseDocTemplate, CondPageBreak, Frame, Image, KeepTogether, NextPageTemplate,
                                PageBreak, PageTemplate, Paragraph, Preformatted, Spacer, Table, TableStyle)
from reportlab.platypus.flowables import PageBreakIfNotEmpty
from reportlab.platypus.tableofcontents import TableOfContents
from io import BytesIO

HERE = os.path.dirname(os.path.abspath(__file__))
DOCS = os.path.dirname(HERE)
SHOTS = os.path.join(DOCS, "screenshots")

# ---------------------------------------------------------------- fonts (plain serif, like a printed report)
FONT_DIR = "/usr/share/fonts/liberation"
try:
    for name, file in [("Serif", "LiberationSerif-Regular.ttf"), ("Serif-Bold", "LiberationSerif-Bold.ttf"),
                       ("Serif-Italic", "LiberationSerif-Italic.ttf"), ("Mono", "LiberationMono-Regular.ttf")]:
        pdfmetrics.registerFont(TTFont(name, os.path.join(FONT_DIR, file)))
    pdfmetrics.registerFontFamily("Serif", normal="Serif", bold="Serif-Bold", italic="Serif-Italic")
    SERIF, SERIF_B, SERIF_I, MONO = "Serif", "Serif-Bold", "Serif-Italic", "Mono"
except Exception:  # fallback to the built-in PDF fonts
    SERIF, SERIF_B, SERIF_I, MONO = "Times-Roman", "Times-Bold", "Times-Italic", "Courier"

GRID = colors.HexColor("#9a9a9a")
HEAD_BG = colors.HexColor("#ededed")
FULL_W = 16.4 * cm

# ---------------------------------------------------------------- styles
body = ParagraphStyle("body", fontName=SERIF, fontSize=11, leading=15.5, alignment=TA_JUSTIFY, spaceAfter=6)
bullet_style = ParagraphStyle("bullet", parent=body, alignment=TA_LEFT, leftIndent=14, bulletIndent=3, spaceAfter=3)
h1 = ParagraphStyle("h1", fontName=SERIF_B, fontSize=16, keepWithNext=1, leading=20, spaceBefore=4, spaceAfter=10)
h2 = ParagraphStyle("h2", fontName=SERIF_B, fontSize=13, keepWithNext=1, leading=17, spaceBefore=10, spaceAfter=5)
h3 = ParagraphStyle("h3", fontName=SERIF_B, fontSize=11.5, keepWithNext=1, leading=15, spaceBefore=6, spaceAfter=3)
cap = ParagraphStyle("cap", fontName=SERIF_I, fontSize=9.5, leading=12, alignment=TA_CENTER, spaceBefore=4,
                     spaceAfter=12)
cell = ParagraphStyle("cell", fontName=SERIF, fontSize=9.5, leading=12)
cell_b = ParagraphStyle("cellb", parent=cell, fontName=SERIF_B)
code_style = ParagraphStyle("code", fontName=MONO, fontSize=8.2, leading=10.5)
centre = ParagraphStyle("centre", parent=body, alignment=TA_CENTER)
toc_1 = ParagraphStyle("toc1", fontName=SERIF, fontSize=11, leading=16, leftIndent=0)
toc_2 = ParagraphStyle("toc2", fontName=SERIF, fontSize=10, leading=13, leftIndent=18)


# ---------------------------------------------------------------- team details from docs/team.md
def read_team():
    text = open(os.path.join(DOCS, "team.md"), encoding="utf-8").read()

    def field(name):
        m = re.search(r"^\| " + re.escape(name) + r" \| (.*?) \|", text, re.M)
        return m.group(1).strip() if m else "TODO"

    members = re.findall(r"^\| (M[123]) \| (.*?) \| (.*?) \| (.*?) \|", text, re.M)
    team_no = field("Team number")
    # "[No team number]", "-", "TODO" or empty all mean: no team number
    if not re.search(r"\d", team_no):
        team_no = ""
    info = {k: field(k) for k in ("Course", "Course code", "Department", "Semester")}
    return team_no, field("GitHub repository"), members, info


def blank(value, width=20):
    """Shows a line to fill in by hand when team.md still says TODO."""
    return "_" * width if value.strip().upper() == "TODO" else value


# ---------------------------------------------------------------- small building blocks
def P(text, style=body):
    return Paragraph(text, style)


def bullets(items):
    return [Paragraph(i, bullet_style, bulletText="•") for i in items] + [Spacer(1, 4)]


def table(rows, widths, header=True):
    data = [[Paragraph(c, cell_b if header and i == 0 else cell) for c in r] for i, r in enumerate(rows)]
    t = Table(data, colWidths=[w * cm for w in widths], repeatRows=1 if header else 0, hAlign="LEFT")
    style = [("GRID", (0, 0), (-1, -1), 0.5, GRID), ("VALIGN", (0, 0), (-1, -1), "TOP"),
             ("LEFTPADDING", (0, 0), (-1, -1), 4), ("RIGHTPADDING", (0, 0), (-1, -1), 4),
             ("TOPPADDING", (0, 0), (-1, -1), 3), ("BOTTOMPADDING", (0, 0), (-1, -1), 3)]
    if header:
        style.append(("BACKGROUND", (0, 0), (-1, 0), HEAD_BG))
    t.setStyle(TableStyle(style))
    return [t, Spacer(1, 9)]


def code(text):
    t = Table([[Preformatted(text.strip("\n"), code_style)]], colWidths=[FULL_W], hAlign="LEFT")
    t.setStyle(TableStyle([("BOX", (0, 0), (-1, -1), 0.5, GRID), ("LEFTPADDING", (0, 0), (-1, -1), 6),
                           ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
    return [t, Spacer(1, 9)]


FIG_NO = [0]


def fig(name, caption, max_w=15.5, max_h=10.5, crop=None):
    """A screenshot with a thin border and a numbered caption, kept on one page.
    crop=(left, top, right, bottom) keeps only the useful part of a very wide screenshot."""
    path = os.path.join(SHOTS, name)
    if not os.path.exists(path):
        return [P(f"[Screenshot missing: {name}]")]
    src = PILImage.open(path)
    if crop:
        src = src.crop(crop)
    w, h = src.size
    buf = BytesIO()
    src.save(buf, "PNG")
    buf.seek(0)
    scale = min(max_w * cm / w, max_h * cm / h, 0.75)  # never enlarge beyond ~96 dpi
    framed = Table([[Image(buf, width=w * scale, height=h * scale)]])
    framed.setStyle(TableStyle([("BOX", (0, 0), (-1, -1), 0.5, GRID)] +
                               [(k, (0, 0), (-1, -1), 0) for k in
                                ("LEFTPADDING", "RIGHTPADDING", "TOPPADDING", "BOTTOMPADDING")]))
    FIG_NO[0] += 1
    return [KeepTogether([framed, P(f"Figure {FIG_NO[0]}: {caption}", cap)])]


def arrow(d, x1, y1, x2, y2):
    """A line with a small triangular head at (x2, y2)."""
    d.add(Line(x1, y1, x2, y2, strokeWidth=0.8))
    import math
    a = math.atan2(y2 - y1, x2 - x1)
    s = 5
    d.add(Polygon([x2, y2, x2 - s * math.cos(a - 0.4), y2 - s * math.sin(a - 0.4),
                   x2 - s * math.cos(a + 0.4), y2 - s * math.sin(a + 0.4)], fillColor=colors.black, strokeWidth=0))


def box(d, x, y, w, h, lines, bold_first=True, size=8.5):
    d.add(Rect(x, y, w, h, fillColor=colors.white, strokeColor=colors.black, strokeWidth=0.7))
    top = y + h / 2 + (len(lines) - 1) * (size + 1.5) / 2 - size / 3
    for i, line in enumerate(lines):
        d.add(String(x + w / 2, top - i * (size + 1.5), line, textAnchor="middle", fontSize=size,
                     fontName=SERIF_B if (bold_first and i == 0) else SERIF))


def architecture_diagram():
    d = Drawing(FULL_W, 300)
    # clients
    box(d, 0, 236, 78, 30, ["Browser", "(HTML pages)"])
    box(d, 0, 194, 78, 30, ["Postman", "(JSON /api)"])
    # main app
    d.add(Rect(100, 55, 220, 235, fillColor=None, strokeColor=colors.black, strokeWidth=1))
    d.add(String(210, 276, "placetrack-app (Spring Boot, port 8080)", textAnchor="middle", fontSize=9,
                 fontName=SERIF_B))
    layers = [(238, 30, ["LoginInterceptor", "login and role check"]),
              (196, 30, ["Controllers", "MVC pages and REST /api"]),
              (148, 36, ["Services", "business rules, @Transactional", "ApplyAuditAspect wraps apply() (AOP)"]),
              (106, 30, ["Repositories", "Spring Data JPA, HQL"]),
              (64, 30, ["H2 database", "data/placetrack (inside the app)"])]
    for y, h, lines in layers:
        box(d, 112, y, 196, h, lines)
    for (y_top, _, _), (y_next, h_next, _) in zip(layers, layers[1:]):
        arrow(d, 210, y_top, 210, y_next + h_next)
    arrow(d, 78, 251, 112, 253)
    arrow(d, 78, 209, 112, 211)
    # eligibility microservice
    d.add(Rect(345, 130, 108, 100, fillColor=None, strokeColor=colors.black, strokeWidth=1))
    d.add(String(399, 218, "eligibility-service", textAnchor="middle", fontSize=9, fontName=SERIF_B))
    d.add(String(399, 207, "(port 8081, no database)", textAnchor="middle", fontSize=8, fontName=SERIF))
    box(d, 351, 170, 96, 26, ["EligibilityController"], size=8)
    box(d, 351, 136, 96, 26, ["EligibilityService", "3 rules"], size=8)
    arrow(d, 399, 170, 399, 162)
    arrow(d, 308, 166, 345, 166)
    d.add(String(399, 118, "EligibilityClient calls", textAnchor="middle", fontSize=8, fontName=SERIF))
    d.add(String(399, 108, "POST /eligibility/check (JSON)", textAnchor="middle", fontSize=8, fontName=SERIF))
    return d


def er_diagram():
    d = Drawing(FULL_W, 225)
    w, h = 120, 64
    boxes = {
        "users": (0, 150, ["USERS", "id (PK)", "full_name, email (unique)", "password (BCrypt hash)"]),
        "students": (0, 10, ["STUDENTS", "id (PK, FK to users.id)", "reg_no (unique), department", "cgpa, backlogs, placed"]),
        "coordinators": (166, 150, ["COORDINATORS", "id (PK, FK to users.id)", "staff_id (unique)"]),
        "applications": (166, 10, ["APPLICATIONS", "id (PK), status, applied_at", "student_id (FK), drive_id (FK)",
                                   "unique (student_id, drive_id)"]),
        "companies": (333, 150, ["COMPANIES", "id (PK)", "name (unique), sector"]),
        "drives": (333, 10, ["DRIVES", "id (PK), company_id (FK)", "role_title, package_lpa",
                             "min_cgpa, max_backlogs,", "allowed_departments, deadline"]),
    }
    for x, y, lines in boxes.values():
        box(d, x, y, w, h, lines, size=7.8)

    def label(x, y, text):
        d.add(String(x, y, text, fontSize=8, fontName=SERIF_I))

    d.add(Line(60, 150, 60, 74, strokeWidth=0.8)); label(64, 108, "is a (same id)")
    d.add(Line(120, 182, 166, 182, strokeWidth=0.8)); label(129, 186, "is a")
    d.add(Line(120, 42, 166, 42, strokeWidth=0.8)); label(124, 46, "1"); label(158, 46, "N")
    d.add(Line(286, 42, 333, 42, strokeWidth=0.8)); label(290, 46, "N"); label(325, 46, "1")
    d.add(Line(393, 150, 393, 74, strokeWidth=0.8)); label(397, 141, "1"); label(397, 77, "N"); label(397, 108, "runs")
    return d


# ---------------------------------------------------------------- document with a table of contents
class ReportDoc(BaseDocTemplate):
    def __init__(self, filename):
        super().__init__(filename, pagesize=A4, leftMargin=2.3 * cm, rightMargin=2.3 * cm, topMargin=2.2 * cm,
                         bottomMargin=2.3 * cm, title="PlaceTrack Project Report",
                         author="IT23502 Mini Project Team")
        frame = Frame(self.leftMargin, self.bottomMargin, self.width, self.height, id="main")
        self.addPageTemplates([PageTemplate("title", [frame]), PageTemplate("body", [frame], onPage=footer)])

    def afterFlowable(self, flowable):
        # Every h1/h2 heading goes into the table of contents and the PDF bookmarks
        if isinstance(flowable, Paragraph) and flowable.style.name in ("h1", "h2"):
            level = 0 if flowable.style.name == "h1" else 1
            text = flowable.getPlainText()
            key = "h" + re.sub(r"\W+", "", text)
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(text, key, level=level, closed=level > 0)
            self.notify("TOCEntry", (level, text, self.page, key))


def footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(GRID)
    canvas.setLineWidth(0.5)
    canvas.line(doc.leftMargin, 1.75 * cm, A4[0] - doc.rightMargin, 1.75 * cm)
    canvas.setFont(SERIF, 9)
    canvas.setFillColor(colors.HexColor("#444444"))
    canvas.drawString(doc.leftMargin, 1.3 * cm, "PlaceTrack: Campus Placement Drive Tracker")
    canvas.drawRightString(A4[0] - doc.rightMargin, 1.3 * cm, f"Page {doc.page}")
    canvas.restoreState()


# ---------------------------------------------------------------- the report content
def build_story(team_no, github, members, info):
    s = []

    # ---------- title page
    s += [Spacer(1, 2.2 * cm),
          P(f"{info['Course']} ({info['Course code']})",
            ParagraphStyle("t0", parent=centre, fontName=SERIF_B, fontSize=13)),
          P("Mini Project Report (Unit IV and Unit V)", centre),
          Spacer(1, 1.6 * cm),
          P("PlaceTrack", ParagraphStyle("t1", parent=centre, fontName=SERIF_B, fontSize=28, leading=34)),
          P("A Campus Placement Drive Tracker", ParagraphStyle("t2", parent=centre, fontSize=15, leading=20)),
          Spacer(1, 0.4 * cm),
          P("Spring Boot · Spring MVC · JPA / Hibernate · REST · Microservice", centre),
          Spacer(1, 1.8 * cm),
          P(f"<b>Team number:</b> {team_no}", centre) if team_no else Spacer(1, 0),
          Spacer(1, 0.5 * cm)]
    rows = [["Member", "Name", "Register number", "Module"]]
    for role, name, reg, module in members:
        rows.append([role, blank(name, 14), blank(reg, 12), module])
    t = table(rows, [1.6, 4.0, 3.6, 7.0])
    t[0].hAlign = "CENTER"
    s += t
    s += [Spacer(1, 1.2 * cm),
          P(f"<b>Source code:</b> {github}", centre),
          Spacer(1, 1.6 * cm),
          P(f"Department of {info['Department']}", centre),
          P(info["Semester"].capitalize(), centre),
          Spacer(1, 0.3 * cm),
          P("October 2026", centre),
          NextPageTemplate("body"), PageBreak()]

    # ---------- contents
    toc = TableOfContents()
    toc.levelStyles = [toc_1, toc_2]
    toc.dotsMinLevel = 0
    s += [P("Contents", ParagraphStyle("ct", parent=h1)), toc, PageBreak()]

    # ---------- 1 abstract
    s += [P("1. Abstract", h1),
          P("PlaceTrack is a web application that manages campus placement drives. A placement drive is one "
            "company recruiting for one job role at the college. Placement coordinators add companies and create "
            "drives with eligibility criteria: a minimum CGPA, a maximum number of backlogs, the departments that may "
            "apply and an application deadline. Students register, keep their academic profile up to date, browse "
            "open drives, check whether they are eligible and apply online."),
          P("Eligibility is decided by a second, independent Spring Boot application (a microservice) that runs on "
            "its own port. The main application calls it over REST before saving an application, and shows the "
            "student the exact reasons when a rule fails. If the eligibility service is not reachable, the "
            "application is not saved and the student is asked to try later. Coordinators move applicants through "
            "Applied, Shortlisted, Selected and Rejected; selecting a student also marks the student as placed in the "
            "same database transaction. Every application attempt is logged by an aspect (AOP), and REST endpoints "
            "tested with Postman expose drives and applications."),
          P("The project is built with Spring Boot 3.5, Spring MVC with Thymeleaf, Spring Data JPA with Hibernate, an "
            "H2 database, Bean Validation, Spring AOP, JUnit and Mockito, Maven, Git and GitHub.")]

    # ---------- 2 problem statement
    s += [P("2. Problem Statement", h1),
          P("In many colleges, placement drives are announced through messages and spreadsheets. Students have to "
            "work out by themselves whether they meet each company's criteria, coordinators collect registrations by "
            "hand, and it is hard to see who applied where, who was shortlisted and who is already placed. Mistakes "
            "are common: ineligible students register, eligible students miss deadlines, and an already placed "
            "student may keep applying."),
          P("The project builds one simple system in which the criteria are stored with each drive, eligibility is "
            "checked automatically when a student applies, and every application has a clear status that both the "
            "student and the coordinator can see.")]

    # ---------- 3 objectives
    s += [P("3. Objectives", h1)]
    s += bullets([
        "Build a working Spring Boot web application using the MVC pattern, created and run with Spring Tool Suite.",
        "Store data with JPA and Hibernate using annotated entities, an ORM mapping with relationships, an ID "
        "generation strategy, CRUD operations and an inheritance mapping.",
        "Write HQL queries and demonstrate SessionFactory, Session and Transaction.",
        "Use dependency injection throughout and demonstrate BeanFactory and ApplicationContext.",
        "Apply AOP to a practical problem: auditing every application attempt.",
        "Provide REST APIs using GET and POST and test them with Postman.",
        "Build a second, independent service on another port and call it over REST, including the case where it "
        "is down.",
        "Maintain the code with Git and GitHub and build it with Maven.",
    ])

    # ---------- 4 scope
    s += [P("4. Scope and Features", h1), P("4.1 Coordinator features", h2)]
    s += bullets([
        "Log in and see a dashboard with the number of students, placed students, open drives and applications per "
        "drive.",
        "Add, edit and delete companies. Company names must be unique and a company that has drives cannot be "
        "deleted.",
        "Create, edit and delete drives with their criteria. A drive that already has applications cannot be "
        "deleted.",
        "See the applicants of a drive (best CGPA first) and move each one: Applied to Shortlisted or Rejected, "
        "Shortlisted to Selected or Rejected. Selecting a student marks the student as placed.",
    ])
    s += [P("4.2 Student features", h2)]
    s += bullets([
        "Register and log in.",
        "See open drives (deadline today or later) and check eligibility for any drive without applying.",
        "Apply. The application is refused with a clear message if the student is already placed, the deadline has "
        "passed, the student already applied, or an eligibility rule fails.",
        "See their applications with their status, and withdraw while the status is Applied or Shortlisted.",
        "Edit their name, department, CGPA and backlogs.",
    ])
    s += [P("4.3 Kept out of scope", h2),
          P("To keep the project small and fully explainable, the following were deliberately left out: Spring "
            "Security and JWT (a simple session-based login and an interceptor are used instead), file uploads, "
            "e-mail notifications, a JavaScript front end, Docker, and running Spring Cloud components. These are "
            "discussed in the report where relevant.")]

    # ---------- 5 technology
    s += [P("5. Technology Stack", h1)]
    s += table([
        ["Area", "Choice", "Purpose"],
        ["Language", "Java 25 (tested with JDK 26)", "Language level used by both projects"],
        ["Framework", "Spring Boot 3.5.16", "Auto-configuration, embedded Tomcat server"],
        ["Web", "Spring MVC, Thymeleaf, Bootstrap 5", "Server-rendered HTML pages"],
        ["Persistence", "Spring Data JPA, Hibernate 6", "ORM, repositories, HQL"],
        ["Database", "H2 (file mode)", "Embedded SQL database, no installation needed"],
        ["Validation", "Bean Validation (Jakarta)", "Rules such as @NotBlank and @Min on forms and JSON"],
        ["AOP", "Spring AOP (AspectJ annotations)", "Audit log of application attempts"],
        ["Passwords", "BCrypt (spring-security-crypto)", "One-way salted password hashes"],
        ["Service call", "RestTemplate with timeouts", "Main app calls the eligibility microservice"],
        ["Testing", "JUnit 5, Mockito, MockMvc, Postman, Newman", "Unit, web layer and API tests"],
        ["Build and DevOps", "Maven (wrapper), Git, GitHub", "Build, dependency management, version control"],
        ["IDE", "Spring Tool Suite 4 / IntelliJ IDEA", "Development and running the applications"],
    ], [3.2, 5.6, 7.6])

    # ---------- 6 architecture
    s += [PageBreakIfNotEmpty(), P("6. System Architecture", h1),
          P("The system consists of two Spring Boot applications that run as separate processes on separate ports. "
            "They share no Java code; they only agree on a URL and a JSON format."),
          KeepTogether([architecture_diagram(),
                        P(f"Figure {FIG_NO[0] + 1}: Architecture of PlaceTrack", cap)])]
    FIG_NO[0] += 1
    s += bullets([
        "<b>placetrack-app (port 8080)</b> owns all data and pages. Requests pass through a login interceptor, then a "
        "controller, which calls a service, which uses repositories to read and write the H2 database.",
        "<b>eligibility-service (port 8081)</b> is a small stateless service with no database. It receives a "
        "student's marks and a drive's criteria and answers whether the student is eligible, with the reasons.",
        "The main app calls the service through <b>EligibilityClient</b> (RestTemplate, 2 second connect and 3 second "
        "read timeouts). Postman can also call the service directly, which shows it is independent.",
    ])
    s += [P("6.1 Layers of the main application", h2)]
    s += table([
        ["Layer", "Package", "Responsibility"],
        ["View", "templates/", "Thymeleaf HTML pages"],
        ["Controller", "controller/, api/", "Handle requests, call services, return a page or JSON"],
        ["Service", "service/", "Business rules and transactions"],
        ["Repository", "repository/", "Database access with Spring Data JPA and HQL"],
        ["Entity", "model/", "Java classes mapped to tables"],
        ["Supporting", "config/, client/, aspect/, exception/", "Login check, seed data, REST client, AOP, errors"],
    ], [3.0, 5.0, 8.4])
    s += [P("6.2 Monolith and microservices", h2),
          P("A monolith is one application that is built and deployed as a whole. Microservices split the system "
            "into small independent services, each with its own process, that talk over the network. Microservices "
            "can be changed, scaled or restarted separately, but add network calls, timeouts and failure handling."),
          P("PlaceTrack is mostly a monolith with one responsibility, the eligibility decision, extracted into a "
            "microservice. This shows the main ideas (separate process and port, REST communication, independent "
            "deployment, handling a service that is down) while keeping the project small.")]

    # ---------- 7 database
    s += [PageBreakIfNotEmpty(), P("7. Database Design", h1),
          P("The database has six tables. Hibernate creates them from the entity classes "
            "(<i>spring.jpa.hibernate.ddl-auto=update</i>) in an H2 database stored in the <i>data/</i> folder."),
          KeepTogether([er_diagram(), P(f"Figure {FIG_NO[0] + 1}: Tables and relationships", cap)])]
    FIG_NO[0] += 1
    s += table([
        ["Table", "Main columns", "Notes"],
        ["users", "id, full_name, email, password", "Shared part of every account; email unique; password is a BCrypt hash"],
        ["students", "id, reg_no, department, cgpa, backlogs, placed", "One row per student; id is also a foreign key to users.id"],
        ["coordinators", "id, staff_id", "One row per coordinator; id is also a foreign key to users.id"],
        ["companies", "id, name, sector", "name unique"],
        ["drives", "id, company_id, role_title, package_lpa, min_cgpa, max_backlogs, allowed_departments, deadline",
         "company_id is a foreign key; allowed_departments is stored as text such as CSE,IT"],
        ["applications", "id, student_id, drive_id, status, applied_at",
         "Links a student to a drive; unique (student_id, drive_id); status stored as text"],
    ], [2.6, 6.6, 7.2])
    s += [P("7.1 Inheritance mapping", h2),
          P("Students and coordinators are both users. The abstract class <i>User</i> is mapped with "
            "<i>@Inheritance(strategy = InheritanceType.JOINED)</i>: the shared fields are stored in <i>users</i> "
            "and each subclass has its own table containing only its extra fields, linked by the same id. Reading a "
            "student therefore joins two tables. JOINED was chosen over SINGLE_TABLE because it avoids a single wide "
            "table with many empty columns, and over TABLE_PER_CLASS because shared fields are stored once."),
          P("7.2 ID generation strategy", h2),
          P("All primary keys use <i>@GeneratedValue(strategy = GenerationType.IDENTITY)</i>: the database "
            "auto-numbers new rows. In the generated schema this appears as "
            "<i>ID BIGINT GENERATED BY DEFAULT AS IDENTITY</i>."),
          P("7.3 Sample records", h2),
          P("On first start the class <i>DataSeeder</i> fills an empty database with demo data (every password is "
            "Pass@123). The full schema and data are exported in <i>docs/report/schema.sql</i> and "
            "<i>docs/report/schema-and-sample-data.sql</i>.")]
    s += table([
        ["Email", "Name", "Dept", "CGPA", "Backlogs", "Placed"],
        ["student1@placetrack.edu", "Aarav Kumar", "IT", "8.6", "0", "No"],
        ["student2@placetrack.edu", "Diya Menon", "IT", "7.2", "1", "No"],
        ["student3@placetrack.edu", "Karthik Raman", "CSE", "8.0", "0", "Yes"],
        ["student4@placetrack.edu", "Sneha Iyer", "MECH", "7.9", "0", "No"],
        ["student5@placetrack.edu", "Rohan Das", "ECE", "9.1", "0", "No"],
        ["student6@placetrack.edu", "Priya Natarajan", "CSE", "6.4", "2", "No"],
    ], [5.0, 3.6, 1.6, 1.6, 2.0, 1.6])
    s += table([
        ["Company", "Role", "LPA", "Min CGPA", "Max backlogs", "Departments", "Deadline"],
        ["Zephyr Systems", "Software Engineer Trainee", "4.5", "6.0", "2", "CSE, IT, ECE", "today + 10"],
        ["Nimbus Cloud", "Cloud Engineer", "9.0", "7.5", "0", "IT, CSE", "today + 7"],
        ["Quanta Analytics", "Data Scientist", "14.0", "8.5", "0", "CSE, IT, ECE", "today + 14"],
        ["Zephyr Systems", "Support Engineer", "3.8", "6.0", "3", "all", "today - 3 (closed)"],
        ["Orion Manufacturing", "Graduate Engineer Trainee", "4.0", "6.5", "1", "MECH, ECE, EEE, CIVIL", "today + 9"],
    ], [3.0, 3.5, 1.1, 1.5, 1.8, 3.0, 2.5])
    s += [P("Two coordinators (coord1 and coord2), five companies (Helio Power has no drives) and four sample "
            "applications in different states are also created.")]

    # ---------- 8 implementation
    s += [PageBreakIfNotEmpty(), P("8. Implementation", h1),
          P("This chapter explains how each required concept is implemented. Class names refer to the source code in "
            "the GitHub repository.")]

    s += [P("8.1 Entities and ORM mapping", h2),
          P("Each entity is a class annotated with <i>@Entity</i> and <i>@Table</i>. Fields become columns; "
            "<i>@Column(nullable = false, unique = true)</i> adds database rules; validation annotations such as "
            "<i>@NotBlank</i>, <i>@Min</i> and <i>@DecimalMax</i> are checked when forms or JSON are submitted."),
          P("Relationships use <i>@ManyToOne</i>: many drives belong to one company (column <i>company_id</i>), and "
            "an application belongs to one student and one drive. The application status is an enum stored as text "
            "with <i>@Enumerated(EnumType.STRING)</i>, and <i>@JsonIgnore</i> keeps password hashes out of JSON.")]
    s += code("""
@Entity
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "drive_id"}))
public class Application {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Student student;   // column student_id
    @ManyToOne(optional = false) private Drive drive;       // column drive_id
    @Enumerated(EnumType.STRING) private ApplicationStatus status;
    private LocalDateTime appliedAt;
}""")

    s += [P("8.2 Repositories and HQL", h2),
          P("Repositories are interfaces that extend <i>JpaRepository</i>, which provides save, findById, findAll, "
            "deleteById and count (CRUD) without any code. Extra queries are either derived from the method name "
            "(for example <i>existsByEmail</i>) or written in HQL with <i>@Query</i>. HQL uses class and field names, "
            "and Hibernate translates it to SQL.")]
    s += table([
        ["Repository method", "Kind", "Purpose"],
        ["DriveRepository.<br/>findOpenDrives(today)", "HQL", "Drives whose deadline is today or later"],
        ["ApplicationRepository.<br/>findByStudent(id)", "HQL", "A student's applications, newest first"],
        ["ApplicationRepository.<br/>findApplicantsOfDrive(id)", "HQL, join fetch",
         "Applicants with their student loaded in one query, best CGPA first"],
        ["ApplicationRepository.<br/>countApplicationsPerDrive()", "HQL, GROUP BY, left join",
         "Rows of company, role and count for the dashboard; drives with no applications are kept"],
        ["UserRepository.<br/>findByEmail(email)", "Derived", "Login"],
        ["ApplicationRepository.<br/>existsByStudentIdAndDriveId", "Derived", "Duplicate application check"],
        ["CompanyRepository.findByNameIgnoreCase,<br/>DriveRepository.countByCompanyId", "Derived",
         "Unique company name; company with drives cannot be deleted"],
    ], [7.0, 3.4, 6.0])
    s += code("""
@Query("select d from Drive d where d.deadline >= :today order by d.deadline")
List<Drive> findOpenDrives(@Param("today") LocalDate today);

@Query("select a from Application a join fetch a.student s "
     + "where a.drive.id = :driveId order by s.cgpa desc")
List<Application> findApplicantsOfDrive(@Param("driveId") Long driveId);""")

    s += [P("8.3 SessionFactory, Session and Transaction", h2),
          P("The <b>SessionFactory</b> is created once from the entity mappings and database settings; it is heavy, "
            "thread-safe and creates Sessions. A <b>Session</b> is one unit of work with the database: it wraps a "
            "connection and keeps a first-level cache of loaded objects. A <b>Transaction</b> groups changes so that "
            "they are either all saved (commit) or all undone (rollback). In normal code Spring manages these through "
            "repositories and <i>@Transactional</i>."),
          P("The class <i>HibernateSessionDemo</i> (run with the <i>demo</i> profile) uses them directly: it obtains "
            "the SessionFactory from JPA, opens a Session, begins a Transaction, runs an HQL query, inserts a "
            "company, rolls back and shows that the insert was undone. Its console output:")]
    s += code("""
===== Hibernate demo: SessionFactory -> Session -> Transaction =====
1. HQL GROUP BY, students per department:
     CSE = 2
     ECE = 1
     IT = 2
     MECH = 1
2. Companies before insert: 5
3. Companies after insert (inside the transaction): 6
4. tx.rollback() called
5. Companies seen by a NEW session: 5  -> the insert was undone""")

    s += [P("8.4 Services and business rules", h2),
          P("Services hold the rules; controllers never use repositories directly. The most important method is "
            "<i>ApplicationService.apply</i>, which runs inside one transaction and checks the rules in this order "
            "(cheap database checks first, the network call last):")]
    s += bullets([
        "The student must not already be placed.",
        "The drive's deadline must not have passed.",
        "The student must not have applied to this drive already (also enforced by a unique constraint).",
        "The eligibility service must accept the student: CGPA at least the minimum, backlogs at most the limit, "
        "department in the allowed list.",
        "If every rule passes, the application is saved with status APPLIED. If any rule fails, an exception is "
        "thrown, the transaction rolls back and nothing is saved.",
    ])
    s += table([
        ["Rule", "Where it is enforced"],
        ["Company names are unique; a company with drives cannot be deleted", "CompanyService"],
        ["A drive with applications cannot be deleted", "DriveService"],
        ["Eligibility: CGPA, backlogs, department", "eligibility-service (EligibilityService)"],
        ["Deadline, one application per drive, placed students cannot apply", "ApplicationService.apply"],
        ["Withdraw only own application, only while Applied or Shortlisted", "ApplicationService.withdraw"],
        ["Allowed status moves; Selected also marks the student as placed", "ApplicationService.updateStatus"],
    ], [10.4, 6.0])

    s += [P("8.5 MVC controllers and pages", h2),
          P("Controllers (<i>@Controller</i>) handle a URL, put data into the <i>Model</i> and return a template "
            "name; Thymeleaf fills the template and returns HTML. Forms use the Post-Redirect-Get pattern: after a "
            "successful POST the browser is redirected, and a one-time flash message is shown, so refreshing the page "
            "does not submit the form again.")]
    s += table([
        ["Controller", "Main URLs", "Used by"],
        ["AuthController", "/, /login, /logout, /register", "Everyone"],
        ["DashboardController", "/coordinator/dashboard", "Coordinator"],
        ["CompanyController", "/coordinator/companies (list, add, edit, delete)", "Coordinator"],
        ["DriveController", "/coordinator/drives (list, create, edit, delete)", "Coordinator"],
        ["ApplicantController", "/coordinator/drives/{id}/applicants, /coordinator/applications/{id}/status", "Coordinator"],
        ["StudentController", "/student/drives, /student/drives/{id}/check, /apply, /student/applications", "Student"],
        ["ProfileController", "/student/profile", "Student"],
    ], [4.0, 9.4, 3.0])

    s += [P("8.6 Login, roles and input safety", h2)]
    s += bullets([
        "Passwords are stored as BCrypt hashes. At login, <i>AuthService</i> compares the typed password with the "
        "hash and stores a small <i>SessionUser</i> (id, name, role) in the HTTP session.",
        "The session id is changed at login (<i>request.changeSessionId()</i>) to prevent session fixation.",
        "<i>LoginInterceptor</i> runs before every /student/** and /coordinator/** page: a visitor who is not logged "
        "in is sent to the login page and a user with the wrong role gets 403 Forbidden.",
        "Forms bound to entities use <i>@InitBinder</i> with an allow-list of fields, so extra fields such as "
        "<i>placed=true</i> or another <i>id</i> added to a request are ignored.",
    ])

    s += [P("8.7 Dependency injection, BeanFactory and ApplicationContext", h2),
          P("Every service and controller receives the objects it needs through its constructor; Spring creates them "
            "(beans) and passes them in. No class creates its collaborators with <i>new</i>, which keeps classes "
            "loosely coupled and lets tests pass in mock objects instead."),
          P("The class <i>BeanFactoryDemo</i> shows the two containers. A <b>BeanFactory</b> is the basic container "
            "and creates a bean only when it is requested. An <b>ApplicationContext</b> extends it, creates singleton "
            "beans at startup and adds annotation processing, AOP, events and profiles; Spring Boot creates one for "
            "the application. Output of the demo:")]
    s += code("""
===== Part 1: BeanFactory (lazy) =====
1. Bean definition registered. Nothing created yet.
2. Calling factory.getBean("greeter")...
      -> Greeter constructor runs (bean created)
3. Hello from a Spring bean

===== Part 2: ApplicationContext (eager + dependency injection) =====
1. Beans registered. Calling context.refresh()...
      -> Greeter constructor runs (bean created)
2. refresh() finished: the Greeter was already created above, before any getBean call.
3. Hello from a Spring bean, injected into WelcomeService""")

    s += [P("8.8 Aspect-oriented programming (AOP)", h2),
          P("Auditing every application attempt is a cross-cutting concern: it is not part of the business logic. "
            "The aspect <i>ApplyAuditAspect</i> uses an <i>@Around</i> advice with the pointcut "
            "<i>execution(* com.placetrack.service.ApplicationService.apply(..))</i>. Spring wraps the service in a "
            "proxy, so every call to apply is timed and logged with the student, the drive and the outcome, while "
            "the service itself contains no logging code.")]
    s += code("""
@Around("execution(* com.placetrack.service.ApplicationService.apply(..))")
public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
    Object[] args = joinPoint.getArgs();   // apply(studentId, driveId)
    long start = System.currentTimeMillis();
    try {
        Object result = joinPoint.proceed();    // run the real apply()
        log.info("AUDIT apply student={} drive={} outcome=SUCCESS time={}ms", ...);
        return result;
    } catch (RuntimeException e) {
        log.info("AUDIT apply student={} drive={} outcome=BLOCKED reason=\\"{}\\" ...", ...);
        throw e;
    }
}""")

    s += [P("8.9 Transactions", h2),
          P("<i>@Transactional</i> on a service method makes Spring begin a transaction before the method and commit "
            "it afterwards, or roll it back if a runtime exception is thrown. When a coordinator selects a student, "
            "<i>updateStatus</i> changes two rows in two tables, the application's status and the student's placed "
            "flag, and both are committed together. Hibernate's dirty checking writes the changes at commit time "
            "without an explicit save call.")]

    s += [P("8.10 REST API", h2),
          P("<i>ApiController</i> is a <i>@RestController</i>: it returns JSON instead of HTML and reuses the same "
            "services, so the same rules, transaction and audit log apply. <i>ApiErrorHandler</i> converts exceptions "
            "into JSON errors with suitable status codes.")]
    s += table([
        ["Method and path", "Body", "Success", "Errors"],
        ["GET /api/drives", "–", "200, list of open drives", "–"],
        ["GET /api/drives/{id}", "–", "200, one drive", "404"],
        ["POST /api/companies", "{name, sector}", "201, the new company", "400"],
        ["POST /api/applications", "{studentId, driveId}", "201, the new application",
         "400 with reasons, 404, 503 if the service is down"],
        ["GET /api/students/{id}/applications", "–", "200, list", "–"],
    ], [5.4, 3.4, 3.8, 3.8])
    s += [P("For simple testing the API has no login and takes the student id from the request body. A real system "
            "would identify the student from the session or a token such as a JWT.")]

    s += [P("8.11 Eligibility microservice and the service call", h2),
          P("The eligibility service has one controller and one service. <i>POST /eligibility/check</i> receives the "
            "student's marks and the drive's criteria as JSON, validates them with <i>@Valid</i> (a missing field "
            "gives 400 Bad Request) and returns the decision with one reason per failed rule. "
            "<i>GET /eligibility/ping</i> returns <i>{\"status\":\"UP\"}</i>.")]
    s += code("""
Request:  { "cgpa": 7.2, "backlogs": 1, "department": "IT",
            "minCgpa": 7.5, "maxBacklogs": 0, "allowedDepartments": ["IT", "CSE"] }
Response: { "eligible": false,
            "reasons": [ "CGPA 7.2 is below the required 7.5",
                         "Backlogs 1 exceed the limit of 0" ] }""")
    s += [P("The main application's <i>EligibilityClient</i> sends this request with RestTemplate using 2 second "
            "connect and 3 second read timeouts. A refused connection, a timeout or an error reply becomes an "
            "<i>EligibilityUnavailableException</i>; the student sees \"Eligibility check unavailable, try later\" "
            "(the API returns 503) and nothing is saved.")]

    # ---------- 9 web services
    s += [PageBreakIfNotEmpty(), P("9. Web Services, REST and Spring Cloud", h1),
          P("A web service is software that offers functions to other programs over a network using standard "
            "protocols and formats, independent of the programming language. Both the eligibility service and the "
            "/api endpoints of the main application are web services."),
          P("9.1 REST and SOAP", h2)]
    s += table([
        ["", "REST", "SOAP"],
        ["Nature", "Architectural style using plain HTTP", "Protocol with strict rules"],
        ["Message format", "Usually JSON (also XML or text)", "Always XML in an envelope"],
        ["Contract", "None required (often OpenAPI documentation)", "WSDL file describes each operation"],
        ["Operations", "HTTP methods on resources (GET /api/drives)", "Named operations, usually all sent with POST"],
        ["Weight", "Light and fast, easy to cache", "Heavier, more parsing"],
        ["Typical use", "Web and mobile apps, microservices", "Banking, government, legacy enterprise systems"],
    ], [3.0, 6.7, 6.7])
    s += [P("PlaceTrack uses REST: resources at URLs, GET to read and POST to create, standard status codes "
            "(200, 201, 400, 404, 503) and JSON bodies. GET is idempotent (repeating it changes nothing); POST is not."),
          P("9.2 Spring Cloud", h2),
          P("Spring Cloud is a set of projects that solve common problems of systems made of many services. None is "
            "used in this project, but each one corresponds to something done by hand here:")]
    s += table([
        ["Feature", "What it does", "Relation to PlaceTrack"],
        ["OpenFeign", "Declarative REST client: an annotated interface instead of hand-written HTTP calls",
         "Would replace EligibilityClient"],
        ["Resilience4j", "Circuit breaker: stops calling a failing service for a while and uses a fallback",
         "Would replace the manual timeout and \"try later\" handling"],
        ["Eureka", "Service discovery: services register by name", "Would replace the fixed eligibility.url"],
        ["Spring Cloud Gateway", "One entry point that routes requests to services", "Single address for both apps"],
        ["Config Server", "Central configuration for all services", "Would replace the separate property files"],
    ], [3.4, 7.0, 6.0])

    # ---------- 10 testing
    s += [PageBreakIfNotEmpty(), P("10. Testing", h1),
          P("The project was tested at three levels: automated unit and web-layer tests run by Maven, API tests with "
            "Postman and Newman, and manual tests of the web application. Screenshots of the results are in chapter 13.")]
    s += [P("10.1 Automated tests (JUnit 5, Mockito, MockMvc)", h2)]
    s += table([
        ["Test class", "Type", "Tests", "What is checked"],
        ["EligibilityServiceTest", "Plain JUnit", "5", "The three rules, including the equal-value boundary"],
        ["EligibilityControllerTest", "@WebMvcTest", "2", "200 for valid JSON, 400 for a missing field"],
        ["RepositoryTest", "@DataJpaTest", "4", "The HQL queries on a temporary database"],
        ["PlacetrackAppTests", "@SpringBootTest", "2", "The application starts; seed data; inheritance"],
        ["ApplicationServiceTest", "Mockito", "8", "Apply and withdraw rules; nothing saved when a rule fails"],
        ["StatusUpdateTest", "Mockito", "4", "Allowed status moves; Selected marks the student as placed"],
        ["ApiControllerTest", "@WebMvcTest, @MockitoBean", "6", "API status codes and JSON, no password hash in output"],
        ["Total", "", "31", "All passing (mvn clean verify: BUILD SUCCESS)"],
    ], [4.4, 3.6, 1.3, 7.1])
    s += [P("10.2 API tests (Postman and Newman)", h2),
          P("A Postman collection with an environment (<i>baseUrl</i>, <i>eligUrl</i>) is stored in the repository. "
            "Every request has test scripts that check the status code and important fields. The collection was run "
            "in the Postman application and from the command line with Newman, Postman's collection runner.")]
    s += table([
        ["Folder", "Requests", "Examples"],
        ["00 Eligibility service", "5", "Ping; eligible; low CGPA and backlogs; wrong department; missing field gives 400"],
        ["01 Drives and companies", "5", "List open drives; one drive; missing drive gives 404; create company 201; no name 400"],
        ["02 Applications", "7", "Apply 201; apply again 400; ineligible 400 with reasons; placed 400; expired 400; "
                                 "missing field 400; list applications 200"],
        ["03 Service down", "1", "Apply with the eligibility service stopped gives 503"],
    ], [4.0, 1.8, 10.6])
    s += [P("Result: folders 00 to 02 ran 17 requests with 36 assertions and 0 failures; folder 03 returned 503 with "
            "both assertions passing.")]
    s += [P("10.3 Manual test cases", h2)]
    s += table([
        ["No.", "Test", "Expected result", "Result"],
        ["T1", "Open a student page without logging in", "Redirected to the login page", "Pass"],
        ["T2", "Log in with a wrong password", "\"Wrong email or password\"", "Pass"],
        ["T3", "Coordinator logs in", "Dashboard: 6 students, 1 placed, 4 open drives", "Pass"],
        ["T4", "Delete a company that has drives", "Refused with a message", "Pass"],
        ["T5", "Delete a drive that has applications", "Refused with a message", "Pass"],
        ["T6", "Create a new drive", "\"Drive saved\", drive listed as Open", "Pass"],
        ["T7", "Select a shortlisted applicant", "Status SELECTED; student marked placed (dashboard Placed 2)", "Pass"],
        ["T8", "student2 checks eligibility for Nimbus Cloud", "Two reasons: CGPA and backlogs", "Pass"],
        ["T9", "student2 applies to Nimbus Cloud", "Refused with the reasons, nothing saved", "Pass"],
        ["T10", "student1 applies to Nimbus Cloud", "\"Applied successfully\", status APPLIED", "Pass"],
        ["T11", "student1 applies to the same drive again", "\"You have already applied\"", "Pass"],
        ["T12", "student3 (placed) applies", "\"You are already placed\"", "Pass"],
        ["T13", "Apply to the closed drive by URL", "\"The deadline for this drive has passed\"", "Pass"],
        ["T14", "Student opens a coordinator page", "403 Forbidden", "Pass"],
        ["T15", "Apply while the eligibility service is stopped", "\"Eligibility check unavailable, try later\"", "Pass"],
        ["T16", "Apply again after restarting the service", "Saved as APPLIED", "Pass"],
        ["T17", "Withdraw an application", "Status WITHDRAWN", "Pass"],
        ["T18", "Register with an existing e-mail / a 3-character password", "Refused with a message", "Pass"],
        ["T19", "Profile: CGPA 11", "Validation error, nothing changed", "Pass"],
    ], [1.2, 6.2, 7.4, 1.6])

    # ---------- 11 devops
    s += [PageBreakIfNotEmpty(), P("11. DevOps: Git, GitHub and Maven", h1),
          P("DevOps is a way of working in which development and operations cooperate and automate building, "
            "testing and releasing software, so that changes reach users quickly and reliably. Its main practices "
            "are version control, build automation, automated testing, continuous integration (every change is built "
            "and tested automatically) and continuous delivery (tested changes are released automatically).")]
    s += table([
        ["Tool", "Purpose", "Used in this project"],
        ["Git", "Version control: history of every change, branches", "Yes"],
        ["GitHub", "Shared repository, collaborators, pull requests", f"Yes ({github})"],
        ["Maven with wrapper", "Build, dependencies, tests, packaging", "Yes (./mvnw clean verify)"],
        ["JUnit, Mockito, Postman, Newman", "Automated testing", "Yes"],
        ["GitHub Actions / Jenkins", "Continuous integration pipelines", "Not used (Newman and Maven can run there)"],
        ["Docker", "Package an application with its runtime", "Not used"],
    ], [4.6, 6.8, 5.0])
    s += [P("The code is organised in one Git repository with two Maven projects. Work was done on separate branches "
            "for each member and merged into the main branch. Each project contains the Maven wrapper (mvnw), so "
            "Maven does not need to be installed. The Maven lifecycle used is <i>clean → compile → test → package → "
            "verify</i>: <i>./mvnw clean verify</i> compiles the code, runs all tests and builds an executable jar.")]

    # ---------- 12 how to run
    s += [P("12. How to Run the Application Locally", h1),
          P("Requirements: JDK 25 or newer and Git. Maven and the database do not need to be installed. Full "
            "instructions are in README.md in the repository.")]
    s += code(f"""
git clone {github}.git
cd placetrack2

# terminal 1: eligibility service (port 8081)
cd eligibility-service && ./mvnw spring-boot:run

# terminal 2: main application (port 8080)
cd placetrack-app && ./mvnw spring-boot:run

# open http://localhost:8080   (demo login: coord1@placetrack.edu / Pass@123)
# H2 console: http://localhost:8080/h2-console
#             JDBC URL jdbc:h2:file:./data/placetrack, user sa, empty password
# tests:      ./mvnw test   (in each project folder)""")
    s += [P("On Windows, use <i>mvnw.cmd</i> instead of <i>./mvnw</i>. In Spring Tool Suite, import both folders with "
            "File → Import → Existing Maven Projects and run each with Run As → Spring Boot App.")]

    # ---------- 13 screenshots
    s += [PageBreakIfNotEmpty(), P("13. Screenshots", h1),
          P("The screenshots below were taken while running both applications locally on fresh demo data."),
          P("13.1 Coordinator", h2)]
    s += fig("app-01-login.png", "Login page with the demo accounts")
    s += fig("app-02-dashboard.png", "Coordinator dashboard: students, placed students, open drives and "
                                     "applications per drive (HQL GROUP BY query)")
    s += fig("app-03b-company-delete-refused.png", "Deleting a company that has drives is refused")
    s += fig("app-04b-drive-delete-refused.png", "Deleting a drive that already has applications is refused")
    s += fig("app-05-drive-saved.png", "A new drive created by the coordinator appears in the list as Open")
    s += fig("app-06-applicants-after-select.png", "Applicants of a drive after selecting Rohan Das")
    s += fig("app-07-dashboard-placed-2.png", "Dashboard after the selection: the number of placed students "
                                              "increased to 2", max_h=11)
    s += [CondPageBreak(8 * cm), P("13.2 Student", h2)]
    s += fig("app-08-check-not-eligible.png", "Eligibility check for a student who does not meet the criteria: "
                                              "one reason per failed rule")
    s += fig("app-09-apply-refused.png", "Applying anyway is refused with the same reasons; nothing is saved")
    s += fig("app-10-applied-success.png", "A successful application, shown in My applications with status APPLIED")
    s += fig("app-11-forbidden-403.png", "A student opening a coordinator page gets 403 Forbidden")
    s += fig("app-12-profile.png", "Student profile: e-mail, register number and placement status cannot be "
                                   "edited here")
    s += [CondPageBreak(8 * cm), P("13.3 Microservice failure handling and AOP audit log", h2)]
    s += fig("app-14a-eligibility-service-stopped.png", "The eligibility service is stopped (Ctrl + C)", max_h=4)
    s += fig("app-14b-service-down-message.png", "Applying while the service is down: the student is asked to try "
                                                 "later and nothing is saved")
    s += fig("app-14d-eligibility-service-restarted.png", "The eligibility service started again on port 8081")
    s += code("""
Tomcat initialized with port 8081 (http)
Tomcat started on port 8081 (http) with context path '/'
Started EligibilityServiceApplication in 2.121 seconds""")
    s += fig("app-14c-applied-after-restart.png", "After the restart the same application succeeds")
    s += fig("app-13-audit-log.png", "Audit lines written by ApplyAuditAspect: the attempt blocked while the service "
                                     "was down and the successful attempt after the restart (the INSERT appears "
                                     "only for the successful one)")
    s += [P("The two AUDIT lines from the screenshot above, as text:")]
    s += code("""
12:03:05 INFO [nio-8080-exec-9] com.placetrack.aspect.ApplyAuditAspect :
    AUDIT apply student=3 drive=3 outcome=BLOCKED
          reason="Eligibility check unavailable, try later" time=18ms
Hibernate: insert into applications (applied_at,drive_id,status,student_id,id) ...
12:03:38 INFO [nio-8080-exec-2] com.placetrack.aspect.ApplyAuditAspect :
    AUDIT apply student=3 drive=3 outcome=SUCCESS time=458ms""")
    s += [CondPageBreak(8 * cm), P("13.4 REST API testing", h2)]
    s += fig("test-04-ping.png", "GET /eligibility/ping in the browser", max_h=3)
    s += fig("test-postman-import.png", "Importing the PlaceTrack collection and environment into Postman",
               crop=(255, 190, 1235, 826))
    s += fig("test-07-postman-get-ping.png", "Postman: GET request to the eligibility service (200 OK)")
    s += fig("test-09-postman-post-check.png", "Postman: POST /eligibility/check returns the decision with the "
                                               "reasons (200 OK)")
    s += fig("test-10a-newman.png", "Newman run, folder 00: eligibility service requests and their test results",
               crop=(0, 0, 950, 706))
    s += fig("test-10b-newman.png", "Newman run, folder 01: drives and companies (200, 404, 201, 400)",
               crop=(0, 0, 720, 746))
    s += fig("test-10c-newman.png", "Newman run, folder 02: refused applications return 400 with the reason",
               crop=(0, 0, 720, 613))
    s += fig("test-10d-newman-summary.png", "Newman summary: 17 requests and 36 assertions, 0 failed", max_h=7)
    s += fig("test-11-newman-503.png", "Service-down test: POST /api/applications returns 503 while the "
                                       "eligibility service is stopped",
               crop=(0, 0, 950, 687))
    s += [CondPageBreak(8 * cm), P("13.5 Automated tests", h2)]
    s += fig("test-01b-maven-eligibility-success.png", "eligibility-service: 7 tests passed, BUILD SUCCESS")
    s += fig("test-02-maven-main-success.png", "placetrack-app: 24 tests passed, BUILD SUCCESS",
               crop=(0, 0, 950, 343))
    s += [CondPageBreak(8 * cm), P("13.6 Database (H2 console)", h2)]
    s += fig("test-24a-h2-login.png", "H2 console login with the JDBC URL of the project database")
    s += fig("test-24b-h2-applications-table.png", "The six tables and the contents of APPLICATIONS")
    s += fig("test-24c-h2-companies-table.png", "Contents of COMPANIES, including the company added during testing")
    s += fig("test-25-h2-users-join-students.png", "JOINED inheritance: each student is a row in USERS joined with "
                                                   "a row in STUDENTS by the same id; PLACED is TRUE after selection")

    # ---------- 14 contributions
    s += [PageBreakIfNotEmpty(), P("14. Individual Contributions", h1),
          P("The work was divided into three modules so that each member owns one part of the application and one "
            "part of the syllabus.")]
    rows = [["Member", "Name", "Contribution"]]
    for role, name, _reg, module in members:
        rows.append([role, blank(name, 14), module])
    s += table(rows, [1.8, 4.4, 10.2])
    s += table([
        ["Member", "Concepts explained in the viva"],
        ["M1", "Entities, ORM mapping, ID strategy, CRUD, JOINED inheritance, HQL, SessionFactory, Session and "
               "Transaction, the eligibility microservice, monolith and microservices"],
        ["M2", "MVC request flow, login and roles, dependency injection, BeanFactory and ApplicationContext, AOP, "
               "@Transactional, the service call and its failure handling"],
        ["M3", "REST APIs, GET and POST, Postman and Newman, web services, REST and SOAP, Spring Cloud, Git, GitHub, "
               "Maven and DevOps"],
    ], [1.8, 14.6])

    # ---------- 15 conclusion
    s += [P("15. Conclusion", h1),
          P("PlaceTrack meets its objectives. Coordinators manage companies, drives and applicants, and students apply "
            "only when they meet the criteria, with clear reasons when they do not. The project demonstrates every "
            "concept required for Units IV and V: a Spring Boot MVC application with JPA and Hibernate, ORM mapping "
            "with relationships, an ID strategy, inheritance, HQL, SessionFactory, Session and Transaction, "
            "dependency injection, AOP, REST APIs tested with Postman, a second service called over REST, and Git, "
            "GitHub and Maven."),
          P("Possible improvements are proper authentication for the REST API (Spring Security with JWT), a circuit "
            "breaker and service discovery with Spring Cloud, coordinator verification of student marks, e-mail "
            "notifications, and a continuous integration pipeline that runs the Maven and Newman tests on every "
            "push."),
          P("References", h2)]
    s += bullets([
        "Spring Boot Reference Documentation, https://docs.spring.io/spring-boot/",
        "Spring Framework Reference (Core, Web MVC, AOP), https://docs.spring.io/spring-framework/reference/",
        "Spring Data JPA Reference, https://docs.spring.io/spring-data/jpa/reference/",
        "Hibernate ORM User Guide, https://hibernate.org/orm/documentation/",
        "Thymeleaf documentation, https://www.thymeleaf.org/documentation.html",
        "H2 Database Engine, https://www.h2database.com/",
        "Postman Learning Center and Newman, https://learning.postman.com/",
    ])
    return s


def main():
    team_no, github, members, info = read_team()
    prefix = re.sub(r"\W+", "", team_no) + "_" if team_no else ""
    out = os.path.join(HERE, f"{prefix}PlaceTrack_Report.pdf")
    doc = ReportDoc(out)
    doc.multiBuild(build_story(team_no, github, members, info))  # two passes so the contents page gets page numbers
    print("Written:", out)


if __name__ == "__main__":
    main()
