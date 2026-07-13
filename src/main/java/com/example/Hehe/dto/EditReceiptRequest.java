package com.example.Hehe.dto;

import java.util.List;

/**
 * Request body khi Staff hoặc Manager chỉnh sửa phiếu.
 */
public class EditReceiptRequest {

    /** Ghi chú phiếu (có thể null nếu không đổi) */
    private String description;

    /** Lý do chỉnh sửa — bắt buộc phải nhập */
    private String editReason;

    private String customerName;
    private String customerPhone;

    /** Danh sách dòng sản phẩm cần cập nhật số lượng */
    private List<EditDetailItem> details;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEditReason() { return editReason; }
    public void setEditReason(String editReason) { this.editReason = editReason; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public List<EditDetailItem> getDetails() { return details; }
    public void setDetails(List<EditDetailItem> details) { this.details = details; }

    /** Một dòng sản phẩm cần sửa số lượng */
    public static class EditDetailItem {
        private Integer detailId;
        private Integer quantity;

        public Integer getDetailId() { return detailId; }
        public void setDetailId(Integer detailId) { this.detailId = detailId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
