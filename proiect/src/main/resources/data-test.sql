-- Optional H2 demonstration data. Enabled only with --spring.sql.init.mode=always.
INSERT INTO bank_limits (max_amount_per_transaction_ron, max_daily_amount_ron,
    max_daily_transactions_count, status, created_at, updated_at)
VALUES (5000, 20000, 100, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Local test account only: admin@test.com / TestAdmin123!
INSERT INTO users (username, email, password_hash, role, failed_login_attempts,
    status, created_at, updated_at)
VALUES ('test_admin', 'admin@test.com', '$2a$10$1J4LE6ARjq7xWc4Wz.xwx.fElc97JtOddnWAETvLuaFmqwhr5RuCy',
    'ADMIN', 0, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Test user profile: teodorasumurduc@gmail.com / Password123!
INSERT INTO individuals (first_name, last_name, cnp, phone_number, date_of_birth, status, created_at, updated_at)
VALUES ('Teodora', 'Sumurduc', '2990101123456', '0712345678', '1999-01-01', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, email, password_hash, role, failed_login_attempts, status, individual_id, created_at, updated_at)
VALUES ('teodora', 'teodorasumurduc@gmail.com', '$2a$10$od1OGlNbqvt2JeqcdNXVj.PPNOLhMCyjbrjer.al.iPRUqWh9HRQa',
    'USER', 0, 'ACTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO user_limits (user_id, max_amount_per_transaction_ron, max_daily_amount_ron, max_daily_transactions_count, status, created_at, updated_at)
VALUES (2, 5000, 20000, 100, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
