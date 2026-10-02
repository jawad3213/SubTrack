# Data Model: SubTrack

## Entity Relationship Diagram

```
┌─────────────┐       ┌──────────────────┐       ┌─────────────┐
│   CLIENT    │       │   SUBSCRIPTION   │       │  CATEGORY   │
├─────────────┤       ├──────────────────┤       ├─────────────┤
│ id (UUID)   │──1:N──│ id (UUID)        │       │ id (UUID)   │
│ email       │       │ client_id (FK)   │─N:1── │ name        │
│ password    │       │ category_id (FK) │       │ description │
│ firstName   │       │ name             │       │ icon        │
│ lastName    │       │ description      │       └─────────────┘
│ accountType │       │ price            │
│ createdAt   │       │ originalCurrency │
│ updatedAt   │       │ frequency        │
└─────────────┘       │ status           │
                      │ startDate        │
                      │ nextBillingDate  │
                      │ logoUrl          │
                      │ cancelLink       │
                      │ createdAt        │
                      │ updatedAt        │
                      └────────┬─────────┘
                               │
                               │ 1:N
                      ┌────────▼────────┐
                      │ PAYMENT_HISTORY │
                      ├─────────────────┤
                      │ id (UUID)       │
                      │ subscription_id │
                      │ amount          │
                      │ currency        │
                      │ paymentDate     │
                      │ invoiceUrl      │
                      └─────────────────┘

┌──────────────────┐       ┌─────────────┐
│  EMAIL_INTEGRATION│      │   ALERT_RULE│
├──────────────────┤       ├─────────────┤
│ id (UUID)        │       │ id (UUID)   │
│ client_id (FK)   │       │ subscription │
│ emailAddress     │       │ channel     │
│ accessToken      │       │ timingDays  │
│ refreshToken     │       │ isActive    │
│ expiresAt        │       │ createdAt   │
└──────────────────┘       └─────────────┘

┌─────────────┐       ┌──────────────┐       ┌───────────────┐
│   INVOICE  │       │EXCHANGE_RATE │       │  SAAS_SERVICE │
├─────────────┤       ├──────────────┤       ├───────────────┤
│ id (UUID)  │       │ id (UUID)    │       │ id (UUID)     │
│ client_id   │       │ fromCurrency │       │ name          │
│ rawContent │       │ toCurrency   │       │ category      │
│ serviceName│       │ rate         │       │ logoUrl       │
│ amount     │       │ updatedAt    │       │ websiteUrl    │
│ currency   │       └──────────────┘       │ defaultPrice  │
│ invoiceDate│                             │ defaultCurrency│
│ isProcessed│                             │ isPopular     │
└─────────────┘                             └───────────────┘
```

## Entities

### 1. Client (User)
Represents platform users (both regular clients and administrators).

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, NOT NULL | Unique identifier |
| email | VARCHAR(255) | NOT NULL, UNIQUE | User email address |
| password | VARCHAR(255) | NOT NULL | Hashed password |
| firstName | VARCHAR(100) | | User first name |
| lastName | VARCHAR(100) | | User last name |
| accountType | ENUM | NOT NULL | B2C, FREELANCE, B2B |
| role | ENUM | NOT NULL | CLIENT, ADMIN |
| timezone | VARCHAR(50) | | User timezone for alerts |
| isActive | BOOLEAN | NOT NULL, DEFAULT TRUE | Account status |
| createdAt | TIMESTAMP | NOT NULL | Creation timestamp |
| updatedAt | TIMESTAMP | NOT NULL | Last update timestamp |

### 2. Subscription
Core business entity for recurring payments.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, NOT NULL | Unique identifier |
| clientId | UUID | FK, NOT NULL | Owner client |
| categoryId | UUID | FK | Associated category |
| name | VARCHAR(255) | NOT NULL | Service name |
| description | TEXT | | Service description |
| price | DECIMAL(10,2) | NOT NULL | Price in original currency |
| originalCurrency | VARCHAR(3) | NOT NULL | ISO currency code |
| frequency | ENUM | NOT NULL | WEEKLY, MONTHLY, QUARTERLY, ANNUAL |
| status | ENUM | NOT NULL | ACTIVE, PAUSED, CANCELLED |
| startDate | DATE | NOT NULL | Subscription start date |
| nextBillingDate | DATE | | Next billing date |
| logoUrl | VARCHAR(500) | | Service logo URL |
| cancelLink | VARCHAR(500) | | Cancellation URL |
| notes | TEXT | | User notes |
| createdAt | TIMESTAMP | NOT NULL | Creation timestamp |
| updatedAt | TIMESTAMP | NOT NULL | Last update timestamp |

### 3. PaymentHistory
Tracks actual payments for price change detection.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, NOT NULL | Unique identifier |
| subscriptionId | UUID | FK, NOT NULL | Associated subscription |
| amount | DECIMAL(10,2) | NOT NULL | Amount paid |
| currency | VARCHAR(3) | NOT NULL | ISO currency code |
| paymentDate | DATE | NOT NULL | Date of payment |
| invoiceUrl | VARCHAR(500) | | Invoice URL |
| notes | TEXT | | Payment notes |
| createdAt | TIMESTAMP | NOT NULL | Creation timestamp |

### 4. Category
Groups subscriptions by theme.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, NOT NULL | Unique identifier |
| name | VARCHAR(100) | NOT NULL | Category name |
| description | TEXT | | Category description |
| icon | VARCHAR(100) | | Icon class/name |
| color | VARCHAR(7) | | Hex color code |
| createdAt | TIMESTAMP | NOT NULL | Creation timestamp |

### 5. AlertRule
Notification configuration for subscriptions.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, NOT NULL | Unique identifier |
| subscriptionId | UUID | FK, NOT NULL | Associated subscription |
| channel | ENUM | NOT NULL | EMAIL, TELEGRAM, WHATSAPP |
| timingDays | INTEGER | NOT NULL | Days before billing |
| isActive | BOOLEAN | NOT NULL, DEFAULT TRUE | Rule status |
| createdAt | TIMESTAMP | NOT NULL | Creation timestamp |
| updatedAt | TIMESTAMP | NOT NULL | Last update timestamp |

### 6. EmailIntegration
Manages OAuth tokens for email access.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, NOT NULL | Unique identifier |
| clientId | UUID | FK, NOT NULL | Owner client |
| emailAddress | VARCHAR(255) | NOT NULL | Email address |
| accessToken | TEXT | | OAuth access token |
| refreshToken | TEXT | | OAuth refresh token |
| tokenExpiresAt | TIMESTAMP | | Token expiration |
| isActive | BOOLEAN | NOT NULL, DEFAULT TRUE | Integration status |
| createdAt | TIMESTAMP | NOT NULL | Creation timestamp |
| updatedAt | TIMESTAMP | NOT NULL | Last update timestamp |

### 7. Invoice
AI-extracted invoice data.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, NOT NULL | Unique identifier |
| clientId | UUID | FK, NOT NULL | Owner client |
| rawContent | TEXT | | Raw email content |
| serviceName | VARCHAR(255) | | Extracted service name |
| amount | DECIMAL(10,2) | | Extracted amount |
| currency | VARCHAR(3) | | Extracted currency |
| invoiceDate | DATE | | Invoice date |
| isProcessed | BOOLEAN | NOT NULL, DEFAULT FALSE | AI processing status |
| createdAt | TIMESTAMP | NOT NULL | Creation timestamp |

### 8. ExchangeRate
Currency conversion rates.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, NOT NULL | Unique identifier |
| fromCurrency | VARCHAR(3) | NOT NULL | Source currency |
| toCurrency | VARCHAR(3) | NOT NULL | Target currency |
| rate | DECIMAL(15,6) | NOT NULL | Exchange rate |
| updatedAt | TIMESTAMP | NOT NULL | Last update timestamp |

### 9. SaaSService
Catalog of known SaaS services.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, NOT NULL | Unique identifier |
| name | VARCHAR(255) | NOT NULL | Service name |
| category | VARCHAR(100) | | Service category |
| logoUrl | VARCHAR(500) | | Service logo |
| websiteUrl | VARCHAR(500) | | Service website |
| defaultPrice | DECIMAL(10,2) | | Typical price |
| defaultCurrency | VARCHAR(3) | | Typical currency |
| defaultFrequency | ENUM | | Typical billing frequency |
| isPopular | BOOLEAN | DEFAULT FALSE | Popular service flag |
| createdAt | TIMESTAMP | NOT NULL | Creation timestamp |
| updatedAt | TIMESTAMP | NOT NULL | Last update timestamp |

## Relationships

| From | To | Type | Description |
|------|-----|------|-------------|
| Client | Subscription | 1:N | One user can have many subscriptions |
| Client | AlertRule | 1:N | One user can have many alert rules |
| Client | EmailIntegration | 1:1 | One user can have one email integration |
| Client | Invoice | 1:N | One user can have many invoices |
| Subscription | Category | N:1 | Many subscriptions belong to one category |
| Subscription | PaymentHistory | 1:N | One subscription has many payment records |
| Subscription | AlertRule | 1:N | One subscription can have many alerts |

## Validation Rules

- **Email**: Valid email format, unique in system
- **Password**: Minimum 8 characters
- **Subscription Price**: Must be positive
- **Alert Timing**: Must be positive integer (days before)
- **Currency**: ISO 4217 codes (3 letters)
- **Frequency**: Must be valid enum value

## State Transitions

### Subscription Status
```
ACTIVE → PAUSED (user action)
ACTIVE → CANCELLED (user action)
PAUSED → ACTIVE (user action)
PAUSED → CANCELLED (user action)
CANCELLED → (terminal state)
```

### Alert Rule
```
ACTIVE → INACTIVE (user action)
INACTIVE → ACTIVE (user action)
```
