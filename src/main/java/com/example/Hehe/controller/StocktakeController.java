package com.example.Hehe.controller;

import com.example.Hehe.dto.StocktakeResponse;
import com.example.Hehe.dto.StocktakeSaveRequest;
import com.example.Hehe.model.User;
import com.example.Hehe.service.StocktakeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocktakes")
public class StocktakeController {

    private final StocktakeService stocktakeService;

    public StocktakeController(StocktakeService stocktakeService) {
        this.stocktakeService = stocktakeService;
    }

    @GetMapping
    public ResponseEntity<List<StocktakeResponse>> getAllStocktakes(@AuthenticationPrincipal User currentUser) {
        List<StocktakeResponse> res = stocktakeService.getAllStocktakes(currentUser);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StocktakeResponse> getStocktakeById(@PathVariable Integer id, @AuthenticationPrincipal User currentUser) {
        StocktakeResponse res = stocktakeService.getStocktakeById(id, currentUser);
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<StocktakeResponse> createStocktake(@RequestBody StocktakeSaveRequest request, @AuthenticationPrincipal User currentUser) {
        StocktakeResponse res = stocktakeService.createStocktake(request, currentUser);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StocktakeResponse> updateStocktake(@PathVariable Integer id, @RequestBody StocktakeSaveRequest request, @AuthenticationPrincipal User currentUser) {
        StocktakeResponse res = stocktakeService.updateStocktake(id, request, currentUser);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<StocktakeResponse> completeStocktake(@PathVariable Integer id, @AuthenticationPrincipal User currentUser) {
        StocktakeResponse res = stocktakeService.completeStocktake(id, currentUser);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<StocktakeResponse> cancelStocktake(@PathVariable Integer id, @AuthenticationPrincipal User currentUser) {
        StocktakeResponse res = stocktakeService.cancelStocktake(id, currentUser);
        return ResponseEntity.ok(res);
    }
}
