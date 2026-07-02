package com.example.Hehe.service;

import com.example.Hehe.model.*;
import com.example.Hehe.repository.ReceiptRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceExcelService {

    private final ReceiptRepository receiptRepository;

    public InvoiceExcelService(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    /**
     * Xuất file Excel danh sách hóa đơn (Loại 1 – 1 dòng/1 phiếu).
     * Phân quyền:
     *  - ADMIN: xuất toàn bộ hệ thống (có thể lọc theo chi nhánh nếu branchId != null)
     *  - MANAGER: chỉ xuất chi nhánh của mình
     *  - STAFF: ném AccessDeniedException
     *
     * @param currentUser Người đang đăng nhập
     * @param startDate   Từ ngày (nullable = không giới hạn)
     * @param endDate     Đến ngày (nullable = không giới hạn)
     * @return byte[] nội dung file .xlsx
     */
    public byte[] exportInvoiceList(User currentUser,
                                    LocalDateTime startDate,
                                    LocalDateTime endDate,
                                    Integer targetBranchId) {

        if (currentUser.getRole() == UserRole.STAFF) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Nhân viên không có quyền xuất danh sách hóa đơn.");
        }

        // Lấy danh sách phiếu EXPORT đã hoàn thành
        List<Receipt> receipts = fetchReceipts(currentUser, startDate, endDate, targetBranchId);

        if (receipts.isEmpty()) {
            throw new RuntimeException("Không có dữ liệu hóa đơn nào trong khoảng thời gian này.");
        }

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Danh sách hóa đơn");

            // ── Styles ──────────────────────────────────────────────────────
            CellStyle titleStyle   = makeTitleStyle(wb);
            CellStyle subStyle     = makeSubStyle(wb);
            CellStyle headerStyle  = makeHeaderStyle(wb);
            CellStyle dataStyle    = makeDataStyle(wb);
            CellStyle centerStyle  = makeCenterStyle(wb);
            CellStyle moneyStyle   = makeMoneyStyle(wb);
            CellStyle totalStyle   = makeTotalStyle(wb);
            CellStyle totalMoneyStyle = makeTotalMoneyStyle(wb);
            CellStyle totalRevStyle   = makeTotalRevenueStyle(wb);
            CellStyle totalPaidStyle  = makeTotalPaidStyle(wb);
            CellStyle totalDebtStyle  = makeTotalDebtStyle(wb);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String now = LocalDateTime.now().format(dtf);
            String reporter = currentUser.getFullName();
            String branchScope;
            if (currentUser.getRole() == UserRole.ADMIN) {
                branchScope = targetBranchId != null ? "Chi nhánh ID: " + targetBranchId : "Toàn hệ thống";
            } else {
                branchScope = currentUser.getBranch() != null ? currentUser.getBranch().getName() : "N/A";
            }

            // ── Header block (rows 0-3) ──────────────────────────────────
            int rowIdx = 0;

            Row r0 = sheet.createRow(rowIdx++);
            r0.setHeightInPoints(24);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("DANH SÁCH HÓA ĐƠN BÁN HÀNG");
            c0.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            Row r1 = sheet.createRow(rowIdx++);
            Cell c1 = r1.createCell(0);
            String rangeText = "Thời gian: " +
                    (startDate != null ? startDate.format(dateFmt) : "Đầu hệ thống") +
                    " → " +
                    (endDate != null ? endDate.format(dateFmt) : "Hiện tại") +
                    "   |   Chi nhánh: " + branchScope;
            c1.setCellValue(rangeText);
            c1.setCellStyle(subStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));

            Row r2 = sheet.createRow(rowIdx++);
            Cell c2 = r2.createCell(0);
            c2.setCellValue("Xuất bởi: " + reporter + "   |   Ngày xuất: " + now);
            c2.setCellStyle(subStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 8));

            rowIdx++; // blank row

            // ── Column headers (row 4) ───────────────────────────────────
            String[] headers = {"STT", "Mã Hóa Đơn", "Ngày Lập Hóa Đơn", "Khách Hàng",
                    "Chi Nhánh", "Người Tạo", "Tổng Tiền (VNĐ)",
                    "Trạng Thái Thanh Toán", "Ghi Chú"};
            Row headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeightInPoints(18);
            for (int i = 0; i < headers.length; i++) {
                Cell hc = headerRow.createCell(i);
                hc.setCellValue(headers[i]);
                hc.setCellStyle(headerStyle);
            }

            // ── Data rows ────────────────────────────────────────────────
            int stt = 1;
            BigDecimal totalRevenue = BigDecimal.ZERO;   // tất cả phiếu
            BigDecimal totalPaid    = BigDecimal.ZERO;   // chỉ PAID
            BigDecimal totalDebt    = BigDecimal.ZERO;   // UNPAID + PARTIAL

            for (Receipt receipt : receipts) {
                BigDecimal amount = calcTotal(receipt);
                totalRevenue = totalRevenue.add(amount);

                String ps = receipt.getPaymentStatus();
                if ("PAID".equalsIgnoreCase(ps)) {
                    totalPaid = totalPaid.add(amount);
                } else {
                    totalDebt = totalDebt.add(amount);
                }

                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(16);

                setCellCenter(row, 0, stt++, centerStyle);
                setCell(row, 1, receipt.getCode(), dataStyle);
                setCell(row, 2, receipt.getCreatedAt() != null ? receipt.getCreatedAt().format(dtf) : "", dataStyle);
                setCell(row, 3, receipt.getCustomerName() != null ? receipt.getCustomerName() : "Khách lẻ", dataStyle);
                setCell(row, 4, receipt.getSourceBranch() != null ? receipt.getSourceBranch().getName() : "", dataStyle);
                setCell(row, 5, receipt.getCreatedBy() != null ? receipt.getCreatedBy().getFullName() : "", dataStyle);
                setCellMoney(row, 6, amount.doubleValue(), moneyStyle);
                setCell(row, 7, translatePayment(receipt.getPaymentStatus()), centerStyle);
                setCell(row, 8, receipt.getDescription() != null ? receipt.getDescription() : "", dataStyle);
            }

            // Blank row before totals
            rowIdx++;

            // ── Totals row (3 chỉ số tài chính) ────────────────────────
            // Row: Tổng Doanh Thu
            Row tr1 = sheet.createRow(rowIdx++);
            Cell tr1label = tr1.createCell(4);
            tr1label.setCellValue("TỔNG DOANH THU (tất cả phiếu):");
            tr1label.setCellStyle(totalRevStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 4, 5));
            Cell tr1val = tr1.createCell(6);
            tr1val.setCellValue(totalRevenue.doubleValue());
            tr1val.setCellStyle(totalMoneyStyle);

            // Row: Tổng Thực Thu
            Row tr2 = sheet.createRow(rowIdx++);
            Cell tr2label = tr2.createCell(4);
            tr2label.setCellValue("TỔNG THỰC THU (đã thanh toán):");
            tr2label.setCellStyle(totalPaidStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 4, 5));
            Cell tr2val = tr2.createCell(6);
            tr2val.setCellValue(totalPaid.doubleValue());
            tr2val.setCellStyle(totalMoneyStyle);

            // Row: Tổng Công Nợ
            Row tr3 = sheet.createRow(rowIdx++);
            Cell tr3label = tr3.createCell(4);
            tr3label.setCellValue("TỔNG CÔNG NỢ (chưa thu hồi):");
            tr3label.setCellStyle(totalDebtStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 4, 5));
            Cell tr3val = tr3.createCell(6);
            tr3val.setCellValue(totalDebt.doubleValue());
            tr3val.setCellStyle(totalMoneyStyle);

            // ── Column widths ────────────────────────────────────────────
            int[] widths = {8, 22, 22, 30, 25, 25, 22, 26, 35};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            wb.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo file Excel: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lấy dữ liệu theo phân quyền và khoảng thời gian
    // ─────────────────────────────────────────────────────────────────────────
    private List<Receipt> fetchReceipts(User currentUser, LocalDateTime startDate, LocalDateTime endDate, Integer targetBranchId) {
        List<Receipt> all;
        if (currentUser.getRole() == UserRole.ADMIN) {
            if (targetBranchId != null) {
                all = receiptRepository.findByTypeAndStatusAndSourceBranchId(ReceiptType.EXPORT, ReceiptStatus.COMPLETED, targetBranchId);
            } else {
                all = receiptRepository.findByTypeAndStatus(ReceiptType.EXPORT, ReceiptStatus.COMPLETED);
            }
        } else {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : -1;
            all = receiptRepository.findByTypeAndStatusAndSourceBranchId(
                    ReceiptType.EXPORT, ReceiptStatus.COMPLETED, myBranchId);
        }

        // Lọc theo thời gian
        return all.stream()
                .filter(r -> {
                    LocalDateTime created = r.getCreatedAt();
                    if (created == null) return false;
                    if (startDate != null && created.isBefore(startDate)) return false;
                    if (endDate   != null && created.isAfter(endDate))    return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calcTotal(Receipt r) {
        return r.getDetails().stream()
                .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private String translateStatus(ReceiptStatus s) {
        if (s == null) return "";
        switch (s) {
            case COMPLETED: return "Hoàn thành";
            case CANCELLED: return "Đã hủy";
            default:        return s.name();
        }
    }

    private String translatePayment(String ps) {
        if (ps == null) return "";
        switch (ps.toUpperCase()) {
            case "PAID":    return "Đã thanh toán";
            case "UNPAID":  return "Chưa thanh toán";
            case "PARTIAL": return "Thanh toán 1 phần";
            default:        return ps;
        }
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private void setCellCenter(Row row, int col, int value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private void setCellMoney(Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Style factories
    // ─────────────────────────────────────────────────────────────────────────
    private CellStyle makeTitleStyle(Workbook wb) {
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)16);
        CellStyle s = wb.createCellStyle(); s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle makeSubStyle(Workbook wb) {
        Font f = wb.createFont(); f.setItalic(true); f.setFontHeightInPoints((short)10);
        CellStyle s = wb.createCellStyle(); s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle makeHeaderStyle(Workbook wb) {
        Font f = wb.createFont(); f.setBold(true);
        CellStyle s = wb.createCellStyle(); s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderTop(BorderStyle.THIN); s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN); s.setBorderRight(BorderStyle.THIN);
        s.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setWrapText(true);
        return s;
    }

    private CellStyle makeDataStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderTop(BorderStyle.THIN); s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN); s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private CellStyle makeCenterStyle(Workbook wb) {
        CellStyle s = makeDataStyle(wb);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle makeMoneyStyle(Workbook wb) {
        DataFormat fmt = wb.createDataFormat();
        CellStyle s = makeDataStyle(wb);
        s.setDataFormat(fmt.getFormat("#,##0"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private CellStyle makeTotalStyle(Workbook wb) {
        Font f = wb.createFont(); f.setBold(true);
        CellStyle s = wb.createCellStyle(); s.setFont(f);
        s.setBorderTop(BorderStyle.MEDIUM); s.setBorderBottom(BorderStyle.MEDIUM);
        s.setBorderLeft(BorderStyle.MEDIUM); s.setBorderRight(BorderStyle.MEDIUM);
        return s;
    }

    private CellStyle makeTotalMoneyStyle(Workbook wb) {
        DataFormat fmt = wb.createDataFormat();
        CellStyle s = makeTotalStyle(wb);
        s.setDataFormat(fmt.getFormat("#,##0"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private CellStyle makeTotalRevenueStyle(Workbook wb) {
        Font f = wb.createFont(); f.setBold(true);
        CellStyle s = wb.createCellStyle(); s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderTop(BorderStyle.MEDIUM); s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.MEDIUM); s.setBorderRight(BorderStyle.MEDIUM);
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private CellStyle makeTotalPaidStyle(Workbook wb) {
        Font f = wb.createFont(); f.setBold(true);
        f.setColor(IndexedColors.DARK_GREEN.getIndex());
        CellStyle s = wb.createCellStyle(); s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderTop(BorderStyle.THIN); s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.MEDIUM); s.setBorderRight(BorderStyle.MEDIUM);
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private CellStyle makeTotalDebtStyle(Workbook wb) {
        Font f = wb.createFont(); f.setBold(true);
        f.setColor(IndexedColors.DARK_RED.getIndex());
        CellStyle s = wb.createCellStyle(); s.setFont(f);
        s.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderTop(BorderStyle.THIN); s.setBorderBottom(BorderStyle.MEDIUM);
        s.setBorderLeft(BorderStyle.MEDIUM); s.setBorderRight(BorderStyle.MEDIUM);
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }
}
