# Debug Guide: Invoice Product Issue

## Problem
When creating a new invoice (hoadon), products are added via the modal but when clicking "Lưu hoá đơn" (Save Invoice), the system shows:
- Toast error: "Hoá đơn cần ít nhất một sản phẩm" (Invoice needs at least 1 product)
- The invoice shows "0 sản phẩm" (0 products)
- This happens for all customer types (khách lẻ, known customer) and all payment methods

## Debug Logging Added

I've added console.log statements to track the issue:

### 1. In `addProductFromModal()` function
- Logs when a product is selected from the modal
- Shows the variant data

### 2. In `addProduct()` function
- Logs the current items array length BEFORE adding
- Logs the newItem being added
- Logs the items array length AFTER adding
- Logs the complete items array content

### 3. In `saveInvoice()` function
- Logs when save is triggered
- Logs items.value.length
- Logs the complete items array
- Logs isCreate, isPosOrder, and khachHangId values

## How to Debug

1. Open the browser DevTools Console (F12)
2. Navigate to create a new invoice: `/admin/hoa-don/detail/create` or `/employee/hoa-don/detail/create`
3. Click "Thêm sản phẩm" (Add Product) button
4. Select a product from the modal
5. Check console for `[DEBUG addProductFromModal]` and `[DEBUG addProduct]` logs
6. Click "Lưu hoá đơn" (Save Invoice) button
7. Check console for `[DEBUG saveInvoice]` logs

## Expected vs Actual Behavior

### Expected:
```
[DEBUG addProductFromModal] Called with variant: {spctId: 123, ...}
[DEBUG addProduct] Called with newItem: {spctId: 123, soLuong: 1}
[DEBUG addProduct] Current items.value.length: 0
[DEBUG addProduct] After adding, items.value.length: 1
[DEBUG addProduct] items.value: [{id: null, spctId: 123, ...}]
[DEBUG saveInvoice] Called
[DEBUG saveInvoice] items.value.length: 1
[DEBUG saveInvoice] items.value: [{id: null, spctId: 123, ...}]
```

### If items are being cleared:
```
[DEBUG addProduct] After adding, items.value.length: 1
[DEBUG saveInvoice] items.value.length: 0  <-- PROBLEM!
```

## Possible Causes

1. **Items array is being cleared between add and save**
   - Check if there's a watcher or lifecycle hook clearing items
   - Check if route navigation is resetting state

2. **Items are added to wrong reactive reference**
   - Verify items.value is the same reference throughout

3. **Modal is in a different component scope**
   - Check if Teleport is causing scope issues

4. **Race condition with async operations**
   - Check if loadData() or other async functions are resetting items

## Next Steps

Based on the console logs, we can determine:
- If items ARE being added successfully → Something clears them before save
- If items are NOT being added → Problem in addProduct() logic
- If items show in console but validation fails → Reactivity issue

## Files Modified

- `DATNDIRTYWAVE-merged/src/views/admin/hoa-don/HoaDonDetail.vue`
  - Added debug logging to `addProductFromModal()` (line ~256)
  - Added debug logging to `addProduct()` (line ~1367)
  - Added debug logging to `saveInvoice()` (line ~1506)
