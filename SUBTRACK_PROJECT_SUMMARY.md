# SubTrack Project - Comprehensive Code Summary

## Project Overview
**SubTrack** is a SaaS Subscription Management Platform built with:
- **Java 17** with **Jakarta EE 10**
- **Hibernate ORM 6.4.x** for database access
- **PostgreSQL** for database
- **JSF (Jakarta Faces)** with **PrimeFaces** for UI
- **JAX-RS** REST services
- **JPA/Jakarta Persistence** for ORM

---

## 1. CONTROLLERS & JSF PAGES

### AdminController
**Location:** `src/main/java/com/subtrack/controller/AdminController.java`
- **Scope:** Session
- **Purpose:** Admin panel for user and service management
- **JSF Page:** Maps to `/admin/` area
- **Key Methods:**
  - `loadUsers()` - Retrieves all users
  - `loadCatalog()` - Retrieves all SaaS services in catalog
  - `suspendUser(UUID userId)` - Deactivates a user account
  - `activateUser(UUID userId)` - Reactivates a user account
  - `removeUser(UUID userId)` - Deletes a user
  - `addServiceToCatalog()` - Adds new SaaS service to catalog
  - `deleteService()` - Removes service from catalog

### AlertController
**Location:** `src/main/java/com/subtrack/controller/AlertController.java`
- **Scope:** Session
- **Purpose:** Manages subscription renewal alerts
- **JSF Page:** `alerts.xhtml`
- **Key Methods:**
  - `loadAlerts()` - Loads all alerts for user's subscriptions
  - `createAlert()` - Creates alert for a subscription
  - `deleteAlert()` - Deletes an alert
  - `toggleAlert(AlertRule)` - Toggles alert active status
- **Data Members:**
  - `alertRules` - List of alert rules
  - `selectedAlert` - Currently selected alert
  - `selectedSubscription` - Target subscription for alert
  - `channel` - Alert channel (EMAIL, TELEGRAM, WHATSAPP)
  - `timingDays` - Days before renewal to alert (default: 3)

### AuthController
**Location:** `src/main/java/com/subtrack/controller/AuthController.java`
- **Scope:** Session
- **Purpose:** Authentication and registration
- **JSF Pages:** `login.xhtml`, `register.xhtml`
- **Key Methods:**
  - `login()` - Authenticates user with email/password
  - `register()` - Creates new client account
  - `logout()` - Invalidates session
  - `isLoggedIn()` - Checks login status
  - `isAdmin()` - Checks if user is admin
- **Validation:**
  - Password must match confirmPassword
  - Account type enum: B2C, B2B
  - Password strength validation via ClientService

### DashboardController
**Location:** `src/main/java/com/subtrack/controller/DashboardController.java`
- **Scope:** ViewScoped
- **Purpose:** Main dashboard with analytics and insights
- **JSF Page:** `dashboard.xhtml`
- **Key Methods:**
  - `loadDashboard()` - Loads all dashboard data
  - `calculateTotals()` - Computes monthly/annual costs
  - `calculateCategoryBreakdown()` - Analyzes spending by category
  - `getMonthlyTrend()` - Retrieves 6-month spending trend
  - `getSpendingByCategory()` - Maps spending by category
  - `getOptimizationSuggestions()` - Detects redundant/unused subscriptions
  - `getTotalPotentialSavings()` - Calculates possible savings
- **Displays:**
  - Recent subscriptions (max 5)
  - Total monthly/annual costs
  - Active subscription count
  - Category breakdown with costs
  - Currency conversion
  - Optimization suggestions

### EmailController
**Location:** `src/main/java/com/subtrack/controller/EmailController.java`
- **Scope:** Session
- **Purpose:** Email integration management
- **JSF Page:** `settings.xhtml` (integration section)
- **Key Methods:**
  - `loadEmailIntegration()` - Loads user's email integration
  - `connectEmail()` - Connects email account (mock OAuth)
  - `disconnectEmail()` - Disconnects email
  - `isTokenValid()` - Validates access token
- **Note:** Uses mock tokens for demo purposes

### InvoiceController
**Location:** `src/main/java/com/subtrack/controller/InvoiceController.java`
- **Scope:** Session
- **Purpose:** Invoice management and processing
- **JSF Page:** `invoices.xhtml`
- **Key Methods:**
  - `loadInvoices()` - Retrieves all invoices for user
  - `processInvoice()` - Marks invoice as processed
  - `deleteInvoice()` - Deletes an invoice
  - `selectInvoice(Invoice)` - Selects invoice for action
  - `getFilteredInvoices()` - Filters by processed status
  - `getPendingCount()` - Returns count of unprocessed invoices

### PaymentController
**Location:** `src/main/java/com/subtrack/controller/PaymentController.java`
- **Scope:** Session
- **Purpose:** Payment history and analysis
- **JSF Page:** `payments.xhtml`
- **Key Methods:**
  - `loadPayments()` - Retrieves all payments for user
  - `filterBySubscription()` - Filters payments by subscription
  - `filterByDateRange()` - Filters payments by date
  - `clearFilters()` - Resets all filters
  - `calculateTotal()` - Sums filtered payments
- **Filters:**
  - By subscription
  - By date range (startDate, endDate)

### SettingsController
**Location:** `src/main/java/com/subtrack/controller/SettingsController.java`
- **Scope:** Session
- **Purpose:** User account settings management
- **JSF Page:** `settings.xhtml`
- **Key Methods:**
  - `loadSettings()` - Loads current user settings
  - `updateProfile()` - Updates name and timezone
  - `changePassword()` - Changes user password
  - `updateNotificationPreferences()` - Updates notification channels
  - `deleteAccount()` - Deletes user account
- **Notification Channels:**
  - Email notifications
  - Telegram (enabled, chat ID)
  - WhatsApp (enabled, phone number)

### SubscriptionController
**Location:** `src/main/java/com/subtrack/controller/SubscriptionController.java`
- **Scope:** Session
- **Purpose:** Subscription lifecycle management
- **JSF Pages:** 
  - `subscriptions/list.xhtml` - View all subscriptions
  - `subscriptions/create.xhtml` - Create new
  - `subscriptions/edit.xhtml` - Edit existing
  - `subscriptions/detail.xhtml` - View details
- **Key Methods:**
  - `createSubscription()` - Creates new subscription
  - `updateSubscription()` - Updates existing subscription
  - `deleteSubscription()` - Deletes subscription
  - `pauseSubscription()` - Pauses subscription
  - `cancelSubscription()` - Cancels subscription
  - `reactivateSubscription()` - Reactivates subscription
  - `prepareEdit(Subscription)` - Loads subscription for editing
  - `getActiveSubscriptions()` - Returns only active subscriptions
  - `getTotalMonthlyCost()` - Calculates total monthly spending
  - `getTotalAnnualCost()` - Calculates total annual spending
- **Form Fields:**
  - name, description, price, originalCurrency
  - frequency (WEEKLY, MONTHLY, QUARTERLY, ANNUAL)
  - category, startDate, logoUrl, cancelLink, notes

### UserContext
**Location:** `src/main/java/com/subtrack/controller/UserContext.java`
- **Scope:** Session
- **Purpose:** Session management and user info access
- **Key Methods:**
  - `setCurrentUser(Client)` - Sets session user
  - `getCurrentUser()` - Retrieves session user
  - `isLoggedIn()` - Checks if user is logged in
  - `isAdmin()` - Checks admin status
  - `getUserName()` - Returns user's full name
  - `getClientId()` - Returns user's UUID
  - `invalidate()` - Clears session

---

## 2. SERVICES

### AdminService
**Location:** `src/main/java/com/subtrack/service/AdminService.java`
- **Scope:** ApplicationScoped
- **Purpose:** Admin operations on users
- **Methods:**
  - `getAllUsers()` - Retrieves all clients
  - `getUserById(UUID)` - Gets specific user
  - `suspendUser(UUID)` - Deactivates user
  - `activateUser(UUID)` - Activates user
  - `removeUser(UUID)` - Deletes user

### AlertService
**Location:** `src/main/java/com/subtrack/service/AlertService.java`
- **Scope:** ApplicationScoped
- **Purpose:** Alert rule management and notifications
- **Methods:**
  - `findAll()` - Gets all alerts
  - `findBySubscriptionId(UUID)` - Gets alerts for subscription
  - `findById(UUID)` - Gets specific alert
  - `createAlert(AlertRule)` - Creates alert
  - `updateAlert(AlertRule)` - Updates alert
  - `deleteAlert(AlertRule)` - Deletes alert
  - `createAlertForSubscription()` - Creates alert with defaults
  - `getActiveAlerts()` - Gets enabled alerts
  - `getAlertsDueSoon(int)` - Gets alerts within N days
  - `checkAndSendAlerts()` - Processes and sends notifications
- **Notification Methods (TODO - Empty):**
  - `sendEmailNotification(AlertRule)`
  - `sendTelegramNotification(AlertRule)`
  - `sendWhatsAppNotification(AlertRule)`

### CategoryService
**Location:** `src/main/java/com/subtrack/service/CategoryService.java`
- **Scope:** ApplicationScoped
- **Purpose:** Category management for subscriptions
- **Methods:**
  - `findAll()` - Gets all categories
  - `findAllOrderByName()` - Gets categories sorted by name
  - `findById(UUID)` - Gets specific category
  - `findByName(String)` - Finds category by name
  - `create(Category)` - Creates new category
  - `update(Category)` - Updates category
  - `delete(Category)` - Deletes category

### ClientService
**Location:** `src/main/java/com/subtrack/service/ClientService.java`
- **Scope:** ApplicationScoped
- **Purpose:** User account management
- **Methods:**
  - `registerClient()` - Creates new account with validation
  - `authenticate(email, password)` - Authenticates user
  - `findById(UUID)` - Gets user by ID
  - `findByEmail(String)` - Finds user by email
  - `findAll()` - Gets all users
  - `findAdmins()` - Gets admin users
  - `findActiveClients()` - Gets non-deactivated users
  - `updateClient(Client)` - Updates user
  - `deactivateClient(UUID)` - Deactivates user
  - `activateClient(UUID)` - Activates user
  - `deleteClient(UUID)` - Deletes user
  - `isEmailTaken(String)` - Checks email availability
  - `changePassword(UUID, currentPassword, newPassword)` - Updates password
  - `updateNotificationPreferences()` - Updates notification settings
  - `deleteAccount(UUID)` - Deletes entire account
- **Validation:**
  - Password strength: min 8 chars, uppercase, lowercase, digits
  - Email uniqueness check

### EmailIntegrationService
**Location:** `src/main/java/com/subtrack/service/EmailIntegrationService.java`
- **Scope:** ApplicationScoped
- **Purpose:** Email account integration (OAuth mock)
- **Methods:**
  - `findByClientId(UUID)` - Gets user's email integration
  - `findById(UUID)` - Gets specific integration
  - `connectEmail()` - Creates/updates email integration
  - `disconnectEmail()` - Deactivates email integration
  - `refreshToken()` - Updates access token
  - `isTokenValid(UUID)` - Checks token expiration
- **Note:** Currently uses mock tokens for demo

### ExchangeRateService
**Location:** `src/main/java/com/subtrack/service/ExchangeRateService.java`
- **Scope:** ApplicationScoped
- **Purpose:** Currency conversion and spending analytics
- **Methods:**
  - `findAll()` - Gets all exchange rates
  - `findByCurrencies(from, to)` - Gets specific rate
  - `save(ExchangeRate)` - Creates/updates rate
  - `convertToLocalCurrency()` - Converts amount with fallback logic
  - `getSpendingByCategory()` - Maps category spending in local currency
  - `getMonthlyTrend(UUID, months)` - Gets spending trend (mock data)
- **Inner Class:** `MonthlySpending` - Represents monthly spending data
- **Fallback Logic:**
  - Direct conversion if available
  - Inverse conversion if available
  - Returns original amount if no rate found

### InvoiceScheduler
**Location:** `src/main/java/com/subtrack/service/InvoiceScheduler.java`
- **Scope:** Stateless EJB
- **Purpose:** Automated invoice processing
- **Methods:**
  - `processPendingInvoices()` - Scheduled task (every 5 minutes)
- **Schedule:** `@Schedule(minute = "*/5", hour = "*")`
- **Note:** Currently logs errors but catches exceptions

### InvoiceService
**Location:** `src/main/java/com/subtrack/service/InvoiceService.java`
- **Scope:** ApplicationScoped
- **Purpose:** Invoice management
- **Methods:**
  - `findAll()` - Gets all invoices
  - `findByClientId(UUID)` - Gets user's invoices
  - `findById(UUID)` - Gets specific invoice
  - `createInvoice()` - Creates invoice entry
  - `processInvoice(UUID)` - Marks invoice processed
  - `processInvoice(Invoice)` - Marks invoice processed (overload)
  - `delete(Invoice)` - Deletes invoice
  - `createSubscriptionFromInvoice()` - Creates subscription from invoice
  - `getUnprocessedInvoices()` - Gets pending invoices
- **TODO - Empty:**
  - `parseInvoiceEmail()` - Email parsing logic

### SaaSCatalogService
**Location:** `src/main/java/com/subtrack/service/SaaSCatalogService.java`
- **Scope:** ApplicationScoped
- **Purpose:** SaaS service catalog management
- **Methods:**
  - `getAllServices()` - Gets all services
  - `getServiceById(UUID)` - Gets specific service
  - `searchServices(String)` - Searches by name
  - `createService(SaaSService)` - Creates service
  - `updateService(SaaSService)` - Updates service
  - `deleteService(UUID)` - Deletes service
  - `addServiceToCatalog()` - Creates service with parameters

### SubscriptionService
**Location:** `src/main/java/com/subtrack/service/SubscriptionService.java`
- **Scope:** ApplicationScoped
- **Purpose:** Core subscription lifecycle and analytics
- **Methods:**
  - `findAll()` - Gets all subscriptions
  - `findByClientId(UUID)` - Gets user's subscriptions
  - `findActiveByClientId(UUID)` - Gets active subscriptions only
  - `findById(UUID)` - Gets specific subscription
  - `create(Subscription)` - Creates subscription + payment record
  - `update(Subscription)` - Updates subscription
  - `delete(Subscription)` - Deletes subscription
  - `calculateMonthlyCost(UUID)` - Total monthly for active subs
  - `calculateAnnualCost(UUID)` - Total annual for active subs
  - `pauseSubscription()` - Pauses subscription
  - `cancelSubscription()` - Cancels subscription
  - `reactivateSubscription()` - Reactivates subscription
  - `recordPayment()` - Creates payment history entry
  - `getPaymentHistoryBySubscriptionId(UUID)` - Gets payments for sub
  - `getPaymentHistoryByClientId(UUID)` - Gets all user payments
  - `calculateTotalByCategory()` - Spending by category
  - `detectRedundantSubscriptions()` - Finds duplicate services
  - `detectUnusedSubscriptions()` - Finds inactive services
- **Inner Class:** `OptimizationSuggestion` - Recommendation for savings

---

## 3. ENTITIES & MODELS

### AlertRule
**Fields:**
- `id` (UUID, PK, generated)
- `subscription` (ManyToOne → Subscription, required)
- `channel` (Enum: EMAIL, TELEGRAM, WHATSAPP)
- `timingDays` (Integer, default: 3)
- `isActive` (Boolean, default: true)
- `createdAt` (LocalDateTime, auto)
- `updatedAt` (LocalDateTime, auto)

### Category
**Fields:**
- `id` (UUID, PK, generated)
- `name` (String, unique, required, max 100)
- `description` (TEXT)
- `icon` (String, max 100)
- `color` (String, max 7 - hex color)
- `createdAt` (LocalDateTime, auto)
- `subscriptions` (OneToMany → Subscription)

### Client (User Account)
**Fields:**
- `id` (UUID, PK, generated)
- `email` (String, unique, required, max 255)
- `password` (String, required, max 255 - hashed)
- `firstName` (String, max 100)
- `lastName` (String, max 100)
- `accountType` (Enum: B2C, B2B, default: B2C)
- `role` (Enum: CLIENT, ADMIN, default: CLIENT)
- `timezone` (String, max 50)
- `isActive` (Boolean, default: true)
- `createdAt` (LocalDateTime, auto)
- `updatedAt` (LocalDateTime, auto)
- `subscriptions` (OneToMany → Subscription, cascade)
- `emailIntegration` (OneToOne → EmailIntegration, cascade)
- `invoices` (OneToMany → Invoice, cascade)
- **Notification Preferences:**
  - `emailNotifications` (Boolean, default: true)
  - `telegramEnabled` (Boolean, default: false)
  - `telegramChatId` (String, max 50)
  - `whatsappEnabled` (Boolean, default: false)
  - `whatsappNumber` (String, max 20)
- **Methods:**
  - `getFullName()` - Combines first and last name

### EmailIntegration
**Fields:**
- `id` (UUID, PK, generated)
- `client` (OneToOne → Client, required)
- `emailAddress` (String, required)
- `accessToken` (TEXT - OAuth token)
- `refreshToken` (TEXT - OAuth refresh)
- `tokenExpiresAt` (LocalDateTime)
- `isActive` (Boolean, default: true)
- `createdAt` (LocalDateTime, auto)
- `updatedAt` (LocalDateTime, auto)
- **Methods:**
  - `isTokenExpired()` - Checks token expiration

### ExchangeRate
**Fields:**
- `id` (UUID, PK, generated)
- `fromCurrency` (String, required, length 3)
- `toCurrency` (String, required, length 3)
- `rate` (BigDecimal, precision 15, scale 6)
- `updatedAt` (LocalDateTime, auto-updated)
- **Methods:**
  - `convert(BigDecimal)` - Applies rate to amount

### Invoice
**Fields:**
- `id` (UUID, PK, generated)
- `client` (ManyToOne → Client, required)
- `rawContent` (TEXT - email body)
- `serviceName` (String - extracted service name)
- `amount` (BigDecimal, precision 10, scale 2)
- `currency` (String, length 3, default: USD)
- `invoiceDate` (LocalDate)
- `isProcessed` (Boolean, default: false)
- `createdAt` (LocalDateTime, auto)

### PaymentHistory
**Fields:**
- `id` (UUID, PK, generated)
- `subscription` (ManyToOne → Subscription, required)
- `amount` (BigDecimal, precision 10, scale 2, required)
- `currency` (String, length 3, required)
- `paymentDate` (LocalDate, required, default: today)
- `invoiceUrl` (String, length 500)
- `notes` (TEXT)
- `createdAt` (LocalDateTime, auto)

### SaaSService (Catalog)
**Fields:**
- `id` (UUID, PK, generated)
- `name` (String, required)
- `description` (TEXT)
- `category` (String, length 100)
- `logoUrl` (String, length 500)
- `websiteUrl` (String, length 500)
- `defaultPrice` (BigDecimal, precision 10, scale 2)
- `defaultCurrency` (String, length 3)
- `defaultFrequency` (Enum: WEEKLY, MONTHLY, QUARTERLY, ANNUAL)
- `isPopular` (Boolean, default: false)
- `createdAt` (LocalDateTime, auto)
- `updatedAt` (LocalDateTime, auto)

### Subscription
**Fields:**
- `id` (UUID, PK, generated)
- `client` (ManyToOne → Client, required)
- `category` (ManyToOne → Category, nullable)
- `name` (String, required, max 255)
- `description` (TEXT)
- `price` (BigDecimal, precision 10, scale 2, required)
- `originalCurrency` (String, length 3, default: USD)
- `frequency` (Enum: WEEKLY, MONTHLY, QUARTERLY, ANNUAL, default: MONTHLY)
- `status` (Enum: ACTIVE, PAUSED, CANCELLED, default: ACTIVE)
- `startDate` (LocalDate, required, auto-default: today)
- `nextBillingDate` (LocalDate, auto-calculated)
- `logoUrl` (String, length 500)
- `cancelLink` (String, length 500)
- `notes` (TEXT)
- `createdAt` (LocalDateTime, auto)
- `updatedAt` (LocalDateTime, auto)
- `paymentHistory` (OneToMany → PaymentHistory, cascade)
- `alertRules` (OneToMany → AlertRule, cascade)
- **Methods:**
  - `calculateNextBillingDate()` - Computes next billing based on frequency
  - `getMonthlyCost()` - Normalizes price to monthly (uses frequency multiplier)
  - `getAnnualCost()` - Normalizes price to annual (uses frequency multiplier)

---

## 4. DATA ACCESS OBJECTS (DAOs)

### AlertRuleDAO / AlertRuleDAOImpl
**Interface Methods:**
- `create(AlertRule)`, `update(AlertRule)`, `delete(AlertRule)`
- `findById(UUID)`, `findAll()`
- `findBySubscriptionId(UUID)`
- `findActiveAlertRules()`
- `findAlertsDueSoon(int days)` - Uses DATEDIFF function

### CategoryDAO / CategoryDAOImpl
**Interface Methods:**
- `create(Category)`, `update(Category)`, `delete(Category)`
- `findById(UUID)`, `findByName(String)`
- `findAll()`, `findAllOrderByName()`
- `existsByName(String)`

### ClientDAO / ClientDAOImpl
**Interface Methods:**
- `create(Client)`, `update(Client)`, `delete(Client)`
- `findById(UUID)`, `findByEmail(String)`
- `findAll()`, `findByRole(Role)`
- `findActiveClients()`
- `existsByEmail(String)`

### EmailIntegrationDAO / EmailIntegrationDAOImpl
**Interface Methods:**
- `create(EmailIntegration)`, `update(EmailIntegration)`, `delete(EmailIntegration)`
- `findById(UUID)`, `findByClientId(UUID)`
- `findAll()`, `findActiveIntegrations()`

### ExchangeRateDAO / ExchangeRateDAOImpl
**Interface Methods:**
- `create(ExchangeRate)`, `update(ExchangeRate)`, `delete(ExchangeRate)`
- `findById(UUID)`, `findAll()`
- `findByCurrencies(fromCurrency, toCurrency)`
- `findByFromCurrency(String)`

### InvoiceDAO / InvoiceDAOImpl
**Interface Methods:**
- `create(Invoice)`, `update(Invoice)`, `delete(Invoice)`
- `findById(UUID)`, `findAll()`
- `findByClientId(UUID)`
- `findUnprocessedInvoices()`
- `findByDateRange(LocalDate, LocalDate)`

### PaymentHistoryDAO / PaymentHistoryDAOImpl
**Interface Methods:**
- `create(PaymentHistory)`, `update(PaymentHistory)`, `delete(PaymentHistory)`
- `findById(UUID)`, `findAll()`
- `findBySubscriptionId(UUID)`
- `findByDateRange(LocalDate, LocalDate)`
- `findBySubscriptionIdAndDateRange(UUID, LocalDate, LocalDate)`

### SaaSServiceDAO / SaaSServiceDAOImpl
**Interface Methods:**
- `create(SaaSService)`, `update(SaaSService)`, `delete(SaaSService)`
- `findById(UUID)`, `findAll()`
- `findByNameContaining(String)` - Case-insensitive LIKE search
- `findByServiceName(String)` - Exact name match

### SubscriptionDAO / SubscriptionDAOImpl
**Interface Methods:**
- `create(Subscription)`, `update(Subscription)`, `delete(Subscription)`
- `findById(UUID)`, `findAll()`
- `findByClientId(UUID)`
- `findByClientIdAndStatus(UUID, SubscriptionStatus)`
- `findByClientIdAndCategoryId(UUID, UUID)`
- `findByNameContaining(String)` - Case-insensitive LIKE search

---

## 5. ENUMS

- **AccountType:** B2C, B2B
- **AlertChannel:** EMAIL, TELEGRAM, WHATSAPP
- **Frequency:** WEEKLY, MONTHLY, QUARTERLY, ANNUAL
  - Has `getMonthlyMultiplier()` and `getAnnualMultiplier()` methods
- **Role:** CLIENT, ADMIN
- **SubscriptionStatus:** ACTIVE, PAUSED, CANCELLED

---

## 6. OUTSTANDING ISSUES & TODOs

### Empty/Incomplete Methods:

1. **AlertService**
   - `sendEmailNotification(AlertRule)` - Empty, needs implementation
   - `sendTelegramNotification(AlertRule)` - Empty, needs implementation
   - `sendWhatsAppNotification(AlertRule)` - Empty, needs implementation

2. **InvoiceService**
   - `parseInvoiceEmail(Invoice)` - Empty, needs email parsing logic

3. **EmailIntegrationService**
   - Uses mock tokens ("mock_access_token", "mock_refresh_token")
   - Real OAuth integration needed for Gmail/Outlook

4. **ExchangeRateService**
   - `getMonthlyTrend()` - Uses mock/fake data with 0.9x and 0.85x multipliers
   - Real payment history query should be implemented
   - Comment: "For a real app, this would query PaymentHistory for that month"

5. **InvoiceScheduler**
   - Exception handling only logs to stderr
   - No persistent error tracking or retry logic

### Missing Implementations:

1. **Notification System:**
   - Email notifications not implemented
   - Telegram bot integration missing
   - WhatsApp API integration missing

2. **Email Integration:**
   - OAuth flow not implemented (uses mock tokens)
   - Email parsing from invoices incomplete
   - No actual email invoice retrieval

3. **Analytics:**
   - Monthly trend data is mocked
   - Real payment history analysis needed

4. **Invoice Processing:**
   - Email parsing logic missing
   - Service name extraction from emails incomplete
   - No OCR or document parsing

### Potential Issues:

1. **AlertRuleDAOImpl.findAlertsDueSoon()** - Uses `DATEDIFF` function which is database-specific (MySQL); may fail on PostgreSQL
   - Should use portable JPA FUNCTION or better approach

2. **Frequency multiplier logic** - Used in `getMonthlyCost()` and `getAnnualCost()` but Frequency enum implementation not shown
   - Depends on WEEKLY=4.33, MONTHLY=1, QUARTERLY=0.33, ANNUAL=0.083 (or similar)

3. **Session management** - Uses JSF session API; ensure proper handling in distributed environments

---

## 7. TECHNOLOGY STACK SUMMARY

- **Framework:** Jakarta EE 10, JSF 4.0
- **ORM:** Hibernate 6.4.4.Final
- **UI:** PrimeFaces 14.0.4 (Jakarta)
- **Database:** PostgreSQL 42.7.1
- **Security:** BCrypt (jbcrypt 0.4), Commons Codec
- **Testing:** JUnit 5, Mockito 5.8.0
- **Build:** Maven 3.12.1
- **Java Version:** 17 (Java Module System compatible)

---

## 8. DATABASE STRUCTURE

Key relationships:
- **Client** → Subscriptions (OneToMany)
- **Client** → EmailIntegration (OneToOne)
- **Client** → Invoices (OneToMany)
- **Subscription** → Category (ManyToOne)
- **Subscription** → PaymentHistory (OneToMany)
- **Subscription** → AlertRules (OneToMany)
- **ExchangeRate** - Standalone (currency conversion lookup table)
- **SaaSService** - Standalone (service catalog)

Cascade deletes configured for Client relationships to maintain referential integrity.

---

**Generated:** Based on full source code analysis
**Last Updated:** 2025 (Current session)
