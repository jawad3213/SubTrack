<!--
Sync Impact Report
==================
Version: 0.0.0 → 1.0.0
Type: MINOR - New constitution for SubTrack project

Modified Principles:
- N/A (new constitution)

Added Sections:
- Core Principles (5 principles: JPA Entity Design, Session Management, Input Validation, Error Handling, Security)
- Technology Stack (with version constraints)
- Development Workflow (with naming conventions)
- Governance

Removed Sections:
- N/A

Templates Updated: ✅
- .specify/templates/constitution-template.md (source template unchanged)
- .specify/memory/constitution.md (populated with SubTrack-specific values)

Templates Pending: ⚠
- None

Follow-up TODOs:
- None
-->

# SubTrack Constitution

## Core Principles

### I. JPA Entity Design
Every entity must follow Jakarta EE JPA standards: proper annotations, relationships, lazy loading strategies, and bidirectional consistency. All entities require equals/hashCode based on business keys, not generated IDs.

### II. Session Management
Session-scoped managed beans must be used for user context and authentication state. View-scoped beans for page-specific data. Stateful conversations for multi-step workflows. Session timeout handling required.

### III. Input Validation (NON-NEGOTIABLE)
All user inputs MUST be validated using JSF validators and Bean Validation (@NotNull, @Size, @Email, etc.). Server-side validation is mandatory - client-side validation is enhancement only. No trusted data from client without validation.

### IV. Error Handling
Global exception handler with custom error pages (404, 500). All service layer exceptions must be caught and logged. User-friendly error messages returned to UI. No stack traces exposed to end users.

### V. Security
Authentication required for all protected routes. Authorization checks on every action. Passwords MUST be hashed using BCrypt. SQL injection prevention via parameterized queries. XSS prevention via output encoding.

## Technology Stack

Jakarta EE 10 with JSF 4.0 for UI, Hibernate ORM 6.4.x for persistence, PostgreSQL for data storage, PrimeFaces 14 for component library. Maven for build management. All dependencies must be Jakarta EE 10 compatible.

### Version Constraints
- Java 17 minimum
- Jakarta EE 10 compatible only
- Hibernate 6.4.x
- PostgreSQL JDBC 42.7.x

## Development Workflow

All features follow the task breakdown in tasks.md. Code review required before merge. Tests must pass before deployment. Database migrations tracked in db/schema.sql. All pages use templates/layout.xhtml for consistency.

### Naming Conventions
- Controllers: *Controller.java (session/view scoped)
- Services: *Service.java (stateless EJBs)
- DAOs: *DAO.java, *DAOImpl.java
- Entities: Entity names singular, camelCase properties

## Governance

Constitution supersedes all other practices. Amendments require documentation and testing. All PRs/reviews must verify compliance with these principles. Use tasks.md for implementation tracking.

**Version**: 1.0.0 | **Ratified**: 2026-03-12 | **Last Amended**: 2026-03-12
