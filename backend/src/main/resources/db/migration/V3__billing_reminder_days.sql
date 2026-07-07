alter table users
    add column billing_reminder_days_before integer not null default 3;
