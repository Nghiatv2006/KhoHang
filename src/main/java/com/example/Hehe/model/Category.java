package com.example.Hehe.model;

import jakarta.persistence.*;

/**
 * Entity đại diện cho bảng categories trong database.
 * Quản lý thông tin danh mục sản phẩm.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    // Getters and Setters

    /**
     * Lấy ID của danh mục
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Gán ID cho danh mục
     * @param id ID mới
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Lấy tên của danh mục
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Gán tên cho danh mục
     * @param name Tên mới
     */
    public void setName(String name) {
        this.name = name;
    }
}
