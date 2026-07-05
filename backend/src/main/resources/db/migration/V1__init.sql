create table users (
    id bigserial primary key,
    username varchar(50) not null unique,
    password_hash varchar(255) not null,
    recovery_key_hash varchar(255) not null,
    email varchar(255),
    plan varchar(10) not null,
    plan_expires_at timestamp,
    timezone varchar(64) not null,
    locale varchar(5) not null,
    terms_accepted boolean not null,
    created_at timestamp with time zone not null
);

create table subscriptions (
    id bigserial primary key,
    user_id bigint not null references users (id) on delete cascade,
    title varchar(120) not null,
    category varchar(80) not null,
    amount numeric(12, 2) not null,
    currency varchar(8) not null,
    next_billing_date date not null,
    active boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_subscriptions_user_id on subscriptions(user_id);
create index idx_subscriptions_next_billing_date on subscriptions(next_billing_date);

create table reminders (
    id bigserial primary key,
    user_id bigint not null references users (id) on delete cascade,
    title varchar(150) not null,
    note varchar(1000),
    remind_at timestamp not null,
    done boolean not null default false,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_reminders_user_id on reminders(user_id);
