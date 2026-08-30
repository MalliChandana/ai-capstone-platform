# AI Capstone — AI-Powered Capstone Project Recommendation & Management Platform

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-HTML5%2FCSS3-green.svg)](https://www.thymeleaf.org/)
[![Security](https://img.shields.io/badge/Security-BCrypt%20Hashing-blueviolet.svg)](https://spring.io/)

A modern, full-stack, enterprise SaaS web platform connecting university students with real-world industry capstone projects through **dynamic AI matching, skill-gap analysis, milestone progress tracking, report evaluation, audit logging, and role-based administration**.

---

## 🚀 Key Features

### 🎓 1. Student Platform
* **AI Recommendations**: Multi-dimensional matching algorithm calculating compatibility scores, matching competencies, skill gaps, and custom learning roadmaps.
* **Skill Gap Analysis**: Direct visual comparison of declared student skills against required project technologies.
* **Project Discovery**: Marketplace search by keyword, technology, domain, and difficulty with duplicate application prevention.
* **Milestone Progress Tracking**: Visual progress bars across 7 standardized milestones (Planning → Design → Implementation → Testing → Final Report).
* **Project Reports & Evaluation**: Multi-section report submission (Abstract, Objectives, Methodology, Results, Conclusion, Future Scope) with company grading and instant PDF generation.

### 🏢 2. Organization / Company Platform
* **Project Lifecycle Management**: Post, edit, and moderate capstone offerings with unlimited detailed descriptions (`TEXT` columns) and prerequisite skill lists.
* **Applicant Review & Evaluation**: Accept or reject student applications with automated instant notifications.
* **Milestone Progress Monitoring**: Review active student milestone submissions, percentage completions, and update notes.
* **Comprehensive Report Grading**: Review submitted student final reports, provide scores/grades, and leave feedback.
* **Verified PDF Export**: Download standardized PDF documentation powered by OpenPDF.

### 🛡️ 3. Administrator Governance Portal
* **System Dashboard**: Real-time KPI metrics (Active Students, Partner Companies, Open Projects, Active Applications, Evaluated Reports).
* **Student & Company Management**: Searchable directories with one-click status activation/deactivation and governance deletion.
* **Project Moderation**: Moderate project visibility (OPEN/CLOSED/UNDER_REVIEW) and remove inappropriate submissions.
* **Global Milestone Tracker**: Cross-institutional view of all active student applications and completion percentages.
* **Security & Compliance Audit Trail**: Immutable logging of all platform actions (Login, Logout, Registration, Moderation, Project Events, Submissions).

---

## 🛠️ Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Backend Framework** | Spring Boot, Spring MVC, Spring Data JPA / Hibernate |
| **Language & Runtime** | Java 17 (Eclipse Adoptium OpenJDK 17) |
| **Database** | MySQL 8.0+ (Relational with InnoDB) |
| **Frontend / Templating** | Thymeleaf, HTML5, Modern SaaS CSS3, Vanilla JavaScript |
| **Security & Cryptography**| BCrypt Password Hashing (`spring-security-crypto`), Role-Based Session Governance |
| **Document Generation** | OpenPDF (Standardized Capstone PDF Exports) |
| **Build & Dependency Tool**| Maven (Wrapper included) |

---

## ⚙️ Environment Configuration

The application uses flexible environment variable overrides with sensible local development defaults:

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `PORT` | HTTP Server Port | `8080` |
| `DB_URL` | MySQL JDBC Connection URL | `jdbc:mysql://localhost:3306/aicapstone?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USERNAME` | Database Username | `root` |
| `DB_PASSWORD` | Database Password | `chandu@123` |
| `APP_ADMIN_EMAIL` | Administrator Login Email | `admin@aicapstone.io` |
| `APP_ADMIN_PASSWORD` | Administrator Password | `AdminPass123!` |
| `APP_ADMIN_NAME` | Administrator Display Name | `Platform Administrator` |
| `APP_DEMO_DATA_ENABLED`| Auto-seed Development Sample Data | `false` (Production) / `true` (Dev) |

---

## 💻 Local Development Setup

### 1. Prerequisites
* **Java 17 JDK** installed and configured in `JAVA_HOME`.
* **MySQL Server 8.0+** running locally on port `3306`.

### 2. Database Initialization
Create the database in MySQL:
```sql
CREATE DATABASE IF NOT EXISTS aicapstone;
```

### 3. Build & Compile
Using the included Maven wrapper:
```bash
# Windows PowerShell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean compile

# macOS / Linux
./mvnw clean compile
```

### 4. Run the Application
```bash
# Windows PowerShell
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

The application will start on: **`http://localhost:8080`**

---

## 🧪 Testing & Verification

Run automated test suites to verify end-to-end user journeys, password hashing, IDOR authorization security, and long description persistence:

```bash
# Run SaaS Platform E2E Verification
python C:\Users\malli\.gemini\antigravity\brain\e020d83a-102b-4605-a42b-93021bb61217\scratch\test_saas_platform.py

# Run Navigation Verification across all 27+ Portal Views
python C:\Users\malli\.gemini\antigravity\brain\e020d83a-102b-4605-a42b-93021bb61217\scratch\verify_navigation.py
```

---

## ☁️ Cloud Production Deployment

### Option A: Deploy on Render / Railway / Heroku
1. Connect your GitHub repository.
2. Provision a **Managed MySQL Database** instance.
3. Configure Environment Variables in the service settings:
   - `DB_URL`
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `APP_ADMIN_PASSWORD`
   - `APP_DEMO_DATA_ENABLED=false`
4. Set the Build Command:
   ```bash
   ./mvnw clean package -DskipTests
   ```
5. Set the Start Command:
   ```bash
   java -Dspring.profiles.active=prod -jar target/*.jar
   ```

### Option B: Docker Containerization
Build and run the standard container:
```bash
# Package the executable JAR
./mvnw clean package -DskipTests

# Run with custom profile
java -jar target/aicapstone-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## 🔑 Default Platform Portals

* **Public Landing Page**: `http://localhost:8080/`
* **Student Portal**: `http://localhost:8080/student/login`
* **Company Portal**: `http://localhost:8080/company/login`
* **Admin Portal**: `http://localhost:8080/admin/login` (Default: `admin@aicapstone.io` / `AdminPass123!`)
