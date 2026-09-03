# 01 — Business Overview

## Product name

**ChainStore** — Convenience Store Chain Sales Management System  
*(Vietnamese Capstone: hệ thống quản lý bán hàng chuỗi cửa hàng tiện lợi)*

## Business problem

Many SME retail chains still run day-to-day operations on **Excel / paper**:

- Sales, cash handover, and inventory are disconnected
- Promotions and loyalty points are inconsistent across branches
- HQ cannot see branch performance in near real time
- Cashiers lack a controlled POS path (shift → sell → pay → close)

## Product vision

Deliver a **single-brand, multi-branch** platform that connects:

1. **HQ & branch web operations** (users, branches, catalog, warehouse, promotions, reports)
2. **In-store POS** (shift cash control, checkout, campaigns, loyalty, refunds)
3. **Customer mobile experience** (register/login OTP, profile, points history, active promotions)

## In scope (Capstone)

| Domain | Capability |
| --- | --- |
| Identity & access | Role-based web/POS access; customer JWT for mobile |
| Master data | Branches, users, products, categories, suppliers |
| Inventory | Central warehouse + branch stock; purchase requests / POs / dispatch |
| Promotions | Campaign management (chain / branch), activate / deactivate |
| POS | Open/close shift, barcode sell, CASH/PayOS, campaign + points, refund window |
| Loyalty | Chain-wide points earn/redeem; membership tiers |
| Customer app API | Auth OTP, profile, QR, points history, promotions list |
| Reporting | Chain / branch sales and operational views |

## Out of scope (explicit)

- Multi-tenant SaaS (many unrelated brands on one deploy)
- Offline POS / local sync
- Split tender (cash + QR on one order)
- E-invoice / tax authority integration
- Full payroll / attendance module
- PayOS bank-side money reversal on refund (system restores stock/points only)

## Success criteria

- Cashier can complete a controlled shift day without Excel cash book
- Price, stock, and promotion amounts are enforced **server-side**
- HQ (Admin/Director) and Branch Manager have clear responsibility boundaries
- Customer can view loyalty status and promotions via mobile API

## Stakeholders

| Stakeholder | Interest |
| --- | --- |
| Chain owner / Director | Revenue, promotions, branch portfolio |
| Admin | System configuration, accounts, master data |
| Branch Manager | Staff, shifts, branch campaigns, cash discrepancy |
| Warehouse Manager | Supply, dispatch, central inventory |
| Cashier | Fast, auditable checkout |
| Customer | Points, tiers, promotions |
