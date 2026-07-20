# Public README Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an accurate English root README that presents EvoLab-as-a-Service as Academic Project G30 and enables an external reader to understand, run, and test it.

**Architecture:** Create one root documentation entry point, `README.md`, grounded in the repository's Gradle, npm, Docker Compose, Spring, and React configuration. Keep operational instructions self-contained, use placeholders for secrets, and distinguish the Docker Compose quick start from component-level development.

**Tech Stack:** Markdown, Docker Compose, Kotlin 2.2, Spring Boot 4, Java 21, PostgreSQL, React 19, TypeScript, Vite, Vitest, Playwright

## Global Constraints

- Write the public README in English.
- Present the repository as Academic Project G30, not as a production-ready commercial service.
- Use the approved project title: `EvoLab-as-a-Service – A Web Platform for Automated Algorithm Optimization using LLM-Driven Evolution`.
- Include both students, all three jury members, and the final grade of 18/20 exactly as approved.
- Do not copy any secret value from `.env`; document only variable names, purposes, and safe example placeholders.
- Do not include machine-specific absolute paths.
- Do not claim features that are not evidenced by the current code.
- Do not add a license section or badge while no `LICENSE` file exists.

---

### Task 1: Create and validate the public root README

**Files:**
- Create: `README.md`
- Reference: `docs/superpowers/specs/2026-07-20-public-readme-design.md`
- Reference: `docker-compose.yml`
- Reference: `docker-compose.local.yml`
- Reference: `build.gradle.kts`
- Reference: `settings.gradle.kts`
- Reference: `evolab-frontend/EvoLabAsAService/package.json`
- Reference: `evolab-frontend/EvoLabAsAService/playwright.config.ts`

**Interfaces:**
- Consumes: checked-in build scripts, Compose services, environment-variable names, backend module layout, frontend scripts, and approved academic attribution.
- Produces: `README.md` as the public entry point for repository visitors.

- [x] **Step 1: Create the README title, overview, features, architecture, and stack**

Create `README.md` with:

- the approved full title and a concise subtitle;
- an academic project marker and `Final grade: 18/20`;
- an overview of managing LLM credentials, evolution configurations, projects, and Docker-executed OpenEvolve jobs;
- implemented capabilities including local/Google authentication, credential management, project lifecycle, metrics, checkpoints, and statistics;
- a text architecture diagram showing `React + Nginx -> Spring Boot HTTP -> service -> repo -> PostgreSQL`, with Docker job execution branching from the service layer;
- a technology table covering the frontend, backend, persistence, execution, build, and test tools.

- [x] **Step 2: Add safe setup and development instructions**

Document these prerequisites:

- Git;
- Docker Engine with Docker Compose;
- Java 21 for component-level backend development;
- Node.js 20.19+ or 22.12+ and npm for component-level frontend development, matching the Vite version in `package-lock.json`.

Document the Docker Compose quick start using an untracked `.env` containing these names:

```dotenv
POSTGRES_PASSWORD=replace-with-a-strong-password
GOOGLE_CLIENT_ID=replace-with-your-google-client-id
GOOGLE_CLIENT_SECRET=replace-with-your-google-client-secret
ENCRYPTION_SECRET_KEY=replace-with-a-long-random-secret
```

Then use:

```bash
./gradlew :evolab-backend:app:bootJar
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build
```

Explain that the backend JAR must be built before the Docker image because the checked-in backend Dockerfile is a runtime-only image. State that the local frontend is available at `http://localhost:8080` and that the backend remains internal to the Compose networks.

For component development, document a PostgreSQL container that uses the repository's schema-initializing database image and publishes port 5432:

```bash
docker build -t evolab-postgres ./evolab-backend/db
docker run --name evolab-db-dev --rm -d \
  -p 5432:5432 \
  -e POSTGRES_DB=evolab \
  -e POSTGRES_USER=evolabuser \
  -e POSTGRES_PASSWORD=replace-with-your-password \
  evolab-postgres
```

```bash
export DB_URL="jdbc:postgresql://localhost:5432/evolab?user=evolabuser&password=replace-with-your-password"
export GOOGLE_CLIENT_ID="replace-with-your-google-client-id"
export GOOGLE_CLIENT_SECRET="replace-with-your-google-client-secret"
export ENCRYPTION_SECRET_KEY="replace-with-a-long-random-secret"
./gradlew :evolab-backend:app:bootRun
```

```bash
cd evolab-frontend/EvoLabAsAService
npm install
npm run dev
```

Mention `./gradlew` can be replaced by `.\gradlew.bat` on Windows and explain the purpose of each environment variable in a table.

- [x] **Step 3: Add testing, repository structure, academic context, and acknowledgements**

Document these test commands:

```bash
./gradlew test
cd evolab-frontend/EvoLabAsAService
npm test -- --run
npm run test:e2e
```

Add a concise tree covering the six backend modules, React frontend, Compose files, and documentation directory. Add the exact approved attribution:

- Project G30;
- `51557 — Miguel Morais Pinto`;
- `51585 — André Filipe de Sousa Vaz`;
- jury members José Simão, Filipe Freitas, and F. Sousa;
- final grade 18/20.

Acknowledge OpenEvolve as the execution engine the platform integrates with and link to `https://github.com/algorithmicsuperintelligence/openevolve`.

- [x] **Step 4: Validate documentation accuracy and safety**

Run:

```powershell
git diff --check
rg -n "C:\\Users|pinto|changeit|TODO|TBD" README.md
git ls-files --error-unmatch build.gradle.kts settings.gradle.kts docker-compose.yml docker-compose.local.yml evolab-frontend/EvoLabAsAService/package.json evolab-frontend/EvoLabAsAService/playwright.config.ts
$env:POSTGRES_PASSWORD='validation-only'; $env:GOOGLE_CLIENT_ID='validation-only'; $env:GOOGLE_CLIENT_SECRET='validation-only'; $env:ENCRYPTION_SECRET_KEY='validation-only'; docker compose -f docker-compose.yml -f docker-compose.local.yml config --quiet
```

Expected results:

- `git diff --check` exits 0;
- the `rg` scan returns no matches;
- all referenced files are tracked;
- Docker Compose configuration validation exits 0.

Review every command and feature claim against the referenced files.

- [ ] **Step 5: Commit the README and implementation plan**

```bash
git add README.md docs/superpowers/plans/2026-07-20-public-readme.md
git commit -m "docs: add public project README"
```
