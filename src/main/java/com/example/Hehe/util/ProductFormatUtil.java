package com.example.Hehe.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductFormatUtil {

    private static final List<String> VALID_CAPACITIES = Arrays.asList("16", "32", "64", "128", "256", "512", "1", "2");

    public static String formatProductName(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }

        // 1. Chuẩn hóa khoảng trắng
        String normalized = input.replaceAll("[\u200B-\u200D\uFEFF]", "").replaceAll("\\s+", " ").trim();

        // 2. Tách dung lượng (Capacity) ở cuối chuỗi
        // Tìm cụm số theo sau là khoảng trắng (tuỳ chọn) và chữ g, gb, t, tb ở cuối
        Pattern capacityPattern = Pattern.compile("(?i)(\\d+)\\s*(g|gb|t|tb)$");
        Matcher matcher = capacityPattern.matcher(normalized);

        String capacity = "";
        String modelName = normalized;

        if (matcher.find()) {
            String capacityNumber = matcher.group(1);
            String capacityUnit = matcher.group(2).toLowerCase();

            // Validate số dung lượng
            if (!VALID_CAPACITIES.contains(capacityNumber)) {
                throw new IllegalArgumentException("Dung lượng không hợp lệ! iPhone chỉ có bản 64GB, 128GB, 256GB, 512GB, 1TB, 2TB.");
            }

            // Chuẩn hóa đơn vị
            if (capacityUnit.startsWith("t")) {
                capacity = capacityNumber + "TB";
            } else {
                capacity = capacityNumber + "GB";
            }

            // Cắt phần model name ra (bỏ phần dung lượng ở cuối)
            modelName = normalized.substring(0, matcher.start()).trim();
        } else {
            // Theo yêu cầu thì dung lượng là bắt buộc để tự sinh ra name chuẩn.
            throw new IllegalArgumentException("Vui lòng nhập dung lượng hợp lệ (VD: 128GB) ở cuối tên sản phẩm.");
        }

        // 3. Chuẩn hóa tên Model (Title Case)
        // Chữa lỗi viết liền
        modelName = modelName.replaceAll("(?i)promax", "Pro Max");
        
        String[] words = modelName.split("\\s+");
        StringBuilder formattedModel = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            
            // Ép từ khóa đầu tiên (iphone) thành iPhone
            if (i == 0 && word.equals("iphone")) {
                formattedModel.append("iPhone");
            } else if (word.equals("pro") || word.equals("max") || word.equals("plus") || word.equals("mini") || word.equals("se")) {
                // Các từ khóa đặc biệt luôn viết hoa chữ cái đầu (đề phòng regex bắt trượt do viết tách)
                formattedModel.append(capitalizeWord(word));
            } else {
                // Title case bình thường
                formattedModel.append(capitalizeWord(word));
            }
            
            if (i < words.length - 1) {
                formattedModel.append(" ");
            }
        }

        // 4. Nối lại Tên + Dung lượng
        return formattedModel.toString() + " " + capacity;
    }

    private static String capitalizeWord(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }

    public static String validateAndFormatProductName(String rawName, String categoryName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }
        
        String tempName = rawName.replaceAll("[\u200B-\u200D\uFEFF]", "").replaceAll("\\s+", " ").trim();
        String catNameLower = categoryName != null ? categoryName.toLowerCase() : "";

        if (catNameLower.contains("iphone")) {
            // 1. Chặn ký tự đặc biệt (Lách luật): Dọn dẹp hết dấu gạch ngang, chấm, gạch dưới thành khoảng trắng
            String sanitizedName = tempName.replaceAll("[-._]", " ").replaceAll("\\s+", " ").trim();
            String lowerName = sanitizedName.toLowerCase();
            
            // 2. Chặn đảo lộn vị trí: Bắt đầu bằng chữ iPhone
            if (!lowerName.startsWith("iphone")) {
                throw new IllegalArgumentException("Sản phẩm thuộc danh mục iPhone bắt buộc phải có chữ 'iPhone' đứng đầu tiên trong tên.");
            }
            
            // Lấy các từ trong chuỗi để dễ xử lý
            String[] words = lowerName.split("\\s+");
            
            String example = "iPhone 11 64GB";
            if (catNameLower.contains("iphone pro max")) {
                example = "iPhone 11 Pro Max 256GB";
            } else if (catNameLower.contains("iphone pro")) {
                example = "iPhone 11 Pro 128GB";
            }

            // 3. Chặn thiếu đời máy & Xuyên không
            if (words.length < 2) {
                throw new IllegalArgumentException("Vui lòng nhập đầy đủ đời máy và dung lượng hợp lệ (VD: " + example + ").");
            }
            
            String modelWord = words[1];
            List<String> validModels = Arrays.asList("7", "8", "x", "xr", "xs", "se", "11", "12", "13", "14", "15", "16");
            if (!validModels.contains(modelWord)) {
                throw new IllegalArgumentException("Cửa hàng hiện chỉ kinh doanh các dòng máy từ iPhone 7 trở lên. Đời máy '" + modelWord.toUpperCase() + "' không được hỗ trợ (Các đời được phép: " + String.join(", ", validModels).toUpperCase() + ").");
            }
            
            // 4. Kiểm tra chéo với danh mục
            boolean hasPro = lowerName.contains("pro");
            boolean hasMax = lowerName.contains("max");
            boolean hasPlus = lowerName.contains("plus");
            boolean hasMini = lowerName.contains("mini");
            boolean hasSE = Arrays.asList(words).contains("se") || modelWord.equals("se");
            boolean hasXS = modelWord.equals("xs");
            
            List<String> invalidProModels = Arrays.asList("7", "8", "x", "xr", "se");

            if (catNameLower.contains("iphone thường")) {
                if (hasPro || hasMax || hasPlus || hasMini || hasSE || hasXS) {
                    throw new IllegalArgumentException("Danh mục 'iPhone thường' chỉ dành cho dòng máy tiêu chuẩn (VD: iPhone 11 128GB, iPhone XR 64GB). Sản phẩm có chứa từ khóa (Pro, Max, Plus, Mini, SE, XS) vui lòng chọn danh mục khác.");
                }
            } else if (catNameLower.contains("iphone pro max")) {
                if (invalidProModels.contains(modelWord)) {
                    throw new IllegalArgumentException("Apple không sản xuất phiên bản Pro/Pro Max cho dòng máy '" + modelWord.toUpperCase() + "'. Vui lòng kiểm tra lại (Các bản cao cấp này chỉ bắt đầu từ đời XS và 11 trở lên).");
                }
                boolean isXSMax = hasXS && hasMax;
                boolean isNormalProMax = hasPro && hasMax;
                if (!isXSMax && !isNormalProMax) {
                    throw new IllegalArgumentException("Sản phẩm thuộc danh mục 'iPhone Pro Max' bắt buộc phải là dòng 'Pro Max' hoặc 'XS Max' (VD: " + example + ", iPhone XS Max 256GB).");
                }
            } else if (catNameLower.contains("iphone pro")) {
                if (invalidProModels.contains(modelWord)) {
                    throw new IllegalArgumentException("Apple không sản xuất phiên bản Pro/Pro Max cho dòng máy '" + modelWord.toUpperCase() + "'. Vui lòng kiểm tra lại (Các bản cao cấp này chỉ bắt đầu từ đời XS và 11 trở lên).");
                }
                boolean isXSPro = hasXS && !hasMax;
                boolean isNormalPro = hasPro && !hasMax;
                
                if (!isXSPro && !isNormalPro) {
                    if (hasMax) {
                        throw new IllegalArgumentException("Sản phẩm có chữ 'Max' vui lòng chọn danh mục 'iPhone Pro Max'.");
                    } else {
                        throw new IllegalArgumentException("Sản phẩm thuộc danh mục 'iPhone Pro' bắt buộc phải là dòng 'Pro' hoặc 'XS' (VD: " + example + ", iPhone XS 64GB).");
                    }
                }
            }
            
            // Sau khi qua vòng gác cửa, gọi hàm chuẩn hóa cũ
            return formatProductName(sanitizedName);
        } else {
            // Không phải iPhone -> chỉ trả về chuỗi đã được dọn dẹp khoảng trắng
            return tempName;
        }
    }
}
