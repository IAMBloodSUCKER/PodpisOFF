alter table subscriptions
    add column billing_period varchar(16) not null default 'MONTHLY';
