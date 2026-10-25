CREATE TABLE accounts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(160) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('member','admin') NOT NULL DEFAULT 'member'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE TABLE menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  category ENUM('Minuman','Makanan') NOT NULL,
  price DECIMAL(12,2) NOT NULL CHECK(price>0),
  image VARCHAR(80) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE TABLE demo_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  account_id BIGINT NOT NULL,
  table_number INT NOT NULL CHECK(table_number BETWEEN 1 AND 999),
  total DECIMAL(14,2) NOT NULL,
  checkout_token VARCHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(account_id,checkout_token),
  FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE TABLE demo_order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  quantity INT NOT NULL CHECK(quantity BETWEEN 1 AND 99),
  subtotal DECIMAL(14,2) NOT NULL,
  FOREIGN KEY(order_id) REFERENCES demo_orders(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Menu dan harga sintetis untuk demo, bukan daftar harga kedai yang diverifikasi.
INSERT INTO menu(name,category,price,image) VALUES
('Cappuccino','Minuman',25000,'cappuccino.png'),
('Americano','Minuman',22000,'americano.png'),
('Frappuccino','Minuman',30000,'frappuccino.png'),
('Flat White','Minuman',28000,'flatwhite.png'),
('Heart Coffee','Minuman',29000,'heartcoffee.png'),
('Croissant','Makanan',18000,'croissant.png');
