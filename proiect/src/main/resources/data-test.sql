INSERT INTO categories (category_id, category_name) VALUES (1, 'Food');
INSERT INTO categories (category_id, category_name) VALUES (2, 'Utilities');

INSERT INTO users (user_id, first_name, last_name, email, password) VALUES (1, 'Alex', 'Popescu', 'alex@test.com', 'pass'); -- de modificat pass cu hash
INSERT INTO users (user_id, first_name, last_name, email, password) VALUES (2, 'Maria', 'Ionescu', 'maria@test.com', 'pass'); -- de modificat pass cu hash

INSERT INTO individuals (individual_id, users_user_id, date_of_birth, cnp, first_name, last_name, phone_number) VALUES (1, 1, '1995-05-10', '1234567890123', 'Alex', 'Popescu', '0711111111');
INSERT INTO individuals (individual_id, users_user_id, date_of_birth, cnp, first_name, last_name, phone_number) VALUES (2, 2, '1998-08-20', '9876543210987', 'Maria', 'Ionescu', '0722222222');

INSERT INTO accounts (account_id, iban, type, currency, balance, alias) VALUES (1, 'RO001', 'CURRENT', 'RON', 1000, 'Alex Account');
INSERT INTO accounts (account_id, iban, type, currency, balance, alias) VALUES (2, 'RO002', 'CURRENT', 'RON', 2000, 'Maria Account');
INSERT INTO accounts (account_id, iban, type, currency, balance, alias) VALUES (3, 'RO003', 'JOINT', 'RON', 3000, 'Joint Account');

INSERT INTO account_user (user_id, account_id) VALUES (1, 1);
INSERT INTO account_user (user_id, account_id) VALUES (2, 2);
INSERT INTO account_user (user_id, account_id) VALUES (1, 3);
INSERT INTO account_user (user_id, account_id) VALUES (2, 3);

INSERT INTO `transaction` (transaction_id, amount, type, status, description, iban_from, iban_to, account_id, user_id, category_id) VALUES (1, 100, 'DEBIT', 'DONE', 'Groceries', 'RO001', 'RO999', 1, 1, 1);
INSERT INTO `transaction` (transaction_id, amount, type, status, description, iban_from, iban_to, account_id, user_id, category_id) VALUES (2, 200, 'DEBIT', 'DONE', 'Electricity', 'RO002', 'RO888', 2, 2, 2);
INSERT INTO `transaction` (transaction_id, amount, type, status, description, iban_from, iban_to, account_id, user_id, category_id) VALUES (3, 300, 'DEBIT', 'DONE', 'Shared Expense', 'RO003', 'RO777', 3, 1, 1);