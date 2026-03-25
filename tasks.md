# Implementation Tasks: SubTrack - SaaS Subscription Management Platform

**Feature Branch**: `001-subtrack-jakarta`  
**Generated**: 2026-03-12  
**Input**: Feature specification from spec.md

## Summary

- **Total Tasks**: 110
- **User Stories**: 7 (3 P1, 2 P2, 2 P3)
- **MVP Scope**: User Story 1 (Authentication) - can be tested independently

## Phase 1: Setup (Project Initialization)

- [ ] T001 Create Jakarta EE project structure with Maven in src/main/java/com/subtrack/
- [ ] T002 Configure pom.xml with Jakarta EE 10, Hibernate 6.4.x, PostgreSQL JDBC Driver dependencies
- [ ] T003 Create src/main/webapp/WEB-INF/web.xml with JSF and session configuration
- [ ] T004 Create src/main/webapp/WEB-INF/faces-config.xml with navigation rules
- [ ] T005 Create src/main/resources/META-INF/persistence.xml with Hibernate and PostgreSQL configuration
- [ ] T006 Configure logging in src/main/resources/logback.xml or logging.properties
- [ ] T007 Create src/main/resources/messages.properties for i18n support
- [ ] T008 Setup database schema SQL script in db/schema.sql for PostgreSQL

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T009 Create enums: AccountType (B2C, FREELANCE, B2B), Role (CLIENT, ADMIN), Frequency (WEEKLY, MONTHLY, QUARTERLY, ANNUAL), SubscriptionStatus (ACTIVE, PAUSED, CANCELLED), AlertChannel (EMAIL, TELEGRAM, WHATSAPP)
- [ ] T010 [P] Create Client entity in src/main/java/com/subtrack/entity/Client.java with all fields and JPA annotations
- [ ] T011 [P] Create Category entity in src/main/java/com/subtrack/entity/Category.java
- [ ] T012 Create ClientDAO interface in src/main/java/com/subtrack/dao/ClientDAO.java
- [ ] T013 Create ClientDAOImpl in src/main/java/com/subtrack/dao/ClientDAOImpl.java with EntityManager injection
- [ ] T014 Create CategoryDAO interface in src/main/java/com/subtrack/dao/CategoryDAO.java
- [ ] T015 Create CategoryDAOImpl in src/main/java/com/subtrack/dao/CategoryDAOImpl.java
- [ ] T016 Create ClientService stateless session bean in src/main/java/com/subtrack/service/ClientService.java
- [ ] T017 Create PasswordUtil in src/main/java/com/subtrack/util/PasswordUtil.java for BCrypt hashing
- [ ] T018 Create DatabaseInitializer in src/main/java/com/subtrack/util/DatabaseInitializer.java to seed default categories

## Phase 3: User Story 1 - Authentication (Priority: P1)

**Independent Test**: Create account, login, verify dashboard access - delivers functioning auth system

### Entity Layer
- [ ] T019 [US1] Implement Client entity relationships (@OneToMany for subscriptions, @OneToOne for email integration)

### DAO Layer
- [ ] T020 [US1] Add findByEmail method to ClientDAO interface and implementation

### Service Layer
- [ ] T021 [US1] Add registerClient, authenticate, findById methods to ClientService
- [ ] T022 [US1] Add password validation logic in ClientService

### Controller Layer
- [ ] T023 [US1] Create AuthController managed bean in src/main/java/com/subtrack/controller/AuthController.java with login, register, logout actions
- [ ] T024 [US1] Create session scoped user context in src/main/java/com/subtrack/controller/UserContext.java

### View Layer
- [ ] T025 [US1] Create login.xhtml in src/main/webapp/login.xhtml with email/password form
- [ ] T026 [US1] Create register.xhtml in src/main/webapp/register.xhtml with registration form
- [ ] T027 [US1] Create layout template in src/main/webapp/templates/layout.xhtml with header/footer
- [ ] T028 [US1] Add CSS styling in src/main/webapp/resources/css/style.css
- [ ] T029 [US1] Configure form-based authentication in web.xml with login-config

### Test
- [ ] T030 [US1] Test registration flow: valid email, password validation
- [ ] T031 [US1] Test login flow: correct credentials grant access, incorrect shows error
- [ ] T032 [US1] Test logout: session invalidated, redirect to login

## Phase 4: User Story 2 - Subscription Management (Priority: P1)

**Independent Test**: CRUD operations on subscriptions, verify changes in dashboard and database

### Entity Layer
- [ ] T033 [US2] Create Subscription entity in src/main/java/com/subtrack/entity/Subscription.java with all fields and JPA annotations
- [ ] T034 [US2] Create PaymentHistory entity in src/main/java/com/subtrack/entity/PaymentHistory.java
- [ ] T035 [US2] Add @ManyToOne relationship from Subscription to Client and Category
- [ ] T036 [US2] Add @OneToMany relationship from Subscription to PaymentHistory

### DAO Layer
- [ ] T037 [US2] Create SubscriptionDAO interface in src/main/java/com/subtrack/dao/SubscriptionDAO.java
- [ ] T038 [US2] Create SubscriptionDAOImpl in src/main/java/com/subtrack/dao/SubscriptionDAOImpl.java
- [ ] T039 [US2] Create PaymentHistoryDAO in src/main/java/com/subtrack/dao/PaymentHistoryDAO.java

### Service Layer
- [ ] T040 [US2] Create SubscriptionService in src/main/java/com/subtrack/service/SubscriptionService.java with CRUD operations
- [ ] T041 [US2] Add calculateMonthlyCost method to SubscriptionService
- [ ] T042 [US2] Add calculateAnnualCost method to SubscriptionService
- [ ] T043 [US2] Add pauseSubscription and cancelSubscription methods with status transition logic

### Controller Layer
- [ ] T044 [US2] Create SubscriptionController managed bean in src/main/java/com/subtrack/controller/SubscriptionController.java
- [ ] T045 [US2] Add create, edit, delete, pause actions to SubscriptionController
- [ ] T046 [US2] Create SubscriptionConverter in src/main/java/com/subtrack/converter/SubscriptionConverter.java for JSF

### View Layer
- [ ] T047 [US2] Create dashboard.xhtml in src/main/webapp/dashboard.xhtml with subscription list
- [ ] T048 [US2] Create subscriptions/list.xhtml in src/main/webapp/subscriptions/list.xhtml
- [ ] T049 [US2] Create subscriptions/create.xhtml in src/main/webapp/subscriptions/create.xhtml with form
- [ ] T050 [US2] Create subscriptions/edit.xhtml in src/main/webapp/subscriptions/edit.xhtml

### Test
- [ ] T051 [US2] Test create subscription: verify appears in list
- [ ] T052 [US2] Test edit subscription: verify changes saved
- [ ] T053 [US2] Test pause/cancel: verify status changes, costs adjusted

## Phase 5: User Story 3 - Financial Dashboard & Analytics (Priority: P1)

**Independent Test**: View dashboard, verify accurate monthly/annual calculations by category

### Entity Layer
- [ ] T054 [US3] Create ExchangeRate entity in src/main/java/com/subtrack/entity/ExchangeRate.java

### DAO Layer
- [ ] T055 [US3] Create ExchangeRateDAO in src/main/java/com/subtrack/dao/ExchangeRateDAO.java

### Service Layer
- [ ] T056 [US3] Create ExchangeRateService in src/main/java/com/subtrack/service/ExchangeRateService.java
- [ ] T057 [US3] Add convertToLocalCurrency method with exchange rate lookup
- [ ] T058 [US3] Add getSpendingByCategory method for analytics
- [ ] T059 [US3] Add getMonthlyTrend method for spending over time
- [ ] T060 [US3] Create CategoryService in src/main/java/com/subtrack/service/CategoryService.java

### Controller Layer
- [ ] T061 [US3] Create DashboardController in src/main/java/com/subtrack/controller/DashboardController.java
- [ ] T062 [US3] Add getTotalMonthlyCost, getTotalAnnualCost, getCategoryBreakdown methods

### View Layer
- [ ] T063 [US3] Update dashboard.xhtml with cost summary cards
- [ ] T064 [US3] Create analytics.xhtml in src/main/webapp/analytics.xhtml with charts
- [ ] T065 [US3] Integrate Chart.js or similar for visual charts in analytics.xhtml

### Test
- [ ] T066 [US3] Test dashboard displays correct totals
- [ ] T067 [US3] Test currency conversion displays correct values
- [ ] T068 [US3] Test analytics charts render correctly

## Phase 6: User Story 4 - Email-Based Invoice Automation (Priority: P2)

**Independent Test**: Connect email, forward test invoice, verify subscription created automatically

### Entity Layer
- [ ] T069 [P] [US4] Create EmailIntegration entity in src/main/java/com/subtrack/entity/EmailIntegration.java
- [ ] T070 [P] [US4] Create Invoice entity in src/main/java/com/subtrack/entity/Invoice.java

### DAO Layer
- [ ] T071 [P] [US4] Create EmailIntegrationDAO in src/main/java/com/subtrack/dao/EmailIntegrationDAO.java
- [ ] T072 [P] [US4] Create InvoiceDAO in src/main/java/com/subtrack/dao/InvoiceDAO.java

### Service Layer
- [ ] T073 [US4] Create EmailIntegrationService in src/main/java/com/subtrack/service/EmailIntegrationService.java
- [ ] T074 [US4] Create InvoiceService in src/main/java/com/subtrack/service/InvoiceService.java
- [ ] T075 [US4] Add OAuth2 flow for Gmail/email connection
- [ ] T076 [US4] Add parseInvoiceEmail method using Google Gemini AI
- [ ] T077 [US4] Add createSubscriptionFromInvoice method
- [ ] T077b [US4] Add scheduled task for processing new invoices within 5 minutes in src/main/java/com/subtrack/service/InvoiceScheduler.java

### Controller Layer
- [ ] T078 [US4] Create EmailController in src/main/java/com/subtrack/controller/EmailController.java

### View Layer
- [ ] T079 [US4] Create profile.xhtml in src/main/webapp/profile.xhtml with email connection UI

## Phase 7: User Story 5 - Alert & Notification Configuration (Priority: P2)

**Independent Test**: Configure alerts, verify notifications sent at specified time

### Entity Layer
- [ ] T080 [P] [US5] Create AlertRule entity in src/main/java/com/subtrack/entity/AlertRule.java

### DAO Layer
- [ ] T081 [P] [US5] Create AlertRuleDAO in src/main/java/com/subtrack/dao/AlertRuleDAO.java

### Service Layer
- [ ] T082 [US5] Create AlertService in src/main/java/com/subtrack/service/AlertService.java
- [ ] T083 [US5] Add sendEmailNotification method using Jakarta Mail
- [ ] T084 [US5] Add sendTelegramNotification method using Telegram Bot API
- [ ] T085 [US5] Add sendWhatsAppNotification method using Twilio API
- [ ] T086 [US5] Add checkAndSendAlerts scheduled task for cron-based checking

### Controller Layer
- [ ] T087 [US5] Create AlertController in src/main/java/com/subtrack/controller/AlertController.java

### View Layer
- [ ] T088 [US5] Create alerts.xhtml in src/main/webapp/alerts.xhtml with alert configuration UI

## Phase 8: User Story 6 - Subscription Optimization (Priority: P3)

**Independent Test**: Create duplicate subscriptions, verify system flags them

### Service Layer
- [ ] T089 [US6] Add detectRedundantSubscriptions method to SubscriptionService
- [ ] T090 [US6] Add detectUnusedSubscriptions method with usage pattern analysis

### Controller Layer
- [ ] T091 [US6] Add optimization recommendations to DashboardController

### View Layer
- [ ] T092 [US6] Update dashboard.xhtml with optimization suggestions panel

## Phase 9: User Story 7 - Admin Back-Office Management (Priority: P3)

**Independent Test**: Login as admin, perform user management and catalog updates

### Entity Layer
- [ ] T093 [P] [US7] Create SaaSService entity in src/main/java/com/subtrack/entity/SaaSService.java

### DAO Layer
- [ ] T094 [P] [US7] Create SaaSServiceDAO in src/main/java/com/subtrack/dao/SaaSServiceDAO.java

### Service Layer
- [ ] T095 [US7] Create AdminService in src/main/java/com/subtrack/service/AdminService.java
- [ ] T096 [US7] Add manageUsers method (suspend, activate, remove)
- [ ] T097 [US7] Create SaaSCatalogService in src/main/java/com/subtrack/service/SaaSCatalogService.java
- [ ] T098 [US7] Add catalog CRUD operations

### Controller Layer
- [ ] T099 [US7] Create AdminController in src/main/java/com/subtrack/controller/AdminController.java

### View Layer
- [ ] T100 [US7] Create admin/index.xhtml in src/main/webapp/admin/index.xhtml with admin dashboard
- [ ] T101 [US7] Add user management section with table and actions
- [ ] T102 [US7] Add SaaS catalog management section

## Phase 10: Polish & Cross-Cutting Concerns

- [ ] T103 Add input validation to all forms using JSF validators
- [ ] T104 Add global exception handler with error pages
- [ ] T105 Implement empty state UI for no subscriptions
- [ ] T106 Add export functionality (CSV/PDF) for subscriptions
- [ ] T107 Add payment history tracking for price change detection
- [ ] T108 Add system logging for audit trail
- [ ] T109 Optimize queries with proper indexing
- [ ] T110 Add unit tests for core services

## Dependencies & Execution Order

```
Phase 1 (Setup) → Phase 2 (Foundational)
                        ↓
Phase 3 (US1: Auth) ← Required for all
    ↓
Phase 4 (US2: Subscriptions) → Phase 5 (US3: Dashboard)
                                    ↓
Phase 6 (US4: Email) → Phase 7 (US5: Alerts)
        ↓                    ↓
Phase 8 (US6: Optimization) → Phase 9 (US7: Admin)
                ↓
        Phase 10 (Polish)
```

## Parallel Execution Opportunities

| Tasks | Can Run In Parallel |
|-------|-------------------|
| T010, T011 | Yes - independent entities |
| T012, T014 | Yes - independent DAOs |
| T069, T070 | Yes - independent entities for US4 |
| T080, T093 | Yes - independent entities for US5/US7 |
| T071, T072 | Yes - independent DAOs for US4 |
| T094, T081 | Yes - independent DAOs for US7/US5 |

## Independent Test Criteria by User Story

| Story | Test Criteria | Deliverable |
|-------|---------------|-------------|
| US1 | Create account, login, access dashboard | Working auth system |
| US2 | CRUD operations, verify in DB | Subscription management |
| US3 | View dashboard, accurate calculations | Financial analytics |
| US4 | Connect email, forward invoice, auto-create | Invoice automation |
| US5 | Configure alerts, receive notifications | Alert system |
| US6 | Create duplicates, see optimization suggestions | Optimization features |
| US7 | Admin login, manage users/catalog | Admin back-office |

## MVP Scope

**Recommended MVP**: User Story 1 (Authentication) + User Story 2 (Subscription Management)

This delivers:
- Complete working authentication system
- Core subscription CRUD
- Basic dashboard with cost calculations

Total MVP Tasks: ~40 tasks (T001-T053)
