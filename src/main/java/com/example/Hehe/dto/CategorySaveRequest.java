package com.example.Hehe.dto;

/**
 * DTO dùng để nhận dữ liệu khi thêm mới hoặc cập nhật danh mục từ client.
 */
public class CategorySaveRequest {

    private String name;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
