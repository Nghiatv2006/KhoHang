package com.example.Hehe.service;

import com.example.Hehe.model.*;
import com.example.Hehe.repository.ReceiptRepository;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoicePdfService {

    private final ReceiptRepository receiptRepository;



    public InvoicePdfService(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    /**
     * Xuất PDF hóa đơn in nhiệt 80mm cho một phiếu xuất.
     * Áp dụng đóng dấu trạng thái thanh toán (PAID / UNPAID / PARTIAL).
     *
     * @param receiptId  ID phiếu xuất
     * @param currentUser Người đang đăng nhập (để kiểm tra quyền)
     * @return byte[] nội dung file PDF
     */
    public byte[] exportReceiptPdf(Integer receiptId, User currentUser) {

        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay phieu ID: " + receiptId));

        // Kiểm tra quyền: STAFF chỉ xem được phiếu của chi nhánh mình
        if (currentUser.getRole() == UserRole.STAFF) {
            Integer userBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            Integer receiptBranchId = receipt.getSourceBranch() != null ? receipt.getSourceBranch().getId() : null;
            if (userBranchId == null || !userBranchId.equals(receiptBranchId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Ban khong co quyen truy cap phieu nay.");
            }
        }

        String html = buildHtml(receipt);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // Đăng ký font nhúng trong project để hỗ trợ tiếng Việt
            registerFonts(builder);

            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Loi khi tao file PDF: " + e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Đăng ký font Roboto từ file trong resource để tương thích đa nền tảng
    // ──────────────────────────────────────────────────────────────
    private void registerFonts(PdfRendererBuilder builder) {
        try {
            File regFile = copyToTempFile("/fonts/Roboto-Regular.ttf", "roboto-reg");
            if (regFile != null) {
                builder.useFont(regFile, "Roboto", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
            }
            File boldFile = copyToTempFile("/fonts/Roboto-Bold.ttf", "roboto-bold");
            if (boldFile != null) {
                builder.useFont(boldFile, "Roboto", 700, BaseRendererBuilder.FontStyle.NORMAL, true);
            }
        } catch (Exception e) {
            System.err.println("Không thể đăng ký font Roboto: " + e.getMessage());
        }
    }

    private File copyToTempFile(String path, String prefix) {
        try (java.io.InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return null;
            File temp = File.createTempFile(prefix, ".ttf");
            temp.deleteOnExit();
            java.nio.file.Files.copy(is, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return temp;
        } catch (Exception e) {
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Xây dựng HTML từ dữ liệu hóa đơn
    // ─────────────────────────────────────────────────────────────────────────
    private String buildHtml(Receipt receipt) {

        Branch branch = receipt.getSourceBranch();
        String shopName  = "WAREHUB";
        String branchName = branch != null ? branch.getName().toUpperCase() : "CHI NHÁNH";
        String branchAddr = branch != null ? branch.getAddress() : "";

        String customerName  = receipt.getCustomerName()  != null ? receipt.getCustomerName()  : "Khách lẻ";
        String customerPhone = receipt.getCustomerPhone() != null ? receipt.getCustomerPhone() : "";
        String createdBy     = receipt.getCreatedBy()     != null ? receipt.getCreatedBy().getFullName() : "";
        String description   = receipt.getDescription()   != null ? receipt.getDescription()   : "";

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String createdAt = receipt.getCreatedAt() != null
                ? receipt.getCreatedAt().format(dtf) : "";
        String printedAt = LocalDateTime.now().format(dtf);

        // Tính tổng tiền
        List<ReceiptDetail> details = receipt.getDetails();
        BigDecimal grandTotal = details.stream()
                .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Các dòng sản phẩm
        StringBuilder rows = new StringBuilder();
        for (ReceiptDetail d : details) {
            BigDecimal subtotal = d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity()));
            String productName  = d.getProduct() != null ? escHtml(d.getProduct().getName()) : "—";
            rows.append("<tr>")
                .append("<td>")
                .append("<div class=\"product-name\">").append(productName).append("</div>")
                .append("<div class=\"product-detail\">").append(formatMoney(d.getPrice())).append("đ / cái</div>")
                .append("</td>")
                .append("<td class=\"right\">").append(d.getQuantity()).append("</td>")
                .append("<td class=\"right\">").append(formatMoney(subtotal)).append("đ</td>")
                .append("</tr>\n");
        }

        // Đóng dấu thanh toán
        String paymentStamp = buildPaymentStamp(receipt.getPaymentStatus(), grandTotal);

        // Ghi chú
        String noteHtml = "";
        if (!description.isEmpty()) {
            noteHtml = "<div style=\"margin-bottom:3px;font-style:italic;\">Ghi chú: "
                    + escHtml(description) + "</div>";
        }

        // Phone row
        String phoneRow = "";
        if (!customerPhone.isEmpty()) {
            phoneRow = "<div class=\"info-row\"><span class=\"label\">SĐT:</span>"
                    + "<span class=\"value\">" + escHtml(customerPhone) + "</span></div>";
        }

        return "<!DOCTYPE html>\n<html lang=\"vi\">\n<head>\n" +
                "<meta charset=\"UTF-8\"/>\n" +
                "<style>" + STYLE_CSS + "</style>\n" +
                "</head>\n<body>\n" +
                // Header
                "<div class=\"header\">" +
                "<div class=\"shop-name\">" + escHtml(shopName) + "</div>" +
                "<div class=\"branch-name\">" + escHtml(branchName) + "</div>" +
                "<div class=\"contact\">" + escHtml(branchAddr) + "</div>" +
                "<div class=\"title\">Hóa đơn bán hàng</div>" +
                "</div>\n" +
                // Info
                "<div class=\"info-block\">" +
                "<div class=\"info-row\"><span class=\"label\">Mã phiếu:</span><span class=\"value\">" + escHtml(receipt.getCode()) + "</span></div>" +
                "<div class=\"info-row\"><span class=\"label\">Ngày giờ:</span><span class=\"value\">" + createdAt + "</span></div>" +
                "<div class=\"info-row\"><span class=\"label\">Khách hàng:</span><span class=\"value\">" + escHtml(customerName) + "</span></div>" +
                phoneRow +
                "<div class=\"info-row\"><span class=\"label\">Nhân viên:</span><span class=\"value\">" + escHtml(createdBy) + "</span></div>" +
                "</div>\n" +
                "<hr class=\"separator\"/>\n" +
                // Table
                "<table class=\"product-table\"><thead><tr>" +
                "<th style=\"width:45%\">Sản phẩm</th>" +
                "<th class=\"right\" style=\"width:18%\">SL</th>" +
                "<th class=\"right\" style=\"width:37%\">Thành tiền</th>" +
                "</tr></thead><tbody>\n" + rows + "</tbody></table>\n" +
                "<hr class=\"separator\"/>\n" +
                // Totals
                "<div class=\"totals\">" +
                "<div class=\"row\"><span class=\"lbl\">Tổng số mặt hàng:</span><span class=\"val\">" + details.size() + "</span></div>" +
                "<div class=\"row grand-total\"><span>TỔNG CỘNG:</span><span>" + formatMoney(grandTotal) + "đ</span></div>" +
                "</div>\n" +
                // Stamp
                paymentStamp +
                // Footer
                "<div class=\"footer\">" + noteHtml +
                "<div>Cảm ơn Quý khách &amp; Hẹn gặp lại!</div>" +
                "<div style=\"margin-top:3px;\">In lúc: " + printedAt + "</div>" +
                "</div>\n" +
                "</body></html>";
    }

    private String buildPaymentStamp(String paymentStatus, BigDecimal grandTotal) {
        if (paymentStatus == null) paymentStatus = "UNPAID";
        switch (paymentStatus.toUpperCase()) {
            case "PAID":
                return "<div class=\"stamp-paid\">&#10004; ĐÃ THANH TOÁN</div>\n";
            case "PARTIAL":
                return "<div class=\"stamp-partial\">&#9888; THANH TOÁN MỘT PHẦN"
                        + "<div class=\"debt-amount\">Vui lòng thanh toán phần còn lại</div>"
                        + "</div>\n";
            case "UNPAID":
            default:
                return "<div class=\"stamp-unpaid\">&#9888; CHƯA THANH TOÁN"
                        + "<div class=\"debt-amount\">Số tiền còn nợ: " + formatMoney(grandTotal) + "đ</div>"
                        + "</div>\n";
        }
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }

    private String escHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    // ──────────────────────────────────────────────────────────────
    // CSS nhúng thẳng vào HTML — dùng font Roboto được đăng ký ở trên
    // ──────────────────────────────────────────────────────────────
    private static final String STYLE_CSS =
        "@page { size: 80mm auto; margin: 4mm 4mm 6mm 4mm; }" +
        "* { box-sizing: border-box; margin: 0; padding: 0; }" +
        "body { font-family: Roboto, Arial, sans-serif; font-size: 11px; color: #000; width: 72mm; }" +
        ".header { text-align: center; margin-bottom: 6px; }" +
        ".header .shop-name { font-size: 15px; font-weight: bold; text-transform: uppercase; }" +
        ".header .branch-name { font-size: 11px; font-weight: bold; }" +
        ".header .contact { font-size: 10px; color: #333; }" +
        ".header .title { font-size: 14px; font-weight: bold; text-transform: uppercase; margin-top: 6px; border-top: 1px dashed #000; border-bottom: 1px dashed #000; padding: 3px 0; }" +
        ".info-block { margin: 5px 0; font-size: 10.5px; }" +
        ".info-row { display: flex; justify-content: space-between; margin-bottom: 2px; }" +
        ".info-row .label { color: #555; min-width: 42%; }" +
        ".info-row .value { font-weight: bold; text-align: right; flex: 1; }" +
        ".separator { border: none; border-top: 1px dashed #000; margin: 5px 0; }" +
        ".product-table { width: 100%; border-collapse: collapse; font-size: 10.5px; margin: 3px 0; }" +
        ".product-table th { font-weight: bold; text-align: left; padding: 2px 1px; border-bottom: 1px solid #000; }" +
        ".product-table th.right, .product-table td.right { text-align: right; }" +
        ".product-table td { padding: 2px 1px; vertical-align: top; }" +
        ".product-name { font-weight: bold; }" +
        ".product-detail { font-size: 10px; color: #333; }" +
        ".product-table tr:nth-child(even) td { background-color: #f7f7f7; }" +
        ".totals { margin-top: 5px; font-size: 10.5px; }" +
        ".totals .row { display: flex; justify-content: space-between; margin-bottom: 2px; }" +
        ".totals .row .lbl { color: #333; }" +
        ".totals .row .val { font-weight: bold; }" +
        ".totals .grand-total { font-size: 13px; font-weight: bold; border-top: 1px dashed #000; padding-top: 3px; margin-top: 3px; }" +
        ".stamp-unpaid { border: 2px solid #cc0000; border-radius: 4px; color: #cc0000; font-size: 12px; font-weight: bold; text-align: center; padding: 4px 0; margin: 6px 0; }" +
        ".stamp-unpaid .debt-amount { font-size: 10.5px; font-weight: normal; margin-top: 2px; }" +
        ".stamp-paid { border: 2px solid #006600; border-radius: 4px; color: #006600; font-size: 12px; font-weight: bold; text-align: center; padding: 4px 0; margin: 6px 0; }" +
        ".stamp-partial { border: 2px solid #cc6600; border-radius: 4px; color: #cc6600; font-size: 12px; font-weight: bold; text-align: center; padding: 4px 0; margin: 6px 0; }" +
        ".stamp-partial .debt-amount { font-size: 10.5px; font-weight: normal; margin-top: 2px; }" +
        ".footer { text-align: center; font-size: 10px; color: #444; margin-top: 6px; padding-top: 5px; border-top: 1px dashed #000; }";
}
