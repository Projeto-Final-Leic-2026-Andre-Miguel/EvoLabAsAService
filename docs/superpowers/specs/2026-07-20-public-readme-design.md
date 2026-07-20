# Public README Design

## Objective

Create an English-language root `README.md` that presents EvoLab-as-a-Service as Academic Project G30 and makes the public repository understandable, credible, and runnable by an external reader.

## Audience and positioning

The primary audience is lecturers, jury members, students, developers, and other visitors interested in LLM-driven algorithm evolution. The README will use a professional academic-showcase tone: it will explain the platform and its engineering without presenting it as a production-ready commercial service.

The title block will identify the project as:

> EvoLab-as-a-Service – A Web Platform for Automated Algorithm Optimization using LLM-Driven Evolution

It will identify the work as Project G30 and display the final grade of 18/20 discreetly as an academic result.

## Content structure

The root README will contain these sections, in this order:

1. Project title and short academic identification.
2. Overview explaining the problem and the role of OpenEvolve and LLM-driven evolution.
3. Key features grounded in the implemented UI and backend endpoints.
4. Architecture showing the React frontend, Spring Boot modules, PostgreSQL persistence, and Docker-based job execution.
5. Technology stack.
6. Prerequisites.
7. Quick start using the repository's local Docker Compose override.
8. Local development instructions for the database, backend, and frontend.
9. Environment variable reference containing names and purposes, never secret values.
10. Test commands for the Gradle, Vitest, and Playwright suites.
11. Concise repository structure.
12. Academic context listing students, jury, and final grade.
13. Acknowledgements for OpenEvolve and the principal open-source technologies used.

## Technical accuracy and safety

- Commands will be derived from the checked-in Gradle, npm, Docker Compose, and Playwright configuration.
- The Docker Compose quick start will use `docker-compose.yml` together with `docker-compose.local.yml` so the frontend is available over local HTTP.
- Required environment variables will be documented using placeholders. Values from the local untracked `.env` file will not be copied.
- The README will state that Docker access is needed to execute evolution jobs.
- Features not evidenced by the current code will not be claimed.
- No license section or license badge will be added because the repository has no `LICENSE` file.
- The generated README will not contain machine-specific absolute paths.

## Academic attribution

- Project: G30
- Students:
  - 51557, Miguel Morais Pinto
  - 51585, André Filipe de Sousa Vaz
- Jury:
  - José Simão
  - Filipe Freitas
  - F. Sousa
- Final grade: 18/20

## Validation

Documentation validation will include:

- checking every referenced file and npm script exists;
- checking the Gradle commands match the multi-module build;
- checking the Compose configuration resolves with placeholder environment variables;
- scanning the README for leaked secret values and machine-specific paths;
- reviewing the final diff for technical accuracy, readability, and public-repository suitability.
