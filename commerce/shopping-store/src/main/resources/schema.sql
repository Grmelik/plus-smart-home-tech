CREATE SCHEMA IF NOT EXISTS store;

CREATE TABLE IF NOT EXISTS store.products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    image_src VARCHAR(512),
    quantity_state VARCHAR(10) NOT NULL,
    product_state VARCHAR(10) NOT NULL,
    product_category VARCHAR(50),
    price DOUBLE PRECISION NOT NULL CHECK (price >= 1)
);

CREATE INDEX IF NOT EXISTS idx_product_category ON store.products(product_category)
WHERE product_category IS NOT NULL;