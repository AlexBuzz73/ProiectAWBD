insert into individuals (individual_id, cnp, created_at, date_of_birth, first_name, last_name, phone_number, status, updated_at) values
(1, '1900101123456', current_timestamp, '1990-01-01', 'Demo', 'User', '0711111111', 'ACTIVE', current_timestamp),
(2, '1800202123456', current_timestamp, '1980-02-02', 'Demo', 'Admin', '0722222222', 'ACTIVE', current_timestamp);

insert into users (user_id, created_at, email, failed_login_attempts, password_hash, role, status, updated_at, username, individual_id) values
(1, current_timestamp, 'user@test.com', 0, '$2a$10$.9CuuT/j9w/.Z.sqlcez4OQmvxFiaOtLbYEfgTGTf66ErMkmQYA/m', 'USER', 'ACTIVE', current_timestamp, 'demo_user', 1),
(2, current_timestamp, 'admin@test.com', 0, '$2a$10$.9CuuT/j9w/.Z.sqlcez4OQmvxFiaOtLbYEfgTGTf66ErMkmQYA/m', 'ADMIN', 'ACTIVE', current_timestamp, 'demo_admin', 2);

insert into accounts (account_id, alias, balance, created_at, currency, iban, status, updated_at) values
(1, 'Main RON Account', 2500.00, current_timestamp, 'RON', 'RO11BANK0000000000000001', 'ACTIVE', current_timestamp);

insert into account_access (account_access_id, access_role, created_at, status, updated_at, account_id, user_id) values
(1, 'OWNER', current_timestamp, 'ACTIVE', current_timestamp, 1, 1);

insert into categories (category_id, created_at, is_system, name, status, updated_at, created_by_user_id) values
(1, current_timestamp, 'Y', 'Food', 'ACTIVE', current_timestamp, null),
(2, current_timestamp, 'Y', 'Utilities', 'ACTIVE', current_timestamp, null),
(3, current_timestamp, 'Y', 'Transport', 'ACTIVE', current_timestamp, null),
(4, current_timestamp, 'Y', 'Shopping', 'ACTIVE', current_timestamp, null),
(5, current_timestamp, 'Y', 'Salary', 'ACTIVE', current_timestamp, null),
(6, current_timestamp, 'N', 'Gym', 'ACTIVE', current_timestamp, 1),
(7, current_timestamp, 'N', 'Subscriptions', 'ACTIVE', current_timestamp, 1),
(8, current_timestamp, 'N', 'Savings', 'ACTIVE', current_timestamp, 1);

insert into transactions (transaction_id, amount, created_at, currency, description, destination_iban, is_scheduled, is_urgent, status, transaction_type, updated_at, category_id, destination_account_id, exchange_rate_id, initiated_by_user_id, source_account_id) values
(1, 120.50, current_timestamp - interval 5 day, 'RON', 'Groceries payment', 'RO99EXT0000000000000001', 'NO', 'NO', 'EXECUTED', 'EXTERNAL', current_timestamp - interval 5 day, 1, null, null, 1, 1),
(2, 300.00, current_timestamp - interval 4 day, 'RON', 'Electricity bill', 'RO99EXT0000000000000002', 'NO', 'NO', 'EXECUTED', 'EXTERNAL', current_timestamp - interval 4 day, 2, null, null, 1, 1),
(3, 45.75, current_timestamp - interval 3 day, 'RON', 'Transport card', 'RO99EXT0000000000000003', 'NO', 'NO', 'EXECUTED', 'EXTERNAL', current_timestamp - interval 3 day, 3, null, null, 1, 1),
(4, 250.00, current_timestamp - interval 2 day, 'RON', 'Gym membership', 'RO99EXT0000000000000004', 'NO', 'NO', 'EXECUTED', 'EXTERNAL', current_timestamp - interval 2 day, 6, null, null, 1, 1),
(5, 80.00, current_timestamp - interval 1 day, 'RON', 'Online subscription', 'RO99EXT0000000000000005', 'NO', 'NO', 'EXECUTED', 'EXTERNAL', current_timestamp - interval 1 day, 7, null, null, 1, 1);

insert into bank_limits (bank_limit_id, created_at, max_amount_per_transaction_ron, max_daily_amount_ron, max_daily_transactions_count, status, updated_at) values
(1, current_timestamp, 5000.00, 20000.00, 10.00, 'ACTIVE', current_timestamp);