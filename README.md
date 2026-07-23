# 🍽️ Restaurant Ordering System
### Dine-In QR Ordering + WhatsApp Pre-Order System

> A full-stack restaurant management platform that allows customers to scan a QR code at their table and place orders directly from their phone — or pre-order via WhatsApp. The restaurant owner gets a real-time dashboard to manage orders, menu, tables, and analytics.

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [System Architecture](#-system-architecture)
- [Prerequisites](#-prerequisites)
- [Local Setup (Step-by-Step)](#-local-setup-step-by-step)
  - [1. Clone the Repository](#1-clone-the-repository)
  - [2. Setup PostgreSQL Database](#2-setup-postgresql-database)
  - [3. Configure Environment Variables](#3-configure-environment-variables)
  - [4. Run the Backend](#4-run-the-backend)
  - [5. Run the Frontend](#5-run-the-frontend)
  - [6. Setup WhatsApp Webhook (ngrok)](#6-setup-whatsapp-webhook-ngrok)
- [External Services Setup](#-external-services-setup)
- [Project Structure](#-project-structure)
- [API Reference](#-api-reference)
- [WhatsApp Bot Flow](#-whatsapp-bot--how-it-works)
- [Troubleshooting](#-troubleshooting)

---

## ✨ Features

### 👨‍🍳 Restaurant Owner Dashboard
| Feature | Description |
|---|---|
| 🔐 **Authentication** | Secure JWT-based login & signup for restaurant owners |
| 📊 **Live Dashboard** | Real-time order stream via WebSocket (STOMP) — new orders appear instantly without refresh |
| 🍔 **Menu Management** | Add/edit/delete menu items with images (Cloudinary), categories, prices, availability toggle |
| 🪑 **Table Management** | Create tables and generate unique QR codes — download and print for each table |
| 📈 **Analytics** | Charts for revenue, order volume, top-selling items, channel breakdown (QR vs WhatsApp) |
| ⚙️ **Settings** | Configure restaurant name, WhatsApp number, and session timeout |

### 📱 Customer (QR Dine-In Ordering)
| Feature | Description |
|---|---|
| 📷 **QR Code Scan** | Customer scans QR at table → lands on a mobile-friendly menu page |
| 🛒 **Browse & Order** | Browse menu by category, add items to cart, adjust quantities |
| ✅ **Order Confirmation** | Order placed → customer gets live status page |
| 🔄 **Real-time Status** | Order status updates (PLACED → PREPARING → READY → SERVED) in real time |

### 💬 WhatsApp Pre-Order Bot
| Feature | Description |
|---|---|
| 🤖 **Conversational Bot** | Customers WhatsApp the restaurant → bot sends a numbered menu |
| 🔢 **Smart Ordering** | Reply with item numbers & quantities (e.g. `1, 3x2, 2`) |
| ✔️ **Confirmation** | Bot shows itemized summary + total, customer replies YES/NO |
| 🔔 **Dashboard Alert** | Confirmed WhatsApp orders appear instantly on owner dashboard via WebSocket |
| ⏱️ **Session Management** | Sessions auto-expire; stale sessions cleaned up every 15 minutes |
| 🔁 **Retry Logic** | WhatsApp API calls use Spring Retry for automatic retry on failure |
| 🚦 **Rate Limiting** | Bucket4j-based rate limiting to prevent webhook abuse |

---

## 🛠️ Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| **Java** | 21 | Core language |
| **Spring Boot** | 4.1.0 | Web framework |
| **Spring Security** | — | JWT authentication & route protection |
| **Spring Data JPA** | — | ORM & database access |
| **Spring WebSocket (STOMP)** | — | Real-time order push to dashboard |
| **Spring Retry** | 2.0.11 | Auto-retry WhatsApp API calls |
| **PostgreSQL** | 16 | Primary relational database |
| **Flyway** | — | Automatic database schema migrations |
| **jjwt** | 0.12.6 | JWT token creation & validation |
| **ZXing** | 3.5.3 | QR code image generation |
| **Cloudinary SDK** | 1.39.0 | Menu item image upload & storage |
| **Bucket4j** | 8.10.1 | In-memory rate limiting |
| **SpringDoc OpenAPI** | 2.6.0 | Swagger UI at /swagger-ui.html |
| **Lombok** | — | Boilerplate reduction |
| **spring-dotenv** | 4.0.0 | Load .env file into Spring Boot |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| **React** | 18.3.1 | UI library |
| **Vite** | 5.4.8 | Build tool & dev server |
| **React Router DOM** | 6.26.2 | Client-side routing |
| **Zustand** | 4.5.5 | Global state management |
| **Axios** | 1.7.7 | HTTP client for API calls |
| **@stomp/stompjs** | 7.0.0 | WebSocket client for real-time updates |
| **SockJS** | 1.6.1 | WebSocket fallback transport |
| **Recharts** | 2.12.7 | Analytics charts |
| **TailwindCSS** | 3.4.13 | Utility-first CSS styling |
| **Lucide React** | 0.441.0 | Icon library |
| **React Hot Toast** | 2.4.1 | Toast notifications |
| **date-fns** | 3.6.0 | Date formatting |

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CUSTOMERS                                │
│                                                                 │
│  📱 Scan QR at table           💬 WhatsApp customer             │
│       │                                  │                      │
│       ▼                                  ▼                      │
│  React Frontend               Meta WhatsApp Cloud API           │
│  (localhost:5173)                         │                     │
│       │                                  │ Webhook POST         │
│       │ REST API                         ▼                      │
│       ▼                    ┌─────────────────────────────────┐  │
│  Spring Boot Backend ◄────►│  /api/webhook/whatsapp          │  │
│  (localhost:8080)          └─────────────────────────────────┘  │
│       │                                                         │
│       ├── REST API (/api/*)                                     │
│       ├── WebSocket (STOMP: /ws)                                │
│       └── PostgreSQL (localhost:5432)                           │
│                                                                 │
│  🖥️ Owner Dashboard ◄── WebSocket ── Backend                   │
│  (React, JWT protected)                                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📦 Prerequisites

Make sure you have installed:

| Tool | Version | Download |
|---|---|---|
| **Java JDK** | 21+ | https://adoptium.net/ |
| **Node.js** | 18+ | https://nodejs.org/ |
| **Docker Desktop** | Latest | https://www.docker.com/products/docker-desktop/ |
| **Git** | Any | https://git-scm.com/ |

> Maven is included as `mvnw` / `mvnw.cmd` wrapper — no separate install needed.
> Docker is only required to run PostgreSQL as a container.

---

## 🚀 Local Setup (Step-by-Step)

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/whatsappOrdering.git
cd whatsappOrdering
```

---

### 2. Setup PostgreSQL Database

**Option A — Using Docker (Recommended)**

```bash
# Start PostgreSQL container
docker-compose up -d

# Verify it is running
docker ps
```

Container details:
- Host: localhost
- Port: 5432
- Database: whatsapp_ordering
- User: postgres
- Password: postgres

**Option B — Local PostgreSQL**

If you have PostgreSQL already installed:

```sql
-- Run in psql or pgAdmin
CREATE DATABASE whatsapp_ordering;
```

> Database tables are created automatically by Flyway migrations on first startup.

---

### 3. Configure Environment Variables

```bash
# Windows
copy .env.example .env

# Linux / Mac
cp .env.example .env
```

Edit `.env` with your values:

```env
# ── Database ─────────────────────────────────────────────────────
DB_URL=jdbc:postgresql://localhost:5432/whatsapp_ordering
DB_USER=postgres
DB_PASSWORD=postgres

# ── JWT ──────────────────────────────────────────────────────────
# Any long random string (min 32 characters)
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production

# ── WhatsApp Cloud API ───────────────────────────────────────────
# From: https://developers.facebook.com/ → Your App → WhatsApp → API Setup
WHATSAPP_TOKEN=your_permanent_system_user_token
WHATSAPP_PHONE_NUMBER_ID=your_phone_number_id
WHATSAPP_VERIFY_TOKEN=any_string_you_choose

# ── Cloudinary ───────────────────────────────────────────────────
# From: https://cloudinary.com → Dashboard → API Environment variable
CLOUDINARY_URL=cloudinary://api_key:api_secret@cloud_name

# ── CORS ─────────────────────────────────────────────────────────
FRONTEND_ORIGIN=http://localhost:5173
```

> For a quick test without WhatsApp / Cloudinary: put any dummy string for those fields.
> The app will still run; only those specific features will be non-functional.

---

### 4. Run the Backend

```bash
# Windows (from project root)
.\mvnw.cmd spring-boot:run

# Linux / Mac
./mvnw spring-boot:run
```

What happens on first run:
1. Maven downloads all dependencies (~2-3 minutes)
2. Flyway creates all database tables automatically
3. Spring Boot starts on http://localhost:8080

Verify it is working:
- Health check:  http://localhost:8080/actuator/health
- Swagger UI:    http://localhost:8080/swagger-ui.html

---

### 5. Run the Frontend

Open a NEW terminal window:

```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start dev server
npm run dev
```

Frontend available at: http://localhost:5173

---

### 6. Setup WhatsApp Webhook (ngrok)

> Skip this step if you only want QR-based ordering. Required only for WhatsApp bot.

The WhatsApp Cloud API needs a public HTTPS URL to send webhooks.
ngrok is already included in the project root as `ngrok.exe`.

```powershell
# Windows (from project root)
.\ngrok.exe http 8080
```

ngrok output example:
```
Forwarding   https://abc123.ngrok-free.app -> http://localhost:8080
```

Copy that https:// URL, then:

1. Go to https://developers.facebook.com/
2. Your App → WhatsApp → Configuration
3. Webhook URL: https://abc123.ngrok-free.app/api/webhook/whatsapp
4. Verify Token: same value you set as WHATSAPP_VERIFY_TOKEN in .env
5. Subscribe to: messages

---

## 🔧 External Services Setup

### Meta WhatsApp Cloud API

1. Go to https://developers.facebook.com/ and create a developer account
2. Create a new App → Select Business type
3. Add the WhatsApp product
4. Go to WhatsApp → API Setup
5. Note down:
   - Phone Number ID → WHATSAPP_PHONE_NUMBER_ID
   - Create a Permanent System User Token → WHATSAPP_TOKEN
6. Add your phone number as a test recipient (free tier limitation)

### Cloudinary (Menu Image Storage)

1. Sign up at https://cloudinary.com (free tier is sufficient)
2. Go to Dashboard
3. Copy the API Environment variable (format: cloudinary://key:secret@cloudname)
4. Paste it as CLOUDINARY_URL in .env

---

## 📁 Project Structure

```
whatsappOrdering/
│
├── src/main/java/com/example/whatsappOrdering/
│   ├── config/               # App properties, CORS, Security, WebSocket config
│   ├── controller/
│   │   ├── AuthController.java            # /api/auth/** — login, signup
│   │   ├── MenuController.java            # /api/menu/** — menu CRUD
│   │   ├── TableController.java           # /api/tables/** — table & QR management
│   │   ├── OrderController.java           # /api/orders/** — order lifecycle
│   │   ├── AnalyticsController.java       # /api/analytics/** — chart data
│   │   ├── RestaurantController.java      # /api/restaurant/** — settings
│   │   ├── PublicMenuController.java      # /api/public/** — customer menu (no auth)
│   │   └── WhatsappWebhookController.java # /api/webhook/whatsapp — Meta webhook
│   ├── service/
│   │   └── WhatsappService.java           # Conversational state machine
│   ├── entity/               # JPA database entities
│   │   ├── Restaurant.java, User.java
│   │   ├── MenuItem.java, Category.java
│   │   ├── RestaurantTable.java
│   │   ├── Order.java, OrderItem.java
│   │   └── WhatsappSession.java, WhatsappMessage.java
│   ├── repository/           # Spring Data JPA repositories
│   ├── security/             # JWT filter & token utility
│   ├── websocket/            # STOMP WebSocket publisher
│   ├── dto/                  # Request / response data classes
│   └── exception/            # Global exception handler
│
├── src/main/resources/
│   ├── application.properties            # Spring config (reads .env values)
│   └── db/migration/                     # Flyway SQL migration scripts
│
├── frontend/
│   └── src/
│       ├── pages/
│       │   ├── auth/         # Login.jsx, Signup.jsx
│       │   ├── dashboard/    # Dashboard, MenuManagement, TableManagement,
│       │   │                 # Analytics, Settings
│       │   └── customer/     # CustomerMenu.jsx, OrderStatus.jsx
│       ├── components/       # Reusable UI components, DashboardLayout
│       ├── store/            # Zustand global state (authStore, etc.)
│       ├── api/              # Axios API client configuration
│       └── hooks/            # Custom React hooks
│
├── docker-compose.yml        # PostgreSQL container definition
├── .env.example              # Template for environment variables
├── .env                      # Your local secrets (DO NOT commit to git)
├── pom.xml                   # Maven build & dependency config
└── ngrok.exe                 # Tunnel for WhatsApp webhook (Windows)
```

---

## 📡 API Reference

All APIs are interactive at http://localhost:8080/swagger-ui.html

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| POST | /api/auth/signup | No | Register restaurant owner |
| POST | /api/auth/login | No | Login, receive JWT token |
| GET | /api/menu | Yes | Get all menu items |
| POST | /api/menu | Yes | Add menu item (with image) |
| PUT | /api/menu/{id} | Yes | Update menu item |
| DELETE | /api/menu/{id} | Yes | Delete menu item |
| GET | /api/tables | Yes | List all tables |
| POST | /api/tables | Yes | Create new table |
| GET | /api/tables/{id}/qr | Yes | Download table QR code |
| GET | /api/orders | Yes | Get orders (filterable) |
| PATCH | /api/orders/{id}/status | Yes | Update order status |
| GET | /api/analytics/summary | Yes | Revenue & analytics data |
| GET | /api/public/menu/{qrSecret} | No | Customer menu via QR |
| POST | /api/public/orders | No | Place a QR-based order |
| GET | /api/webhook/whatsapp | No | Meta webhook verification |
| POST | /api/webhook/whatsapp | No | Receive WhatsApp messages |
| GET | /actuator/health | No | Backend health check |

> Auth: Add header `Authorization: Bearer <your_jwt_token>`

---

## 🔄 WhatsApp Bot — How It Works

The bot uses a 3-state conversational state machine:

```
Customer sends any message
         │
         ▼
   [AWAITING_ITEMS] ──► Bot sends numbered menu list
         │
         │  Customer replies: "1, 3x2, 2"
         ▼
    [CONFIRMING] ──► Bot shows order summary + total
                     "Reply YES to confirm or NO to cancel"
         │
    ┌────┴────┐
   YES       NO
    │         │
    ▼         ▼
  [DONE]  [AWAITING_ITEMS] (restart)
    │
    ▼
  Order created in database
  Real-time WebSocket push → Owner Dashboard notification
  Confirmation SMS sent to customer
```

Order format: "1, 3x2, 2"
  - 1     → Item #1, quantity 1
  - 3x2   → Item #3, quantity 2
  - 2     → Item #2, quantity 1

Sessions auto-expire after a configurable timeout (set in Settings).
Stale sessions are cleaned up automatically every 15 minutes.

---

## 🔑 First Time Login

There are no pre-seeded users. Steps:
1. Open http://localhost:5173/signup
2. Create your restaurant owner account
3. Login at http://localhost:5173/login
4. Go to Settings and set your restaurant WhatsApp number

---

## 🐛 Troubleshooting

### Backend won't start

| Problem | Solution |
|---|---|
| Cannot connect to PostgreSQL | Run: docker-compose up -d and wait 10 seconds |
| Port 8080 already in use | Kill the process or change port in application.properties |
| Flyway migration failed | Drop and recreate the DB, then restart backend |
| .env values not loading | Ensure .env file is in the project root (same folder as pom.xml) |

### Frontend issues

| Problem | Solution |
|---|---|
| npm not found | Install Node.js from https://nodejs.org/ |
| CORS error in browser | Ensure FRONTEND_ORIGIN=http://localhost:5173 in .env; restart backend |
| Blank page after login | Open browser DevTools (F12) → Console for errors |
| Cannot connect to backend | Verify backend is running on port 8080 |

### WhatsApp bot issues

| Problem | Solution |
|---|---|
| Webhook not verified | WHATSAPP_VERIFY_TOKEN in .env must exactly match Meta Console value |
| ngrok tunnel expired | Restart ngrok and update webhook URL in Meta Developer Console |
| Messages not received | Add your phone as a test recipient in Meta Developer Console |
| Restaurant not found in logs | In Dashboard Settings, set WhatsApp number (digits only, no + or spaces) |

---

## 🔒 Security Notes

- NEVER commit .env to git — it is already in .gitignore
- Use a strong random string (32+ characters) for JWT_SECRET
- In production, use your hosting platform's environment variable system instead of .env
- WHATSAPP_VERIFY_TOKEN can be any string; just keep it consistent between .env and Meta Console

---

## 📄 License

This project is for educational and demonstration purposes.

---

*Built with Spring Boot 4.1 + React 18 + PostgreSQL 16*
