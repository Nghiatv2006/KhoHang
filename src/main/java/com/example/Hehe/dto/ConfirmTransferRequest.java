package com.example.Hehe.dto;

import java.util.Map;

/**
 * Request body khi chi nhánh đích xác nhận nhận hàng điều chuyển.
 * receivedQuantities: key = receiptDetailId, value = số lượng thực tế nhận được
 */
public class ConfirmTransferRequest {
    private Map<Integer, Integer> receivedQuantities;

    public Map<Integer, Integer> getReceivedQuantities() { return receivedQuantities; }
    public void setReceivedQuantities(Map<Integer, Integer> receivedQuantities) { this.receivedQuantities = receivedQuantities; }
}
