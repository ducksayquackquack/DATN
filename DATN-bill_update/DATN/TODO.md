# TODO - Payment Flow Implementation

## Task: Complete payment flow with notifications and invoice

### Steps:
1. [x] Analyze codebase and understand current flow
2. [x] Update Checkout.vue - Fix payment flow and "Xác nhận thanh toán" button
3. [x] Update HoaDonDetail.vue - Ensure employee can confirm payment and send notification
4. [ ] Update HomeView.vue - Display invoice after successful payment notification
5. [ ] Test and verify the complete flow

### Files Edited:
- `src/views/customer/Checkout.vue` ✅
- `src/views/admin/hoa-don/HoaDonDetail.vue` ✅
- `src/views/customer/HomeView.vue` - Pending

### Flow:
1. Customer selects products → Checkout
2. Customer fills info, selects payment method (Tiền mặt/VNPAY)
3. Customer clicks "Xác nhận đặt hàng"
4. System shows "Xác nhận thanh toán" button for customer to confirm they paid
5. Employee receives notification, verifies payment with shop account
6. Employee clicks "Xác nhận thanh toán"
7. Customer receives notification with invoice

