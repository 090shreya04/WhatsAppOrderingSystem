-- V1 Initial Schema — Dine-In QR Ordering + WhatsApp Pre-Order System
-- Matches TRD section 3 + Backend Schema doc, with additions:
--   • orders.version column (optimistic locking via @Version)

-- ─── Users (restaurant owners) ────────────────────────────────────
CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    email        VARCHAR(150)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role         VARCHAR(20)   NOT NULL DEFAULT 'OWNER',
    created_at   TIMESTAMP     NOT NULL DEFAULT now()
);

-- ─── Restaurants ──────────────────────────────────────────────────
CREATE TABLE restaurants (
    id               BIGSERIAL PRIMARY KEY,
    owner_id         BIGINT        NOT NULL REFERENCES users(id),
    name             VARCHAR(150)  NOT NULL,
    address          VARCHAR(255),
    whatsapp_number  VARCHAR(20),
    qr_secret        VARCHAR(64)   NOT NULL UNIQUE,   -- used in public URLs to prevent enumeration
    is_active        BOOLEAN       NOT NULL DEFAULT true,
    created_at       TIMESTAMP     NOT NULL DEFAULT now()
);

-- ─── Tables ────────────────────────────────────────────────────────
CREATE TABLE tables (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT      NOT NULL REFERENCES restaurants(id),
    table_number    VARCHAR(10) NOT NULL,
    qr_code_url     VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'FREE',   -- FREE, OCCUPIED
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE(restaurant_id, table_number)
);

-- ─── Menu Categories ───────────────────────────────────────────────
CREATE TABLE categories (
    id            BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT      NOT NULL REFERENCES restaurants(id),
    name          VARCHAR(100) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0
);

-- ─── Menu Items ────────────────────────────────────────────────────
CREATE TABLE menu_items (
    id            BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT         NOT NULL REFERENCES restaurants(id),
    category_id   BIGINT         REFERENCES categories(id),
    name          VARCHAR(150)   NOT NULL,
    description   VARCHAR(500),
    price         NUMERIC(8,2)   NOT NULL CHECK (price >= 0),
    image_url     VARCHAR(255),
    is_available  BOOLEAN        NOT NULL DEFAULT true,
    created_at    TIMESTAMP      NOT NULL DEFAULT now()
);

-- ─── Orders ────────────────────────────────────────────────────────
CREATE TABLE orders (
    id              BIGSERIAL    PRIMARY KEY,
    restaurant_id   BIGINT       NOT NULL REFERENCES restaurants(id),
    table_id        BIGINT       REFERENCES tables(id),          -- NULL for WhatsApp orders
    customer_phone  VARCHAR(20),                                  -- NULL for dine-in
    channel         VARCHAR(20)  NOT NULL,                        -- DINE_IN, WHATSAPP
    status          VARCHAR(20)  NOT NULL DEFAULT 'PLACED',       -- PLACED, CONFIRMED, PREPARING, READY, SERVED, CANCELLED
    total_amount    NUMERIC(10,2) NOT NULL DEFAULT 0,
    version         BIGINT       NOT NULL DEFAULT 0,              -- optimistic locking
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

-- ─── Order Items (line items with price snapshot) ─────────────────
CREATE TABLE order_items (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id    BIGINT       NOT NULL REFERENCES menu_items(id),
    quantity        INT          NOT NULL CHECK (quantity > 0),
    price_at_order  NUMERIC(8,2) NOT NULL   -- snapshot — protected from later price changes
);

-- ─── WhatsApp Sessions (conversation state per customer per restaurant) ──
CREATE TABLE whatsapp_sessions (
    id                BIGSERIAL PRIMARY KEY,
    restaurant_id     BIGINT      NOT NULL REFERENCES restaurants(id),
    customer_phone    VARCHAR(20) NOT NULL,
    state             VARCHAR(30) NOT NULL DEFAULT 'AWAITING_ITEMS',  -- AWAITING_ITEMS, CONFIRMING, DONE
    pending_order_json TEXT,                                           -- temp cart / menu map
    last_message_at   TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE(restaurant_id, customer_phone)
);

-- ─── WhatsApp Messages (audit log) ───────────────────────────────
CREATE TABLE whatsapp_messages (
    id                    BIGSERIAL    PRIMARY KEY,
    order_id              BIGINT       REFERENCES orders(id),
    restaurant_id         BIGINT       NOT NULL REFERENCES restaurants(id),
    customer_phone        VARCHAR(20)  NOT NULL,
    direction             VARCHAR(10)  NOT NULL,      -- IN, OUT
    message_body          TEXT,
    whatsapp_message_id   VARCHAR(100),               -- Meta's message ID
    status                VARCHAR(20),                -- sent, delivered, read, failed
    created_at            TIMESTAMP    NOT NULL DEFAULT now()
);

-- ─── Indexes ──────────────────────────────────────────────────────
CREATE INDEX idx_orders_restaurant_status  ON orders(restaurant_id, status);
CREATE INDEX idx_orders_channel            ON orders(restaurant_id, channel);
CREATE INDEX idx_menu_items_restaurant     ON menu_items(restaurant_id, is_available);
CREATE INDEX idx_whatsapp_sessions_phone   ON whatsapp_sessions(restaurant_id, customer_phone);
CREATE INDEX idx_whatsapp_messages_order   ON whatsapp_messages(order_id);
CREATE INDEX idx_restaurants_owner        ON restaurants(owner_id);
CREATE INDEX idx_tables_restaurant        ON tables(restaurant_id);
CREATE INDEX idx_categories_restaurant    ON categories(restaurant_id);
