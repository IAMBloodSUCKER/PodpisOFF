create table subscription_month_charges (
    id bigserial primary key,
    subscription_id bigint not null references subscriptions (id) on delete cascade,
    charge_year int not null,
    charge_month int not null,
    amount numeric(12, 2) not null,
    note varchar(500),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_subscription_month_charge unique (subscription_id, charge_year, charge_month)
);

create index idx_subscription_month_charges_sub on subscription_month_charges(subscription_id);
