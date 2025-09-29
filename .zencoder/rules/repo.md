---
description: Repository Information Overview
alwaysApply: true
---

# WCFC Manuals Server Information

## Summary
A web application designed to make pilot operating handbooks and equipment manuals available to WCFC (Wings of Carolina Flying Club) members. The system requires authentication through the WCFC Slack workspace to access the secured documents.

## Structure
- **client/**: Svelte-based frontend application
- **integration-tests/**: Integration test scripts and data
- **root/**: JSON configuration files for aircraft and equipment
- **scripts/**: Utility scripts for deployment and operations
- **src/**: Java backend application code
- **.mvn/**: Maven configuration files

## Output Directories
                                                                                                                        Note that these directories are in .gitignore, so you will normally be denied access to them.

- **target/**: Compiled Java classes and output from the Maven build process
- **docker/**: Dockerfile and build files, mostly copied from src/main/resources

## Backend (Java)

### Language & Runtime
**Language**: Java
**Version**: Java 21
**Build System**: Maven
**Package Manager**: Maven

### Dependencies
**Main Dependencies**:
- Dropwizard 5.0.0 (Web framework)
- Jackson 2.20.0 (JSON processing)
- MongoDB Driver 5.6.0 (Database)
- JWT 0.13.0 (Authentication)
- Google API Services (Email)

## Build & Installation
```bash
# Build the application
make

# Build Docker image
make build

# Run integration tests
make integration-tests
```

### Docker
**Base Image**: azul/zulu-openjdk-alpine:21-latest
**Configuration**: Uses Alpine Linux with Azul Zulu OpenJDK 21

### Testing
**Unit Tests**: As of now there are no unit tests for this app.
**Integration Tests**: There are end-to-end integration tests in `integration-tests/`.  These use Playwright for browser automation and WireMock for mocking external APIs.
**Run Command**:
```bash
make integration-tests
```

## Frontend (Svelte)

### Language & Runtime
**Language**: JavaScript
**Framework**: Svelte 5.x with SvelteKit 2.x
**Build System**: Vite 7.x
**Package Manager**: npm

### Dependencies
**Main Dependencies**:
- @pdftron/pdfjs-express 8.7.5 (PDF viewing)
- showdown 2.1.0 (Markdown processing)
- svelte-loading-spinners 0.3.6 (UI components)

**Development Dependencies**:
- @sveltejs/kit 2.43.5
- @sveltejs/adapter-node 5.3.2
- @sveltejs/adapter-static 3.0.9
- vite 7.1.7

### Build & Installation
```bash
cd client
npm ci --legacy-peer-deps
npm run build
```

## Deployment
This app is expected to run in Google Cloud Run.  In its production deployment, it does not have a long-running process; instead, its container is launched on demand when there is traffic.  As a result, internal maintenance tasks are scheduled opportunistically at app start.

