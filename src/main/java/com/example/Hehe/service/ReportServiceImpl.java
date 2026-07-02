package com.example.Hehe.service;

import com.example.Hehe.dto.CustomerResponse;
import com.example.Hehe.model.*;
import com.example.Hehe.repository.BranchRepository;
import com.example.Hehe.repository.InventoryRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final CustomerService customerService;
    private final com.example.Hehe.repository.ReceiptRepository receiptRepository;
    private final com.example.Hehe.repository.StocktakeRepository stocktakeRepository;

    public ReportServiceImpl(InventoryRepository inventoryRepository, BranchRepository branchRepository, CustomerService customerService, com.example.Hehe.repository.ReceiptRepository receiptRepository, com.example.Hehe.repository.StocktakeRepository stocktakeRepository) {
        this.inventoryRepository = inventoryRepository;
        this.branchRepository = branchRepository;
        this.customerService = customerService;
        this.receiptRepository = receiptRepository;
        this.stocktakeRepository = stocktakeRepository;
    }

    @Override
    public byte[] exportInventoryToExcel(Integer targetBranchId, User currentUser) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new AccessDeniedException("Staff is not allowed to export inventory reports.");
        }
        if (currentUser.getRole() == UserRole.MANAGER) {
            if (!currentUser.getBranch().getId().equals(targetBranchId)) {
                throw new AccessDeniedException("Manager can only export reports for their own branch.");
            }
        }

        Branch branch = branchRepository.findById(targetBranchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

        List<Inventory> inventories = inventoryRepository.findByBranchId(targetBranchId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo Cáo Tồn Kho");

            // Fonts and Styles
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            Font italicFont = workbook.createFont();
            italicFont.setItalic(true);

            CellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldFont);

            CellStyle boldCenterStyle = workbook.createCellStyle();
            boldCenterStyle.setFont(boldFont);
            boldCenterStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle italicStyle = workbook.createCellStyle();
            italicStyle.setFont(italicFont);
            italicStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            CellStyle borderBoldStyle = workbook.createCellStyle();
            borderBoldStyle.cloneStyleFrom(borderStyle);
            borderBoldStyle.setFont(boldFont);

            CellStyle borderCenterStyle = workbook.createCellStyle();
            borderCenterStyle.cloneStyleFrom(borderStyle);
            borderCenterStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle borderBoldCenterStyle = workbook.createCellStyle();
            borderBoldCenterStyle.cloneStyleFrom(borderCenterStyle);
            borderBoldCenterStyle.setFont(boldFont);

            DataFormat format = workbook.createDataFormat();
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.cloneStyleFrom(borderStyle);
            moneyStyle.setDataFormat(format.getFormat("#,##0"));

            CellStyle boldMoneyStyle = workbook.createCellStyle();
            boldMoneyStyle.cloneStyleFrom(borderBoldStyle);
            boldMoneyStyle.setDataFormat(format.getFormat("#,##0"));
            
            CellStyle italicBorderStyle = workbook.createCellStyle();
            italicBorderStyle.cloneStyleFrom(borderStyle);
            italicBorderStyle.setFont(italicFont);

            CellStyle totalRowStyle = workbook.createCellStyle();
            totalRowStyle.cloneStyleFrom(borderBoldStyle);
            totalRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            totalRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            CellStyle totalRowMoneyStyle = workbook.createCellStyle();
            totalRowMoneyStyle.cloneStyleFrom(totalRowStyle);
            totalRowMoneyStyle.setDataFormat(format.getFormat("#,##0"));
            
            CellStyle totalRowCenterStyle = workbook.createCellStyle();
            totalRowCenterStyle.cloneStyleFrom(totalRowStyle);
            totalRowCenterStyle.setAlignment(HorizontalAlignment.CENTER);
            
            CellStyle totalRowRightStyle = workbook.createCellStyle();
            totalRowRightStyle.cloneStyleFrom(totalRowStyle);
            totalRowRightStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Header Section
            Row r0 = sheet.createRow(0);
            Cell c00 = r0.createCell(0);
            c00.setCellValue("CÔNG TY TNHH WAREHUB");
            c00.setCellStyle(boldStyle);

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("Chi nhánh: " + branch.getName());

            Row r3 = sheet.createRow(3);
            Cell titleCell = r3.createCell(0);
            titleCell.setCellValue("BÁO CÁO TỔNG HỢP TỒN KHO");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 9));

            Row r4 = sheet.createRow(4);
            Cell infoCell = r4.createCell(0);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            infoCell.setCellValue("Ngày xuất: " + LocalDateTime.now().format(dtf) + " - Người xuất: " + currentUser.getFullName());
            infoCell.setCellStyle(italicStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 9));

            // Table Header
            String[] headers = {"STT", "Mã SP", "Tên Sản Phẩm", "Danh mục", "ĐVT", "Ngày SX", "Hạn SD", "Số lượng", "Đơn giá (VNĐ)", "Thành tiền (VNĐ)"};
            Row headerRow = sheet.createRow(6);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Group data by product
            Map<Product, List<Inventory>> groupedData = inventories.stream()
                    .collect(Collectors.groupingBy(Inventory::getProduct));

            int rowIdx = 7;
            int stt = 1;
            int grandTotalQty = 0;
            BigDecimal grandTotalAmount = BigDecimal.ZERO;

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (Map.Entry<Product, List<Inventory>> entry : groupedData.entrySet()) {
                Product product = entry.getKey();
                List<Inventory> lots = entry.getValue();

                int totalQty = lots.stream().mapToInt(Inventory::getQuantity).sum();
                BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(totalQty));

                grandTotalQty += totalQty;
                grandTotalAmount = grandTotalAmount.add(totalAmount);

                // Master Row
                Row mRow = sheet.createRow(rowIdx++);
                mRow.createCell(0).setCellValue(stt++);
                mRow.getCell(0).setCellStyle(borderBoldCenterStyle);
                mRow.createCell(1).setCellValue(product.getSku());
                mRow.getCell(1).setCellStyle(borderBoldStyle);
                mRow.createCell(2).setCellValue(product.getName());
                mRow.getCell(2).setCellStyle(borderBoldStyle);
                mRow.createCell(3).setCellValue(product.getCategory().getName());
                mRow.getCell(3).setCellStyle(borderBoldStyle);
                mRow.createCell(4).setCellValue(product.getUnit());
                mRow.getCell(4).setCellStyle(borderBoldCenterStyle);
                
                mRow.createCell(5).setCellStyle(borderBoldStyle);
                mRow.createCell(6).setCellStyle(borderBoldStyle);
                
                mRow.createCell(7).setCellValue(totalQty);
                mRow.getCell(7).setCellStyle(borderBoldCenterStyle);
                
                Cell priceCell = mRow.createCell(8);
                priceCell.setCellValue(product.getPrice().doubleValue());
                priceCell.setCellStyle(boldMoneyStyle);
                
                Cell amtCell = mRow.createCell(9);
                amtCell.setCellValue(totalAmount.doubleValue());
                amtCell.setCellStyle(boldMoneyStyle);

                // Detail Rows
                for (Inventory lot : lots) {
                    Row dRow = sheet.createRow(rowIdx++);
                    dRow.createCell(0).setCellStyle(borderStyle);
                    dRow.createCell(1).setCellStyle(borderStyle);
                    
                    Cell detailLabel = dRow.createCell(2);
                    detailLabel.setCellValue("  \u21B3 Chi tiết lô");
                    detailLabel.setCellStyle(italicBorderStyle);
                    
                    dRow.createCell(3).setCellStyle(borderStyle);
                    dRow.createCell(4).setCellStyle(borderStyle);
                    
                    Cell mfgCell = dRow.createCell(5);
                    mfgCell.setCellStyle(borderCenterStyle);
                    if (lot.getManufacturingDate() != null) {
                        mfgCell.setCellValue(lot.getManufacturingDate().format(dateFmt));
                    } else {
                        mfgCell.setCellValue("-");
                    }

                    Cell expCell = dRow.createCell(6);
                    expCell.setCellStyle(borderCenterStyle);
                    if (lot.getHasExpiry() && lot.getExpirationDate() != null) {
                        expCell.setCellValue(lot.getExpirationDate().format(dateFmt));
                    } else {
                        expCell.setCellValue("-");
                    }

                    Cell qtyCell = dRow.createCell(7);
                    qtyCell.setCellValue(lot.getQuantity());
                    qtyCell.setCellStyle(borderCenterStyle);
                    
                    dRow.createCell(8).setCellStyle(borderStyle);

                    Cell dAmtCell = dRow.createCell(9);
                    dAmtCell.setCellValue(product.getPrice().multiply(BigDecimal.valueOf(lot.getQuantity())).doubleValue());
                    dAmtCell.setCellStyle(moneyStyle);
                }
            }

            // Total Row
            Row tRow = sheet.createRow(rowIdx++);
            for(int i=0; i<=9; i++) {
                tRow.createCell(i).setCellStyle(totalRowStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 6));
            tRow.getCell(0).setCellValue("TỔNG KHO:");
            tRow.getCell(0).setCellStyle(totalRowRightStyle);
            
            tRow.getCell(7).setCellValue(grandTotalQty);
            tRow.getCell(7).setCellStyle(totalRowCenterStyle);
            
            tRow.getCell(9).setCellValue(grandTotalAmount.doubleValue());
            tRow.getCell(9).setCellStyle(totalRowMoneyStyle);

            // Footer Signatures
            rowIdx += 2;
            Row signLabelRow = sheet.createRow(rowIdx);
            Cell sig1 = signLabelRow.createCell(1);
            sig1.setCellValue("Người lập biểu");
            sig1.setCellStyle(boldCenterStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 1, 3));
            
            Cell sig2 = signLabelRow.createCell(4);
            sig2.setCellValue("Quản lý kho");
            sig2.setCellStyle(boldCenterStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 4, 6));
            
            Cell sig3 = signLabelRow.createCell(7);
            sig3.setCellValue("Giám đốc");
            sig3.setCellStyle(boldCenterStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 7, 9));
            
            rowIdx++;
            Row signSubRow = sheet.createRow(rowIdx);
            Cell sub1 = signSubRow.createCell(1);
            sub1.setCellValue("(Ký, họ tên)");
            sub1.setCellStyle(italicStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 1, 3));
            
            Cell sub2 = signSubRow.createCell(4);
            sub2.setCellValue("(Ký, họ tên)");
            sub2.setCellStyle(italicStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 4, 6));
            
            Cell sub3 = signSubRow.createCell(7);
            sub3.setCellValue("(Ký, đóng dấu, họ tên)");
            sub3.setCellStyle(italicStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 7, 9));

            // AutoSize Columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Excel report", e);
        }
    }

    @Override
    public byte[] exportCustomersToExcel(User currentUser) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new AccessDeniedException("Staff is not allowed to export customer reports.");
        }

        // Fetch all customers, already filtered by branch if user is Manager
        List<CustomerResponse> allCustomers = customerService.searchCustomers(null, null, currentUser);

        // Filter only those with debt > 0
        List<CustomerResponse> debtCustomers = allCustomers.stream()
                .filter(c -> c.getDebt() != null && c.getDebt().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo Cáo Công Nợ");

            // Fonts and Styles
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            Font italicFont = workbook.createFont();
            italicFont.setItalic(true);

            CellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldFont);

            CellStyle boldCenterStyle = workbook.createCellStyle();
            boldCenterStyle.setFont(boldFont);
            boldCenterStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle italicStyle = workbook.createCellStyle();
            italicStyle.setFont(italicFont);
            italicStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            CellStyle borderCenterStyle = workbook.createCellStyle();
            borderCenterStyle.cloneStyleFrom(borderStyle);
            borderCenterStyle.setAlignment(HorizontalAlignment.CENTER);

            DataFormat format = workbook.createDataFormat();
            
            CellStyle borderMoneyStyle = workbook.createCellStyle();
            borderMoneyStyle.cloneStyleFrom(borderStyle);
            borderMoneyStyle.setDataFormat(format.getFormat("#,##0"));
            
            // Highlight Style for Debt (Red text, Yellow BG)
            Font highlightFont = workbook.createFont();
            highlightFont.setBold(true);
            highlightFont.setColor(IndexedColors.RED.getIndex());
            
            CellStyle highlightMoneyStyle = workbook.createCellStyle();
            highlightMoneyStyle.cloneStyleFrom(borderMoneyStyle);
            highlightMoneyStyle.setFont(highlightFont);
            highlightMoneyStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            highlightMoneyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle totalRowStyle = workbook.createCellStyle();
            totalRowStyle.cloneStyleFrom(borderStyle);
            totalRowStyle.setFont(boldFont);
            totalRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            totalRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            CellStyle totalRowRightStyle = workbook.createCellStyle();
            totalRowRightStyle.cloneStyleFrom(totalRowStyle);
            totalRowRightStyle.setAlignment(HorizontalAlignment.RIGHT);
            
            CellStyle totalRowMoneyStyle = workbook.createCellStyle();
            totalRowMoneyStyle.cloneStyleFrom(totalRowStyle);
            totalRowMoneyStyle.setDataFormat(format.getFormat("#,##0"));
            totalRowMoneyStyle.setFont(highlightFont);

            // Header Section
            Row r0 = sheet.createRow(0);
            Cell c00 = r0.createCell(0);
            c00.setCellValue("CÔNG TY TNHH WAREHUB");
            c00.setCellStyle(boldStyle);

            Row r1 = sheet.createRow(1);
            if (currentUser.getRole() == UserRole.MANAGER) {
                r1.createCell(0).setCellValue("Chi nhánh: " + currentUser.getBranch().getName());
            } else {
                r1.createCell(0).setCellValue("Chi nhánh: TOÀN HỆ THỐNG");
            }

            Row r3 = sheet.createRow(3);
            Cell titleCell = r3.createCell(0);
            titleCell.setCellValue("BÁO CÁO CÔNG NỢ KHÁCH HÀNG");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 5));

            Row r4 = sheet.createRow(4);
            Cell infoCell = r4.createCell(0);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            infoCell.setCellValue("Ngày xuất: " + LocalDateTime.now().format(dtf) + " - Người xuất: " + currentUser.getFullName());
            infoCell.setCellStyle(italicStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 5));

            // Table Header
            String[] headers = {"STT", "Tên Khách Hàng", "Liên hệ", "Địa chỉ", "Trạng thái", "CÔNG NỢ (VNĐ)"};
            Row headerRow = sheet.createRow(6);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 7;
            int stt = 1;
            BigDecimal grandTotalDebt = BigDecimal.ZERO;

            for (CustomerResponse customer : debtCustomers) {
                Row dRow = sheet.createRow(rowIdx++);
                
                Cell sttCell = dRow.createCell(0);
                sttCell.setCellValue(stt++);
                sttCell.setCellStyle(borderCenterStyle);
                
                Cell nameCell = dRow.createCell(1);
                nameCell.setCellValue(customer.getName());
                nameCell.setCellStyle(borderStyle);
                
                Cell contactCell = dRow.createCell(2);
                contactCell.setCellValue(customer.getContactInfo() != null ? customer.getContactInfo() : "");
                contactCell.setCellStyle(borderStyle);
                
                Cell addressCell = dRow.createCell(3);
                addressCell.setCellValue(customer.getAddress() != null ? customer.getAddress() : "");
                addressCell.setCellStyle(borderStyle);
                
                Cell statusCell = dRow.createCell(4);
                statusCell.setCellValue(customer.getStatus());
                statusCell.setCellStyle(borderCenterStyle);
                
                Cell debtCell = dRow.createCell(5);
                BigDecimal debt = customer.getDebt() != null ? customer.getDebt() : BigDecimal.ZERO;
                grandTotalDebt = grandTotalDebt.add(debt);
                debtCell.setCellValue(debt.doubleValue());
                debtCell.setCellStyle(highlightMoneyStyle); // Always > 0 here, so highlight
            }

            // Total Row
            Row tRow = sheet.createRow(rowIdx++);
            for(int i=0; i<=5; i++) {
                tRow.createCell(i).setCellStyle(totalRowStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 4));
            tRow.getCell(0).setCellValue("TỔNG CỘNG CÔNG NỢ:");
            tRow.getCell(0).setCellStyle(totalRowRightStyle);
            
            tRow.getCell(5).setCellValue(grandTotalDebt.doubleValue());
            tRow.getCell(5).setCellStyle(totalRowMoneyStyle);

            // Footer Signatures
            rowIdx += 2;
            Row signLabelRow = sheet.createRow(rowIdx);
            
            Cell sig1 = signLabelRow.createCell(0);
            sig1.setCellValue("Người lập biểu");
            sig1.setCellStyle(boldCenterStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 1));
            
            Cell sig2 = signLabelRow.createCell(2);
            sig2.setCellValue("Kế toán trưởng");
            sig2.setCellStyle(boldCenterStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 2, 3));
            
            Cell sig3 = signLabelRow.createCell(4);
            sig3.setCellValue("Giám đốc");
            sig3.setCellStyle(boldCenterStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 4, 5));
            
            rowIdx++;
            Row signSubRow = sheet.createRow(rowIdx);
            
            Cell sub1 = signSubRow.createCell(0);
            sub1.setCellValue("(Ký, họ tên)");
            sub1.setCellStyle(italicStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 1));
            
            Cell sub2 = signSubRow.createCell(2);
            sub2.setCellValue("(Ký, họ tên)");
            sub2.setCellStyle(italicStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 2, 3));
            
            Cell sub3 = signSubRow.createCell(4);
            sub3.setCellValue("(Ký, đóng dấu, họ tên)");
            sub3.setCellStyle(italicStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 4, 5));

            // AutoSize Columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Customer Excel report", e);
        }
    }

    @Override
    public java.util.Map<String, Object> getDebtAgingAnalysis(User currentUser) {
        List<Receipt> unpaidReceipts;
        boolean isGlobalUser = currentUser.getRole() == UserRole.ADMIN || 
                               (currentUser.getBranch() != null && currentUser.getBranch().getId() == 1);
        if (!isGlobalUser) {
            unpaidReceipts = receiptRepository.findByTypeAndStatusAndPaymentStatusAndSourceBranchId(
                    ReceiptType.EXPORT, ReceiptStatus.COMPLETED, "UNPAID", currentUser.getBranch().getId());
        } else {
            unpaidReceipts = receiptRepository.findByTypeAndStatusAndPaymentStatus(
                    ReceiptType.EXPORT, ReceiptStatus.COMPLETED, "UNPAID");
        }

        BigDecimal inTermDebt = BigDecimal.ZERO; // <= 15 days
        BigDecimal warningDebt = BigDecimal.ZERO; // 16 - 30 days
        BigDecimal badDebt = BigDecimal.ZERO; // > 30 days

        LocalDateTime now = LocalDateTime.now();

        for (Receipt receipt : unpaidReceipts) {
            BigDecimal receiptTotal = receipt.getDetails().stream()
                    .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long days = java.time.temporal.ChronoUnit.DAYS.between(receipt.getCreatedAt(), now);

            if (days <= 15) {
                inTermDebt = inTermDebt.add(receiptTotal);
            } else if (days <= 30) {
                warningDebt = warningDebt.add(receiptTotal);
            } else {
                badDebt = badDebt.add(receiptTotal);
            }
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("inTermDebt", inTermDebt);
        result.put("warningDebt", warningDebt);
        result.put("badDebt", badDebt);
        return result;
    }

    @Override
    public java.util.Map<String, Object> getInventoryAgeAnalysis(User currentUser) {
        List<Inventory> inventories;
        boolean isGlobalUser = currentUser.getRole() == UserRole.ADMIN || 
                               (currentUser.getBranch() != null && currentUser.getBranch().getId() == 1);
        if (!isGlobalUser) {
            inventories = inventoryRepository.findByBranchId(currentUser.getBranch().getId());
        } else {
            inventories = inventoryRepository.findAll();
        }

        BigDecimal fastMovingValue = BigDecimal.ZERO; // <= 30 days
        BigDecimal slowMovingValue = BigDecimal.ZERO; // 31 - 90 days
        BigDecimal deadStockValue = BigDecimal.ZERO;  // > 90 days

        LocalDateTime now = LocalDateTime.now();

        for (Inventory inv : inventories) {
            if (inv.getQuantity() <= 0) continue;
            
            BigDecimal value = inv.getProduct().getPrice().multiply(BigDecimal.valueOf(inv.getQuantity()));
            long days = java.time.temporal.ChronoUnit.DAYS.between(inv.getLastUpdated(), now);
            
            if (days <= 30) {
                fastMovingValue = fastMovingValue.add(value);
            } else if (days <= 90) {
                slowMovingValue = slowMovingValue.add(value);
            } else {
                deadStockValue = deadStockValue.add(value);
            }
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("fastMoving", fastMovingValue);
        result.put("slowMoving", slowMovingValue);
        result.put("deadStock", deadStockValue);
        return result;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getStocktakeDiscrepancyHistory(User currentUser) {
        List<Stocktake> stocktakes;
        boolean isGlobalUser = currentUser.getRole() == UserRole.ADMIN || 
                               (currentUser.getBranch() != null && currentUser.getBranch().getId() == 1);
        if (!isGlobalUser) {
            stocktakes = stocktakeRepository.findByBranchIdOrderByCreatedAtDesc(currentUser.getBranch().getId());
        } else {
            stocktakes = stocktakeRepository.findAllByOrderByCreatedAtDesc();
        }

        // Limit to last 5 completed stocktakes
        List<Stocktake> completedStocktakes = stocktakes.stream()
                .filter(s -> s.getStatus() == StocktakeStatus.COMPLETED)
                .limit(5)
                .collect(Collectors.toList());

        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        // Reverse to show chronologically
        for (int i = completedStocktakes.size() - 1; i >= 0; i--) {
            Stocktake stocktake = completedStocktakes.get(i);
            int totalShortfall = 0;
            int totalSurplus = 0;

            for (StocktakeDetail detail : stocktake.getDetails()) {
                int expected = detail.getExpectedQuantity();
                int actual = detail.getActualQuantity();
                if (actual < expected) {
                    totalShortfall += (expected - actual);
                } else if (actual > expected) {
                    totalSurplus += (actual - expected);
                }
            }

            java.util.Map<String, Object> dataPoint = new java.util.HashMap<>();
            dataPoint.put("code", stocktake.getCode());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
            dataPoint.put("date", stocktake.getCreatedAt().format(formatter));
            dataPoint.put("shortfall", totalShortfall);
            dataPoint.put("surplus", totalSurplus);
            result.add(dataPoint);
        }

        return result;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.Map<Integer, java.util.List<java.math.BigDecimal>> getBranchSalesTrend30Days() {
        // Trả về doanh thu xuất bán 30 ngày cho TẤT CẢ chi nhánh (Không phụ thuộc vai trò người dùng)
        // Điều này phục vụ riêng cho Dashboard để hiện "tất cả chi nhánh" theo yêu cầu.
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgoStart = now.minusDays(29).toLocalDate().atStartOfDay();

        List<Receipt> exportReceipts = receiptRepository.findByTypeAndStatus(ReceiptType.EXPORT, ReceiptStatus.COMPLETED);
        
        java.util.List<Branch> allBranches = branchRepository.findAll();
        java.util.Map<Integer, java.util.List<java.math.BigDecimal>> branchSalesMap = new java.util.HashMap<>();
        
        for (Branch b : allBranches) {
            java.util.List<java.math.BigDecimal> dailySales = new java.util.ArrayList<>();
            for (int i = 0; i < 30; i++) {
                dailySales.add(BigDecimal.ZERO);
            }
            branchSalesMap.put(b.getId(), dailySales);
        }

        for (Receipt r : exportReceipts) {
            if (r.getCreatedAt() != null && !r.getCreatedAt().isBefore(thirtyDaysAgoStart)) {
                if (r.getSourceBranch() != null && branchSalesMap.containsKey(r.getSourceBranch().getId())) {
                    BigDecimal receiptTotal = r.getDetails().stream()
                            .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                            
                    long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(r.getCreatedAt().toLocalDate(), now.toLocalDate());
                    if (daysAgo >= 0 && daysAgo <= 29) {
                        int index = 29 - (int)daysAgo;
                        java.util.List<java.math.BigDecimal> currentList = branchSalesMap.get(r.getSourceBranch().getId());
                        currentList.set(index, currentList.get(index).add(receiptTotal));
                    }
                }
            }
        }
        
        return branchSalesMap;
    }
}
