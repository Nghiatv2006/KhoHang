package com.example.Hehe.service;

import com.example.Hehe.dto.CustomerResponse;
import com.example.Hehe.dto.CustomerSaveRequest;
import com.example.Hehe.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerService {
    List<CustomerResponse> searchCustomers(String keyword, String status);
    CustomerResponse getCustomerById(Integer id);
    CustomerResponse createCustomer(CustomerSaveRequest request, User currentUser);
    CustomerResponse updateCustomer(Integer id, CustomerSaveRequest request, User currentUser);
    void deleteCustomer(Integer id, User currentUser);
    CustomerResponse toggleCustomerStatus(Integer id, User currentUser);
    CustomerResponse adjustDebt(Integer id, BigDecimal amount, User currentUser);
}
