# 05 — User Stories (sample backlog)

Format: **As a… I want… So that…** + Acceptance Criteria.

---

## Epic A — Access & organization

### US-A1 Create staff account
**As an** Admin  
**I want** to create HQ/branch staff with the correct role and branch  
**So that** only authorized people access the system  

**AC**
- Critical roles (Admin/Director/WM) respect single-slot rules
- Director cannot create an Admin account
- Branch roles require `branchId`

### US-A2 Role-based landing
**As a** signed-in staff user  
**I want** to land on the screen that matches my role  
**So that** I start work without hunting menus  

**AC**
- Cashier → POS; Admin → Users; Director → Dashboard; etc.

---

## Epic B — Shift & cash control

### US-B1 Open shift
**As a** Cashier  
**I want** to open my assigned shift with an opening fund  
**So that** cash sales are tied to a controllable till  

**AC**
- Opening requires published assignment (demo bypass only for demo account)
- Only one OPEN session per branch
- Opening fund = default for first session of day, else previous actual cash

### US-B2 Close shift
**As a** Cashier  
**I want** to count cash, confirm high-value items, and close  
**So that** discrepancies are recorded for BM review  

**AC**
- Expected cash = opening + successful CASH sales − refunds
- Remark required when actual ≠ expected
- Early close requires a scheduled replacement cashier covering “now”

---

## Epic C — POS selling

### US-C1 Add product by barcode
**As a** Cashier  
**I want** to scan/type barcode into the cart  
**So that** checkout is fast at the counter  

**AC**
- Unknown barcode shows clear error
- Quantity editable before payment

### US-C2 Checkout cash / PayOS
**As a** Cashier  
**I want** to complete payment with CASH or PayOS  
**So that** the customer can leave with a valid invoice  

**AC**
- Server recomputes line prices (client amounts ignored)
- Stock deducted atomically; insufficient stock fails the whole order
- One payment method per order (no split tender)
- CASH → COMPLETED; PayOS → PENDING_PAYMENT until webhook

### US-C3 Apply campaign & redeem points
**As a** Cashier  
**I want** to select an active campaign and redeem customer points  
**So that** discounts stay consistent with HQ rules  

**AC**
- Campaign must be ACTIVE, in date range, visible for the branch
- Points redeem capped by order value rules
- Customer lookup by exact phone/email

### US-C4 Refund within window
**As a** Cashier  
**I want** to refund a mistaken completed order within 5 minutes  
**So that** stock and points can be restored quickly  

**AC**
- Reason required
- Window enforced by server time
- Restores stock + points; PayOS bank money is not auto-reversed

---

## Epic D — Promotions (HQ / Branch)

### US-D1 Create chain campaign
**As a** Director or Admin  
**I want** to create a CHAIN campaign  
**So that** all targeted branches can apply the same promotion  

**AC**
- Created as DEACTIVATED until activated
- BM may deactivate for their own branch (opt-out)

### US-D2 Create branch campaign
**As a** Branch Manager  
**I want** to create a BRANCH campaign for my store only  
**So that** local promotions do not affect other branches  

---

## Epic E — Customer mobile

### US-E1 Register / login with OTP
**As a** Customer  
**I want** to verify email with OTP  
**So that** my loyalty account is protected  

### US-E2 View points & promotions
**As a** Customer  
**I want** to see point history, tier, and active promotions  
**So that** I understand my benefits before visiting a store  

**AC**
- Mobile lists promotions for awareness; POS applies campaigns at checkout
- Invoice history shows purchase summary (cashier name not exposed on customer app)

---

## Epic F — Supply & warehouse (summary)

- BM creates purchase / import requests when branch stock is low  
- WM approves, chooses supplier / dispatch, manages central inventory  
- Branch receives shipments and may run inventory counts  

*(Detailed supply stories follow the same AC style in sprint backlogs.)*
