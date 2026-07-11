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
                fileExtension = originalFilename.substring(dotIndex).toLowerCase();
            }

            // Kiểm tra Content-Type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("error", "Chỉ chấp nhận file hình ảnh!");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra file extension
            if (!fileExtension.equals(".jpg") && !fileExtension.equals(".jpeg") && 
                !fileExtension.equals(".png") && !fileExtension.equals(".gif") && 
                !fileExtension.equals(".webp") && !fileExtension.equals(".svg")) {
                response.put("error", "Chỉ chấp nhận định dạng ảnh (jpg, jpeg, png, gif, webp, svg)!");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra nội dung thực tế của file ảnh
            if (!fileExtension.equals(".svg")) {
                try (java.io.InputStream is = file.getInputStream()) {
                    java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(is);
                    if (bi == null) {
                        response.put("error", "Nội dung file không phải là hình ảnh hợp lệ (File bị giả mạo hoặc lỗi)!");
                        return ResponseEntity.badRequest().body(response);
                    }
                }
            } else {
                try (java.io.InputStream is = file.getInputStream()) {
                    byte[] bytes = is.readNBytes(1000);
                    String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    if (!content.contains("<svg") && !content.contains("svg")) {
                        response.put("error", "Nội dung SVG không hợp lệ!");
                        return ResponseEntity.badRequest().body(response);
                    }
                }
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
