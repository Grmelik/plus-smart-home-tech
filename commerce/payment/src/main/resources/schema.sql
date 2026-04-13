CREATE SCHEMA IF NOT EXISTS payment;

CREATE TABLE IF NOT EXISTS payment.payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    total_payment DOUBLE PRECISION,
    delivery_total DOUBLE PRECISION,
    fee_total DOUBLE PRECISION,
    product_total DOUBLE PRECISION,
    payment_state VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payment.payments(order_id);