# 🍽️ WhatsApp & Dine-In QR Restaurant Ordering System

[![Live Frontend](https://img.shields.io/badge/Vercel-Live_Frontend-000000?style=for-the-badge&logo=vercel)](https://whats-app-ordering-system-eta.vercel.app)
[![Live Backend](https://img.shields.io/badge/Render-Live_Backend-46E3B7?style=for-the-badge&logo=render)](https://whatsapporderingsystem.onrender.com)
[![Database](https://img.shields.io/badge/Supabase-PostgreSQL-3ECF8E?style=for-the-badge&logo=supabase)](https://supabase.com)
[![WhatsApp API](https://img.shields.io/badge/Meta-WhatsApp_Cloud_API-25D366?style=for-the-badge&logo=whatsapp)](https://developers.facebook.com)

> **A Complete Production-Grade SaaS Platform** for restaurants featuring **Dine-In Table QR Code Ordering** and **Conversational WhatsApp Pre-Ordering**. Restaurant owners get a real-time sound-enabled dashboard to manage live orders, interactive menus, dining tables, and analytics.

---

## 🌐 Live Production Links & Test Credentials

- 🖥️ **Live Web Dashboard:** [https://whats-app-ordering-system-eta.vercel.app](https://whats-app-ordering-system-eta.vercel.app)
- ⚙️ **Live API Server:** `https://whatsapporderingsystem.onrender.com`
- 🤖 **Active WhatsApp Bot Number:** `+1 (555) 164-0709` (`15551640709`)
- 🔑 **Test Admin Credentials:**
  - **Email:** `admin@tasty.com`
  - **Password:** `admin123`

---

## 📋 Table of Contents

- [Features Breakdown](#-features-breakdown)
- [System Architecture](#-system-architecture)
- [Tech Stack](#-tech-stack)
- [WhatsApp Bot Flow](#-whatsapp-bot-flow)
- [API Reference](#-api-reference)
- [Security Architecture](#-security-architecture)
- [Local Setup & Deployment](#-local-setup--deployment)

---

## ✨ Features Breakdown

### 👨‍🍳 Restaurant Owner Dashboard
| Feature | Capabilities & Description |
|---|---|
| 🔐 **Authentication & Security** | JWT-based auth (`JwtAuthFilter`), BCrypt password hashing (strength 12), role-based protection |
| 🔔 **Live Order Queue & Alerts** | Real-time STOMP WebSocket push — new orders pop up instantly with sound alerts 🔔 |
| 🛑 **Cancel Order & Custom Reasons** | Cancel any order with custom reason (e.g., *"Item out of stock"*) or quick presets; WhatsApp sends the reason to customer! |
| 🍔 **Full Menu Management** | Full CRUD for Categories & Items! Inline Category rename/delete, Menu item CRUD, Cloudinary image upload, and **Available / Out of stock** toggle |
| 🪑 **Full Table Management** | Full CRUD for Tables! Add, edit/rename, and delete tables with instant PNG QR Code generation & browser download |
| 📊 **Analytics & Reports** | Revenue breakdown, order channel comparison (Dine-In QR vs WhatsApp), top-selling items, and peak hour traffic |
| ⚙️ **Settings & Multi-Tenant** | Dynamic restaurant profile, phone number binding, and multi-tenant isolation by `restaurant_id` |

### 📱 Customer (Dine-In QR Code Ordering)
| Feature | Description |
|---|---|
| 📷 **Table QR Scan** | Customer scans QR on table → lands on mobile SPA ordering page (`/order/<qrSecret>/<tableId>`) |
| 🛒 **Interactive Mobile Menu** | Filter items by category, add to cart, adjust quantities, view total price |
| ⚡ **Instant Kitchen Dispatch** | Placing order dispatches to kitchen dashboard in <100ms via WebSocket |

### 💬 Conversational WhatsApp Pre-Order Bot
| Feature | Description |
|---|---|
| 🤖 **Interactive Bot** | Customer sends `Menu` to `+1 (555) 164-0709` → bot replies with dynamic restaurant menu |
| 🔢 **Smart Input Parsing** | Accepts flexible formats like `1, 3x2, 2` (Item #1 x1, Item #3 x2, Item #2 x1) |
| ✔️ **Confirmation & Receipt** | Bot generates itemized bill + total amount; customer replies `YES` to place or `NO` to cancel |
| 📢 **Automated WhatsApp Status Updates** | Automatic WhatsApp messages on status changes: <br>• **CONFIRMED:** *"✅ Your order has been confirmed!"* <br>• **PREPARING:** *"🍳 Your order is being prepared!"* <br>• **READY:** *"🎉 Your order is ready for pickup!"* <br>• **SERVED:** *"🙏 Thank you for ordering from [Restaurant Name]! Hope you enjoyed your meal. Visit us again soon! ❤️"* <br>• **CANCELLED:** *"❌ Your order #4 has been cancelled. Reason: [Custom Reason]"* |
| 🛡️ **Duplicate & Session Safety** | Ordered `List` queries prevent `NonUniqueResultException`; sessions auto-expire after inactivity |

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                CUSTOMERS                                        │
│                                                                                 │
│  📱 Scan QR at table                     💬 WhatsApp Customer                   │
│       │                                           │                             │
│       ▼                                           ▼                             │
│  Vercel React SPA                      Meta WhatsApp Cloud API                  │
│  (whats-app-ordering-system-eta.vercel.app)       │                             │
│       │                                           │ Webhook POST                │
│       │ REST API                                  ▼                             │
│       ▼                              ┌───────────────────────────────────────┐  │
│  Render Spring Boot Backend ◄────────┤ /api/v1/webhook/whatsapp              │  │
│  (whatsapporderingsystem.onrender.com) └───────────────────────────────────────┘  │
│       │                                                                         │
│       ├── REST API (/api/v1/*)                                                  │
│       ├── STOMP WebSocket (/ws)                                                 │
│       └── Supabase PostgreSQL Database (db.ohftwlkycqsaevznacii.supabase.co)     │
│                                                                                 │
│  🖥️ Restaurant Owner Dashboard ◄────── WebSocket ────── Backend                │
│  (Real-time live queue + sound alert)                                           │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

### Backend
- **Core:** Java 21, Spring Boot 4.1.0
- **Security:** Spring Security 6.x, JWT (`jjwt` 0.12.6), BCrypt Password Hashing
- **Database:** Supabase PostgreSQL 16, Spring Data JPA, Flyway Migrations
- **Real-Time:** Spring WebSocket (STOMP + SockJS)
- **Utilities:** ZXing 3.5.3 (QR Generation), Cloudinary SDK 1.39.0 (Image Upload), Spring Retry, Bucket4j 8.10.1 (Rate Limiting)

### Frontend
- **Framework:** React 18, Vite 5.4, React Router DOM 6
- **State & HTTP:** Zustand 4.5, Axios 1.7
- **Real-Time:** `@stomp/stompjs` 7.0
- **Styling & UI:** Vanilla CSS + TailwindCSS 3.4, Lucide React Icons, React Hot Toast

---

## 💬 WhatsApp Bot Flow

```
Customer messages "Menu"
         │
         ▼
   [AWAITING_ITEMS] ──► Bot sends numbered menu list
         │
         │ Customer replies: "1, 3x2, 2"
         ▼
    [CONFIRMING] ──► Bot shows order summary & total
                     "Reply YES to confirm or NO to cancel"
         │
    ┌────┴────┐
   YES       NO
    │         │
    ▼         ▼
  [DONE]  [AWAITING_ITEMS] (reset)
    │
    ▼
  1. Order saved in Database
  2. Live STOMP WebSocket push → Sound alert 🔔 on Owner Dashboard
  3. Automated WhatsApp status updates sent as order progresses (CONFIRMED → PREPARING → READY → SERVED / CANCELLED)
```

---

## 📡 API Reference

Interactive OpenAPI / Swagger documentation is available at `/swagger-ui.html`.

### Key Endpoints:

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| **POST** | `/api/v1/auth/signup` | No | Register new restaurant owner |
| **POST** | `/api/v1/auth/login` | No | Login and receive Bearer JWT token |
| **GET** | `/api/v1/restaurants/me/categories` | Yes | List all menu categories |
| **POST** | `/api/v1/restaurants/me/categories` | Yes | Create menu category |
| **PUT** | `/api/v1/categories/{id}` | Yes | Edit category name |
| **DELETE** | `/api/v1/categories/{id}` | Yes | Delete category (items unlinked safely) |
| **GET** | `/api/v1/restaurants/me/menu-items` | Yes | List menu items |
| **POST** | `/api/v1/restaurants/me/menu-items` | Yes | Create menu item |
| **PUT** | `/api/v1/menu-items/{id}` | Yes | Edit menu item |
| **DELETE** | `/api/v1/menu-items/{id}` | Yes | Delete menu item (FK safety unlinking) |
| **PATCH** | `/api/v1/menu-items/{id}/availability` | Yes | Toggle item availability (Out of Stock) |
| **GET** | `/api/v1/restaurants/me/tables` | Yes | List all tables |
| **POST** | `/api/v1/restaurants/me/tables` | Yes | Create table |
| **PUT** | `/api/v1/tables/{id}` | Yes | Edit table number |
| **DELETE** | `/api/v1/tables/{id}` | Yes | Delete table |
| **GET** | `/api/v1/tables/{id}/qr` | Yes | Download table QR code PNG |
| **GET** | `/api/v1/restaurants/me/orders` | Yes | Fetch live orders (filterable) |
| **PATCH** | `/api/v1/orders/{id}/status` | Yes | Advance status or Cancel with custom `reason` |
| **GET** | `/api/v1/public/menu/{qrSecret}` | No | Customer menu via QR scan |
| **POST** | `/api/v1/public/orders` | No | Customer places Dine-In QR order |
| **POST** | `/api/v1/webhook/whatsapp` | No | Meta WhatsApp Webhook endpoint |

---

## 🔐 Security Architecture

1. **JWT Authentication:** Private endpoints require header `Authorization: Bearer <token>`. Unauthorized calls return HTTP 401.
2. **BCrypt Password Hashing:** Owner passwords hashed with strength 12.
3. **Stateless Session Management:** No server-side HTTP session vulnerability.
4. **Parameterized SQL Queries:** Spring Data JPA prevents SQL injection.
5. **Foreign Key Integrity:** Cascaded unlinking before item/category deletion prevents database constraint violations.

---

## 🚀 Local Setup & Deployment

### 1. Environment Configuration (`.env`)
Create a `.env` file in the root directory:

```env
DB_URL=jdbc:postgresql://localhost:5432/whatsapp_ordering
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=your_super_secret_jwt_key_min_32_characters
WHATSAPP_TOKEN=your_meta_permanent_access_token
WHATSAPP_PHONE_NUMBER_ID=1302776732910505
WHATSAPP_VERIFY_TOKEN=your_verify_token
CLOUDINARY_URL=cloudinary://key:secret@cloudname
FRONTEND_ORIGIN=http://localhost:5173
```

### 2. Run Backend (Spring Boot)
```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

### 3. Run Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```

---

## 📄 License
This project is for educational and commercial demonstration purposes.

*Built with Spring Boot 4.1 + React 18 + Supabase PostgreSQL 16 + Meta WhatsApp Cloud API*
