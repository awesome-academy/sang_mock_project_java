-- 1. Create Users Table
CREATE TABLE users (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(255),
    is_active BIT(1) DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB;

-- 2. Create Roles Table
CREATE TABLE roles (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    name VARCHAR(20),
    description VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB;

-- 3. Users_Roles (Many-to-Many)
CREATE TABLE users_roles (
    user_id BINARY(16) NOT NULL,
    role_id BINARY(16) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_users_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_users_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB;

-- 4. Categories
CREATE TABLE categories (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    icon VARCHAR(255),
    type VARCHAR(20) NOT NULL,
    is_deleted BIT(1) DEFAULT 0,
    user_id BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- 5. Expenses
CREATE TABLE expenses (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    title VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    expense_date DATE NOT NULL,
    note TEXT,
    user_id BINARY(16) NOT NULL,
    category_id BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB;

-- 6. Incomes
CREATE TABLE incomes (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    title VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    income_date DATE NOT NULL,
    note TEXT,
    user_id BINARY(16) NOT NULL,
    category_id BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT fk_incomes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_incomes_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB;

-- 7. Budgets
CREATE TABLE budgets (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    period VARCHAR(7) NOT NULL,
    user_id BINARY(16) NOT NULL,
    category_id BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB;

-- 8. Budget Templates
CREATE TABLE budget_templates (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    name VARCHAR(100) NOT NULL,
    period VARCHAR(7),
    default_amount DECIMAL(15,2),
    default_categories TEXT,
    created_by BIGINT,
    updated_by BIGINT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- 9. Attachments
CREATE TABLE attachments (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_type VARCHAR(50),
    expense_id BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT fk_attachments_expense FOREIGN KEY (expense_id) REFERENCES expenses (id)
) ENGINE=InnoDB;

-- 10. Activity Logs
CREATE TABLE activity_logs (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    action VARCHAR(20) NOT NULL,
    entity_type VARCHAR(20),
    entity_id BIGINT,
    description TEXT,
    user_id BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT fk_activity_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- 11. Import Export Logs
CREATE TABLE import_export_logs (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    action VARCHAR(20) NOT NULL,
    target_type VARCHAR(20),
    file_name VARCHAR(255),
    status VARCHAR(20),
    message TEXT,
    user_id BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT fk_impexp_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- 12. Refresh Tokens
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    user_id BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
