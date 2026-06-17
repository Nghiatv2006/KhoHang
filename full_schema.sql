-- ==============================================================================
-- SƠ ĐỒ CƠ SỞ DỮ LIỆU TOÀN DIỆN (FULL CONSOLIDATED SCHEMA)
-- (Không chứa lệnh INSERT - Dùng để xem cấu trúc hoặc dán vào dbdiagram.io)
-- ==============================================================================

-- 1. ENUM TYPES
CREATE TYPE user_role AS ENUM ('ADMIN', 'MANAGER', 'STAFF');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'LOCKED');
CREATE TYPE receipt_type AS ENUM ('IMPORT', 'EXPORT', 'TRANSFER', 'ADJUST_IN', 'ADJUST_OUT');
CREATE TYPE receipt_status AS ENUM ('DRAFT', 'COMPLETED', 'CANCELLED');
CREATE TYPE stocktake_status AS ENUM ('DRAFT', 'COMPLETED', 'CANCELLED');

-- Bảng Chi nhánh (Branches)
-- Lưu ý quy ước vận hành: Chi nhánh có id = 1 mặc định được xem là Kho Tổng (Main Branch).
CREATE TABLE branches (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    address TEXT NOT NULL,
    low_stock_threshold INT NOT NULL DEFAULT 5
);

-- Bảng Danh mục sản phẩm (Categories)
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Bảng Khách hàng (Customers)
CREATE TABLE customers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_info VARCHAR(255),
    address TEXT,
    debt NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Người dùng (Users)
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    role user_role NOT NULL,
    branch_id INT,
    status user_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE RESTRICT,
    CONSTRAINT chk_user_branch CHECK (role = 'ADMIN' OR branch_id IS NOT NULL)
);

-- Bảng Sản phẩm (Products)
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE CHECK (code = UPPER(code)),
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    price NUMERIC(15, 2) NOT NULL CHECK (price >= 0),
    category_id INT NOT NULL,
    has_expiry BOOLEAN NOT NULL DEFAULT FALSE,
    mfg_date DATE DEFAULT '1970-01-01',
    exp_date DATE DEFAULT '1970-01-01',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT chk_product_dates CHECK (exp_date >= mfg_date)
);

-- Bảng Tồn kho (Inventories)
CREATE TABLE inventories (
    id SERIAL PRIMARY KEY,
    branch_id INT NOT NULL,
    product_id INT NOT NULL,
    mfg_date DATE NOT NULL DEFAULT '1970-01-01',
    exp_date DATE NOT NULL DEFAULT '1970-01-01',
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_inventory_batch UNIQUE (branch_id, product_id, mfg_date, exp_date),
    CONSTRAINT fk_inventory_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT chk_inventory_dates CHECK (exp_date >= mfg_date)
);

-- Bảng Phiếu kho (Receipts)
CREATE TABLE receipts (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    type receipt_type NOT NULL,
    status receipt_status NOT NULL DEFAULT 'COMPLETED',
    payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID', -- Trạng thái thanh toán: UNPAID, PAID
    source_branch_id INT,
    dest_branch_id INT,
    created_by INT NOT NULL,
    customer_id INT,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_receipt_source_branch FOREIGN KEY (source_branch_id) REFERENCES branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_dest_branch FOREIGN KEY (dest_branch_id) REFERENCES branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT
);

-- Bảng Chi tiết phiếu kho (Receipt Details)
CREATE TABLE receipt_details (
    id SERIAL PRIMARY KEY,
    receipt_id INT NOT NULL,
    product_id INT NOT NULL,
    mfg_date DATE NOT NULL DEFAULT '1970-01-01',
    exp_date DATE NOT NULL DEFAULT '1970-01-01',
    quantity INT NOT NULL CHECK (quantity > 0),
    price NUMERIC(15, 2) NOT NULL CHECK (price >= 0),
    CONSTRAINT fk_detail_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id) ON DELETE CASCADE,
    CONSTRAINT fk_detail_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT chk_receipt_detail_dates CHECK (exp_date >= mfg_date)
);

-- Bảng Phiên kiểm kê (Stocktakes)
CREATE TABLE stocktakes (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    branch_id INT NOT NULL,
    created_by INT NOT NULL,
    status stocktake_status NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stocktake_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_stocktake_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT
);

-- Bảng Chi tiết kiểm kê (Stocktake Details)
CREATE TABLE stocktake_details (
    id SERIAL PRIMARY KEY,
    stocktake_id INT NOT NULL,
    product_id INT NOT NULL,
    mfg_date DATE NOT NULL DEFAULT '1970-01-01',
    exp_date DATE NOT NULL DEFAULT '1970-01-01',
    expected_quantity INT NOT NULL CHECK (expected_quantity >= 0),
    actual_quantity INT NOT NULL CHECK (actual_quantity >= 0),
    adjustment_receipt_id INT,
    CONSTRAINT fk_stocktake_detail FOREIGN KEY (stocktake_id) REFERENCES stocktakes(id) ON DELETE CASCADE,
    CONSTRAINT fk_stocktake_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_stocktake_receipt FOREIGN KEY (adjustment_receipt_id) REFERENCES receipts(id) ON DELETE SET NULL,
    CONSTRAINT chk_stocktake_detail_dates CHECK (exp_date >= mfg_date)
);

-- Bảng Nhật ký hoạt động (Audit Logs)
CREATE TABLE audit_logs (
    id SERIAL PRIMARY KEY,
    user_id INT,
    action VARCHAR(50) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(50),
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_products_code_name ON products (code, name);
CREATE INDEX idx_inventories_branch_product ON inventories (branch_id, product_id);
CREATE INDEX idx_inventories_last_updated ON inventories (last_updated DESC);
CREATE INDEX idx_receipts_created_at ON receipts (created_at DESC);
CREATE INDEX idx_receipts_branches ON receipts (source_branch_id, dest_branch_id);
CREATE INDEX idx_receipts_type_status ON receipts (type, status);
CREATE INDEX idx_stocktakes_branch ON stocktakes (branch_id);
CREATE INDEX idx_customers_status ON customers (status);
CREATE INDEX idx_receipts_payment_status ON receipts (payment_status);
