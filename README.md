# Real-Time Event Ticketing System Frontend

A premium, production-quality React frontend built with Vite and vanilla CSS. It features a dark cinematic aesthetic tailored for live performances, concerts, and theatrical events. It interacts in real-time with a Spring Boot backend, implementing live seat-map updates via WebSockets (STOMP/SockJS) and secure checkout via Razorpay integration.

---

## 🚀 Key Features

*   **Real-time Seat Map:** Real-time seat updates are broadcast using WebSockets. When another user holds or books a seat, the change is pushed to all active clients immediately without page refreshes, styled with a visual flash keyframe animation.
*   **Secure Multi-Step Booking:**
    1.  Hold seats (atomically requests the backend to transition seats to `HELD` for 10 minutes).
    2.  Timer countdown displays on checkout.
    3.  Initiates Razorpay transaction and launches the Razorpay Checkout widget.
    4.  Polls the backend booking status until payment confirmation is verified via the backend's server-to-server webhook (the frontend success callback does not directly mark the ticket as confirmed).
*   **Multi-step Signup:** Secure registration flow verifying email addresses via OTP (`sendOtp` &rarr; `signup` with verification).
*   **Role-Based Access Control:** Automatically decodes the JWT role and restricts access dynamically:
    *   **USER:** Browses events, holds seats, checks out via Razorpay, manages ticket history, cancels unpaid holds, requests refunds, joins waitlists, and accepts seat offers.
    *   **ORGANISER:** Creates venues, designs seat layouts (with a capacity matching validator), publishes events (which triggers bulk seat generation), and cancels events.
    *   **ADMIN:** Toggles user roles (USER &harr; ORGANISER), deletes users, moderates system-wide events, and manually triggers trending event recalculations.

---

## 🛠️ Tech Stack

*   **Framework:** React 18 + Vite (Plain JavaScript `.js` / `.jsx`)
*   **Styling:** Vanilla CSS using a custom HSL Design System, glassmorphism, responsive grids, and CSS transitions.
*   **Real-Time Messaging:** `@stomp/stompjs` + `sockjs-client`
*   **Networking:** `axios`
*   **Icons:** `lucide-react`
*   **Routing:** `react-router-dom`

---

## 📂 Directory Structure

```text
frontend/
├── package.json
├── vite.config.js
├── index.html
└── src/
    ├── main.jsx
    ├── App.jsx
    ├── index.css                 # Custom HSL design tokens, scrollbars, and keyframes
    ├── services/
    │   └── api.js                # Axios client with JWT request headers
    ├── context/
    │   └── AuthContext.jsx       # Auth state, login/signup, and JWT base64 decoder
    ├── hooks/
    │   ├── useEventWebSocket.js   # STOMP-over-SockJS seat map updates
    │   └── useRazorpay.js        # Dynamic Razorpay script loader
    ├── components/
    │   ├── Button.jsx            # Idempotency-safe button with spinner
    │   ├── Navbar.jsx            # Responsive role-based header
    │   ├── Footer.jsx            # Sleek cinematic footer
    │   └── SeatMap.jsx           # Seating grid, hover states, and category zones
    └── pages/
        ├── Home.jsx              # Event browsing, Trending section, and Admin recalculate
        ├── EventDetail.jsx       # Seat map, hold reservation, and waitlist joining
        ├── Login.jsx             # Credentials login & Forgot Password OTP reset flows
        ├── Signup.jsx            # 3-Step OTP Signup Wizard
        ├── BookingCheckout.jsx   # 10-min countdown, Razorpay widget, and status polling
        ├── MyBookings.jsx        # Ticket history, refund requests, and waitlist claims
        ├── OrganiserDashboard.jsx# Venue registration, Event builder, and layout validator
        └── AdminDashboard.jsx    # User moderation tables, role toggles, and account deletion
```

---

## 🔧 Getting Started

### 1. Prerequisites
Ensure you have [Node.js](https://nodejs.org/) installed (LTS recommended) and that your Spring Boot backend is running locally on port `8080`.

### 2. Installation
Navigate to the `frontend/` directory and install the dependencies:
```bash
cd frontend
npm install
```

### 3. Running Locally
Launch the Vite development server:
```bash
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) in your browser.

### 4. Build for Production
To generate a production build:
```bash
npm run build
```
The output files will be compiled into the `dist/` folder.

---

## 🎨 HSL Design System (`src/index.css`)

The UI is built using tailored HSL color variables:
*   `--bg-main`: `hsl(222, 25%, 6%)` (Deep Slate Black)
*   `--primary`: `hsl(342, 100%, 50%)` (Cinematic Neon Pink)
*   `--accent`: `hsl(250, 100%, 65%)` (Electric Indigo)
*   `--color-available`: `hsl(142, 72%, 45%)` (Emerald Green)
*   `--color-held`: `hsl(45, 100%, 50%)` (Gold Yellow)
*   `--color-booked`: `hsl(354, 85%, 52%)` (Danger Red)
*   `--color-selected`: `hsl(190, 100%, 50%)` (Selected Cyan)

---

## 🛡️ Role-based Demo Accounts

To test the application, register accounts via the **Sign Up** wizard or promote accounts via the **Admin Panel**:
1.  **USER Role:** Registers directly from the sign-up page.
2.  **ORGANISER Role:** Registers directly from the sign-up page (by selecting "Organiser" in Step 3).
3.  **ADMIN Role:** Must be promoted by editing database values directly or using a pre-seeded admin user.
