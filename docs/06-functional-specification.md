# 06 — Functional Specification (key rules)

This specification captures **business rules** implemented in the Capstone system.

---

## 1. Product scope

- Single retail brand, many branches  
- Staff use Web + POS; customers use Mobile API  
- Shared MySQL database

---

## 2. Authentication

| Client | Auth |
| --- | --- |
| Staff Web / POS | JWT from staff auth API |
| Customer mobile | Customer JWT (separate secret) |

Staff without a web role cannot access the web system.

---

## 3. POS module

### 3.1 Preconditions
- FE route guard requires an OPEN shift session before POS selling screens  
- Cart is in-memory (refresh clears cart) — documented limitation

### 3.2 Pricing
- Client must not send final price / discount totals as source of truth  
- Server loads product prices, applies campaign (PERCENT/FIXED), then points

### 3.3 Payments
- Methods: `CASH` | `PAYOS` only  
- PayOS: stock reserved at checkout; status `PENDING_PAYMENT` until webhook; cancel releases stock

### 3.4 Loyalty at POS
- Lookup by exact phone or email  
- No OTP at POS redeem (risk accepted; audit via invoice + cashier id)  
- Points chain-wide

### 3.5 Refunds
- Completed orders only  
- Same branch  
- ≤ 5 minutes from `createdAt` (server clock)  
- Reason mandatory  
- Restores inventory and points

---

## 4. Shift module

| Rule | Behavior |
| --- | --- |
| Open sessions | Max 1 OPEN per branch |
| Opening cash | 2,000,000 VND first published shift of day; else previous close actual |
| Overdue | Auto-close job after `endTime + 30 minutes` |
| Early close | Needs scheduled replacement cashier covering current time |
| Expected cash | Opening + successful CASH sales − refunds |

---

## 5. Promotions

- Entities: **Campaigns** (not POS voucher codes)  
- Scopes: `CHAIN` | `BRANCH`  
- HQ roles create chain campaigns; BM creates branch campaigns  
- Status lifecycle includes DEACTIVATED ↔ ACTIVE  
- BUY_X_GET_Y type deactivated in current product

---

## 6. Customer mobile API (`mobile-backend`)

| Area | Behavior |
| --- | --- |
| Auth | Register / login with email OTP |
| Profile | View/update profile, membership QR |
| Loyalty | Point history, tier list, loyalty config |
| Promotions | List active promotions for display |

Port default: **4314**. Must use same DB and loyalty rate config as staff API.

---

## 7. Non-functional notes

- HTTPS production demo: `chainstore.site` / `api.chainstore.site`  
- Secrets via environment variables (not committed)  
- CORS restricted to known frontend origins in production

---

## 8. Known limitations (honest BA notes)

| Topic | Status |
| --- | --- |
| Offline POS | Not supported |
| Split payment | Not supported |
| POS OTP for points | Not supported |
| Full PayOS money refund | Manual outside system |
| Some Director planning APIs | Stub / redirected to shared Reports UI |
