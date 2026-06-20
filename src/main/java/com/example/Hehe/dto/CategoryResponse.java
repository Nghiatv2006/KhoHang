package com.example.Hehe.dto;

import com.example.Hehe.model.Category;

/**
 * DTO trả về thông tin của danh mục cho client.
 */
public class CategoryResponse {

    private Integer id;
    private String name;

    public CategoryResponse() {}

    /**
     * Constructor chuyển đổi từ Entity sang DTO
     * @param category Đối tượng entity
     */
    public CategoryResponse(Category category) {
        if (category != null) {
            this.id = category.getId();
            this.name = category.getName();
        }
    }

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
