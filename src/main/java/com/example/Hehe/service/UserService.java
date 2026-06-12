package com.example.Hehe.service;

import com.example.Hehe.dto.ChangePasswordRequest;
import com.example.Hehe.dto.UserResponse;
import com.example.Hehe.dto.UserSaveRequest;
import com.example.Hehe.model.User;
import java.util.List;

public interface UserService {
    List<UserResponse> searchUsers(String keyword, String role, Integer branchId, String status, User currentUser);
    
    UserResponse createUser(UserSaveRequest request, User currentUser);
    
    UserResponse updateUser(Integer id, UserSaveRequest request, User currentUser);
    
    void deleteUser(Integer id, User currentUser);
    
    UserResponse toggleUserStatus(Integer id, User currentUser);

    void changePassword(ChangePasswordRequest request, User currentUser);
}
