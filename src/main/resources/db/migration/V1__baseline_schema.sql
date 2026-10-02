-- Baseline schema, matching what hibernate.hbm2ddl.auto=update created before migrations were introduced.
-- Databases that already have these tables are baselined at version 1 and skip this script.

create table alert_rule (
    id uuid not null,
    channel varchar(255) not null check (channel in ('EMAIL','TELEGRAM','WHATSAPP')),
    created_at timestamp(6) not null,
    is_active boolean not null,
    timing_days integer not null,
    updated_at timestamp(6) not null,
    subscription_id uuid not null,
    primary key (id)
);

create table category (
    id uuid not null,
    color varchar(7),
    created_at timestamp(6) not null,
    description TEXT,
    icon varchar(100),
    name varchar(100) not null unique,
    primary key (id)
);

create table client (
    id uuid not null,
    account_type varchar(255) not null check (account_type in ('B2C','FREELANCE','B2B')),
    created_at timestamp(6) not null,
    email varchar(255) not null unique,
    email_notifications boolean,
    first_name varchar(100),
    is_active boolean not null,
    last_name varchar(100),
    password varchar(255) not null,
    role varchar(255) not null check (role in ('CLIENT','ADMIN')),
    telegram_chat_id varchar(50),
    telegram_enabled boolean,
    timezone varchar(50),
    updated_at timestamp(6) not null,
    whatsapp_enabled boolean,
    whatsapp_number varchar(20),
    primary key (id)
);

create table email_integration (
    id uuid not null,
    access_token TEXT,
    created_at timestamp(6) not null,
    email_address varchar(255) not null,
    is_active boolean not null,
    refresh_token TEXT,
    token_expires_at timestamp(6),
    updated_at timestamp(6) not null,
    client_id uuid not null unique,
    primary key (id)
);

create table exchange_rate (
    id uuid not null,
    from_currency varchar(3) not null,
    rate numeric(15,6) not null,
    to_currency varchar(3) not null,
    updated_at timestamp(6) not null,
    primary key (id)
);

create table invoice (
    id uuid not null,
    amount numeric(10,2),
    created_at timestamp(6) not null,
    currency varchar(3),
    invoice_date date,
    is_processed boolean not null,
    raw_content TEXT,
    service_name varchar(255),
    client_id uuid not null,
    primary key (id)
);

create table notification_log (
    id uuid not null,
    channel varchar(255) not null check (channel in ('EMAIL','TELEGRAM','WHATSAPP')),
    client_email varchar(255),
    delivery_status varchar(255) not null,
    error_message varchar(255),
    recipient_identifier varchar(255),
    sent_at timestamp(6) not null,
    subscription_id uuid,
    primary key (id)
);

create table payment_history (
    id uuid not null,
    amount numeric(10,2) not null,
    created_at timestamp(6) not null,
    currency varchar(3) not null,
    invoice_url varchar(500),
    notes TEXT,
    payment_date date not null,
    subscription_id uuid not null,
    primary key (id)
);

create table saas_service (
    id uuid not null,
    category varchar(100),
    created_at timestamp(6) not null,
    default_currency varchar(3),
    default_frequency varchar(255) check (default_frequency in ('WEEKLY','MONTHLY','QUARTERLY','ANNUAL')),
    default_price numeric(10,2),
    description TEXT,
    is_popular boolean,
    logo_url varchar(500),
    name varchar(255) not null,
    updated_at timestamp(6) not null,
    website_url varchar(500),
    primary key (id)
);

create table subscription (
    id uuid not null,
    cancel_link varchar(500),
    created_at timestamp(6) not null,
    description TEXT,
    frequency varchar(255) not null check (frequency in ('WEEKLY','MONTHLY','QUARTERLY','ANNUAL')),
    logo_url varchar(500),
    name varchar(255) not null,
    next_billing_date date,
    notes TEXT,
    original_currency varchar(3) not null,
    price numeric(10,2) not null,
    start_date date not null,
    status varchar(255) not null check (status in ('ACTIVE','PAUSED','CANCELLED')),
    updated_at timestamp(6) not null,
    category_id uuid,
    client_id uuid not null,
    primary key (id)
);

create table system_config (
    config_key varchar(100) not null,
    config_value varchar(1000),
    updated_at timestamp(6),
    primary key (config_key)
);

alter table if exists alert_rule 
   add constraint FK3cam1c3xaj0qa17as6x1wk99v 
   foreign key (subscription_id) 
   references subscription;

alter table if exists email_integration 
   add constraint FK8lwwshlat6rwbf2yqxkac9nc9 
   foreign key (client_id) 
   references client;

alter table if exists invoice 
   add constraint FK6y01j0975eqwmnb0gckttrbj2 
   foreign key (client_id) 
   references client;

alter table if exists notification_log 
   add constraint FKkoxsa11gcgrlhovhxg3humjto 
   foreign key (subscription_id) 
   references subscription;

alter table if exists payment_history 
   add constraint FK61846vqoaitb3b78lw3g7h3uo 
   foreign key (subscription_id) 
   references subscription;

alter table if exists subscription 
   add constraint FKnj55m79pro2qanuayd9ckn3st 
   foreign key (category_id) 
   references category;

alter table if exists subscription 
   add constraint FKq1ax3xfgnv5s0xay1y80fdyca 
   foreign key (client_id) 
   references client;
