package com.example.Hehe.service;

import com.example.Hehe.dto.StocktakeResponse;
import com.example.Hehe.dto.StocktakeSaveRequest;
import com.example.Hehe.model.User;
import java.util.List;

public interface StocktakeService {
    List<StocktakeResponse> getAllStocktakes(User currentUser);
    StocktakeResponse getStocktakeById(Integer id, User currentUser);
    StocktakeResponse createStocktake(StocktakeSaveRequest request, User currentUser);
    StocktakeResponse updateStocktake(Integer id, StocktakeSaveRequest request, User currentUser);
    StocktakeResponse completeStocktake(Integer id, User currentUser);
    StocktakeResponse approveStocktake(Integer id, com.example.Hehe.dto.StocktakeApproveRequest request, User currentUser);
    StocktakeResponse cancelStocktake(Integer id, User currentUser);
    StocktakeResponse rejectStocktake(Integer id, User currentUser);
}
