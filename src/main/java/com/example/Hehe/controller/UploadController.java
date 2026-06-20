package com.example.Hehe.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        
        if (file.isEmpty()) {
            response.put("error", "File is empty");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Lấy tên file gốc và tạo tên file mới tránh trùng lặp
            String originalName = file.getOriginalFilename();
            if (originalName == null) {
                originalName = "unknown";
            }
            String originalFilename = StringUtils.cleanPath(originalName);
            String fileExtension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if(dotIndex > 0) {
                fileExtension = originalFilename.substring(dotIndex);
            }
            
            String newFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Đảm bảo thư mục tồn tại
            Path uploadPath = Paths.get("uploads/images");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Lưu file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Trả về đường dẫn tương đối để client có thể truy cập
            // Ví dụ: /uploads/images/abc-xyz.png
            String fileUrl = "/uploads/images/" + newFilename;
            response.put("url", fileUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException ex) {
            response.put("error", "Could not store file. Please try again!");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
