# Research: SubTrack - Jakarta EE + Hibernate Implementation

## Technology Stack Decisions

### 1. Jakarta EE Version and Java Version

**Decision**: Jakarta EE 10 with Java 17 LTS

**Rationale**: 
- Jakarta EE 10 is the current stable release with full migration from javax to jakarta namespace
- Java 17 is the minimum required for Jakarta EE 10
- Java 17 offers good performance and is widely supported

**Alternatives Considered**:
- Jakarta EE 11 (newer but less stable)
- Java 21 (future-proof but newer)

---

### 2. Hibernate Version

**Decision**: Hibernate ORM 6.4.x (latest stable 6.x)

**Rationale**:
- Hibernate 6.x supports Jakarta Persistence 3.1
- Good stability and wide community support
- Avoids breaking changes in Hibernate 7 for production

**Alternatives Considered**:
- Hibernate 7 (cutting edge, Jakarta Persistence 3.2, requires more refactoring)
- Hibernate 5.x (legacy, no Jakarta namespace support)

---

### 3. Application Server

**Decision**: Payara Server (or WildFly)

**Rationale**:
- Full Jakarta EE implementation
- Good community support
- Easy to configure and deploy

**Alternatives Considered**:
- WildFly (more enterprise-focused)
- Apache TomEE (lighter weight)
- Embedded (for testing)

---

### 4. JSF Implementation

**Decision**: Jakarta Faces (Mojarra or MyFaces)

**Rationale**:
- Standard Jakarta EE specification
- Built-in support in application servers

**Alternatives Considered**:
- PrimeFaces (component library for richer UI)
- OmniFaces (utility library)

---

### 5. Testing Framework

**Decision**: JUnit 5 + Arquillian

**Rationale**:
- JUnit 5 is the standard for Java testing
- Arquillian provides container-based integration testing for Jakarta EE

**Alternatives Considered**:
- Mockito for unit tests
- Selenium for browser testing

---

### 6. Exchange Rate API

**Decision**: Free currency API (exchangerate-api.com free tier or similar)

**Rationale**:
- Provides free tier for development
- Easy to integrate
- Caching layer required for production

---

### 7. Notification Channels

**Decision**: 
- Email: Jakarta Mail (built into Jakarta EE)
- Telegram: Telegram Bot API
- WhatsApp: Twilio WhatsApp API (or similar)

---

## Architecture Patterns

### Layered Architecture (N-Tiers)
1. **Presentation Layer**: JSF + Managed Beans (CDI)
2. **Business Logic Layer**: Stateless Session Beans
3. **Persistence Layer**: JPA/Hibernate with DAO pattern

### Project Structure
```
src/main/java/com/subtrack/
├── entities/          # JPA entities
├── services/          # Business logic (Session Beans)
├── dao/               # Data Access Objects
├── controllers/       # JSF Managed Beans
├── converters/       # JSF converters
├── validators/       # JSF validators
└── util/              # Utility classes

src/main/webapp/
├── WEB-INF/
│   └── faces-config.xml
├── resources/         # CSS, JS, images
└── *.xhtml           # Facelets pages
```

---

## Database Schema Notes

PostgreSQL will be used with the following considerations:
- UUID for primary keys (recommended for distributed systems)
- Timestamps with timezone support
- JSONB for flexible fields if needed
- Indexes on frequently queried columns

---

## Implementation Approach

### Phase 1: Core Infrastructure
1. Setup project with Maven/Gradle
2. Configure persistence.xml
3. Create JPA entities
4. Implement DAOs

### Phase 2: Business Logic
1. Create Session Beans
2. Implement CRUD operations
3. Add validation

### Phase 3: UI Layer
1. Create JSF pages
2. Implement Managed Beans
3. Add navigation

### Phase 4: Advanced Features
1. Email integration
2. Notifications
3. Analytics

---

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| JSF complexity | Use CDI properly, keep Managed Beans thin |
| Performance | Use lazy loading, proper indexing |
| Currency API dependency | Cache rates, fallback to cached values |
| Email OAuth | Use standard OAuth2 flow |
