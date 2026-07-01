package com.example.Hehe.service;

import com.example.Hehe.dto.SupplierResponse;
import com.example.Hehe.dto.SupplierSaveRequest;
import com.example.Hehe.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface SupplierService {
    List<SupplierResponse> searchSuppliers(String keyword, String status);
    SupplierResponse getSupplierById(Integer id);
    SupplierResponse createSupplier(SupplierSaveRequest request, User currentUser);
    SupplierResponse updateSupplier(Integer id, SupplierSaveRequest request, User currentUser);
    void deleteSupplier(Integer id, User currentUser);
    SupplierResponse toggleSupplierStatus(Integer id, User currentUser);
    SupplierResponse adjustDebt(Integer id, BigDecimal amount, User currentUser);
}
