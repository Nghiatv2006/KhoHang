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

        List<Inventory> inventories = inventoryRepository.findByBranchId(targetBranchId).stream()
                .filter(inv -> {
                    if (inv.getProduct() != null && Boolean.TRUE.equals(inv.getProduct().getIsDeleted())) {
                        return inv.getQuantity() > 0;
                    }
                    return true;
                })
                .collect(Collectors.toList());

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
        if (currentUser.getRole() == UserRole.MANAGER) {
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
    public byte[] exportRevenueReport(User currentUser, Integer branchId, java.time.LocalDate startDate, java.time.LocalDate endDate, String period) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new AccessDeniedException("Staff không có quyền xuất báo cáo doanh thu.");
        }

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isHeadBranch = currentUser.getBranch() != null && Boolean.TRUE.equals(currentUser.getBranch().getIsHead());

        // Determine report type: Head branch uses TRANSFER, others use EXPORT
        boolean isTransferReport = !isAdmin && isHeadBranch;

        List<Receipt> receipts;
        if (isAdmin) {
            if (branchId != null) {
                // Admin chọn lọc theo một chi nhánh cụ thể → chỉ lấy hoá đơn của chi nhánh đó
                receipts = receiptRepository.findByTypeAndStatusAndSourceBranchId(
                        ReceiptType.EXPORT, ReceiptStatus.COMPLETED, branchId);
            } else {
                // Admin xem toàn hệ thống
                receipts = receiptRepository.findByTypeAndStatus(ReceiptType.EXPORT, ReceiptStatus.COMPLETED);
            }
        } else if (isTransferReport) {
            // Head Branch Manager: get TRANSFER receipts where this branch is the source
            receipts = receiptRepository.findByTypeAndStatusAndSourceBranchId(
                    ReceiptType.TRANSFER, ReceiptStatus.COMPLETED, currentUser.getBranch().getId());
        } else {
            // Child Branch Manager: get EXPORT receipts for their branch
            receipts = receiptRepository.findByTypeAndStatusAndSourceBranchId(
                    ReceiptType.EXPORT, ReceiptStatus.COMPLETED, currentUser.getBranch().getId());
        }

        // Apply date filters if present
        if (startDate != null || endDate != null) {
            receipts = receipts.stream()
                .filter(r -> {
                    java.time.LocalDate rDate = r.getCreatedAt().toLocalDate();
                    if (startDate != null && rDate.isBefore(startDate)) return false;
                    if (endDate != null && rDate.isAfter(endDate)) return false;
                    return true;
                })
                .collect(Collectors.toList());
        }

        // Xác định tên chi nhánh hiển thị trong file Excel
        String branchDisplayName;
        if (isAdmin) {
            if (branchId != null) {
                Branch selectedBranch = branchRepository.findById(branchId).orElse(null);
                branchDisplayName = selectedBranch != null ? selectedBranch.getName() : "Chi nhánh #" + branchId;
            } else {
                branchDisplayName = "TOÀN HỆ THỐNG";
            }
        } else {
            branchDisplayName = currentUser.getBranch() != null ? currentUser.getBranch().getName() : "N/A";
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ====== STYLES ======
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            Font italicFont = workbook.createFont();
            italicFont.setItalic(true);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldFont);

            CellStyle boldCenterStyle = workbook.createCellStyle();
            boldCenterStyle.setFont(boldFont);
            boldCenterStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle italicCenterStyle = workbook.createCellStyle();
            italicCenterStyle.setFont(italicFont);
            italicCenterStyle.setAlignment(HorizontalAlignment.CENTER);

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

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.cloneStyleFrom(borderStyle);
            moneyStyle.setDataFormat(format.getFormat("#,##0"));

            CellStyle percentStyle = workbook.createCellStyle();
            percentStyle.cloneStyleFrom(borderStyle);
            percentStyle.setDataFormat(format.getFormat("0.00%"));

            CellStyle totalRowStyle = workbook.createCellStyle();
            totalRowStyle.cloneStyleFrom(borderStyle);
            totalRowStyle.setFont(boldFont);
            totalRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            totalRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle totalRowMoneyStyle = workbook.createCellStyle();
            totalRowMoneyStyle.cloneStyleFrom(totalRowStyle);
            totalRowMoneyStyle.setDataFormat(format.getFormat("#,##0"));

            CellStyle totalRowCenterStyle = workbook.createCellStyle();
            totalRowCenterStyle.cloneStyleFrom(totalRowStyle);
            totalRowCenterStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle totalRowPercentStyle = workbook.createCellStyle();
            totalRowPercentStyle.cloneStyleFrom(totalRowStyle);
            totalRowPercentStyle.setDataFormat(format.getFormat("0.00%"));

            CellStyle totalRowRightStyle = workbook.createCellStyle();
            totalRowRightStyle.cloneStyleFrom(totalRowStyle);
            totalRowRightStyle.setAlignment(HorizontalAlignment.RIGHT);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String reportDate = LocalDateTime.now().format(dtf);
            String reporterName = currentUser.getFullName();

            if (isTransferReport) {
                // ====== HEAD BRANCH: TRANSFER REPORT ======
                buildTransferTimeSheets(workbook, receipts, reportDate, reporterName, branchDisplayName, period, startDate, endDate,
                        titleStyle, boldStyle, boldCenterStyle, italicCenterStyle,
                        headerStyle, borderStyle, borderCenterStyle, moneyStyle,
                        totalRowStyle, totalRowMoneyStyle, totalRowCenterStyle, totalRowRightStyle);

                buildTransferProductSheet(workbook, receipts, reportDate, reporterName, branchDisplayName,
                        titleStyle, boldStyle, boldCenterStyle, italicCenterStyle,
                        headerStyle, borderStyle, borderCenterStyle, moneyStyle,
                        totalRowStyle, totalRowMoneyStyle, totalRowCenterStyle, totalRowRightStyle);
            } else {
                // ====== REVENUE REPORT (Admin or Child Branch Manager) ======
                buildRevenueTimeSheets(workbook, receipts, isAdmin, reportDate, reporterName, branchDisplayName, period, startDate, endDate,
                        titleStyle, boldStyle, boldCenterStyle, italicCenterStyle,
                        headerStyle, borderStyle, borderCenterStyle, moneyStyle, percentStyle,
                        totalRowStyle, totalRowMoneyStyle, totalRowCenterStyle, totalRowPercentStyle, totalRowRightStyle);

                buildRevenueProductSheet(workbook, receipts, isAdmin, reportDate, reporterName, branchDisplayName,
                        titleStyle, boldStyle, boldCenterStyle, italicCenterStyle,
                        headerStyle, borderStyle, borderCenterStyle, moneyStyle, percentStyle,
                        totalRowStyle, totalRowMoneyStyle, totalRowCenterStyle, totalRowPercentStyle, totalRowRightStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo file Excel báo cáo doanh thu", e);
        }
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

            java.util.Map<String, Object> map = new java.util.HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
            map.put("date", stocktake.getCreatedAt().format(formatter));
            map.put("shortfall", totalShortfall);
            map.put("surplus", totalSurplus);
            result.add(map);
        }
        return result;
    }
    // ================================================================
    //  REVENUE TIME SHEETS (Tuần / Tháng / Quý / Năm)
    // ================================================================
    private void buildRevenueTimeSheets(Workbook workbook, List<Receipt> receipts, boolean isAdmin,
            String reportDate, String reporterName, String branchName, String period,
            java.time.LocalDate startDate, java.time.LocalDate endDate,
            CellStyle titleStyle, CellStyle boldStyle, CellStyle boldCenterStyle, CellStyle italicCenterStyle,
            CellStyle headerStyle, CellStyle borderStyle, CellStyle borderCenterStyle,
            CellStyle moneyStyle, CellStyle percentStyle,
            CellStyle totalRowStyle, CellStyle totalRowMoneyStyle, CellStyle totalRowCenterStyle,
            CellStyle totalRowPercentStyle, CellStyle totalRowRightStyle) {

        String[][] sheetConfigs;
        if (period == null || period.isEmpty()) period = "month";
        switch (period) {
            case "today":
            case "week":
            case "month":
            case "custom":
                sheetConfigs = new String[][]{{"Theo Ngày", "DAY"}};
                break;
            case "quarter":
                sheetConfigs = new String[][]{{"Theo Tháng", "MONTH"}};
                break;
            case "year":
                sheetConfigs = new String[][]{{"Theo Tháng", "MONTH"}, {"Theo Quý", "QUARTER"}};
                break;
            default:
                sheetConfigs = new String[][]{{"Theo Tháng", "MONTH"}};
        }

        for (String[] config : sheetConfigs) {
            Sheet sheet = workbook.createSheet(config[0]);
            String groupType = config[1];

            // Group receipts by time period and optionally by branch
            Map<String, Map<String, List<Receipt>>> groupedData = groupReceiptsByTimeAndBranch(receipts, groupType, isAdmin);

            // Headers
            String[] headers;
            if (isAdmin) {
                headers = new String[]{"STT", "Thời gian", "Tên Chi nhánh", "Số lượng Đơn", "Tổng Sản phẩm",
                        "Tổng Doanh thu (VNĐ)", "Tổng Thực thu (VNĐ)", "Giá vốn hàng ĐÃ BÁN (VNĐ)", "Lợi nhuận gộp (VNĐ)", "Tỷ suất LN"};
            } else {
                headers = new String[]{"STT", "Thời gian", "Số lượng Đơn", "Tổng Sản phẩm",
                        "Tổng Doanh thu (VNĐ)", "Tổng Thực thu (VNĐ)", "Giá vốn hàng ĐÃ BÁN (VNĐ)", "Lợi nhuận gộp (VNĐ)", "Tỷ suất LN"};
            }

            String finalTitle = "BÁO CÁO DOANH THU & LỢI NHUẬN (" + config[0].toUpperCase() + ")";
            if ("custom".equals(period) && startDate != null && endDate != null) {
                java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                finalTitle = "BÁO CÁO DOANH THU (Từ " + startDate.format(df) + " đến " + endDate.format(df) + ")";
            }

            int rowIdx = writeReportHeader(sheet, finalTitle,
                    reportDate, reporterName, branchName, headers,
                    titleStyle, boldStyle, italicCenterStyle, headerStyle);

            // Add cell comments for clarity
            addCellComment(workbook, sheet, rowIdx - 1, isAdmin ? 5 : 4, "Công thức: Giá bán khách hàng × Số lượng bán ra");
            addCellComment(workbook, sheet, rowIdx - 1, isAdmin ? 6 : 5, "Tổng tiền khách đã thanh toán cho các đơn hàng");
            addCellComment(workbook, sheet, rowIdx - 1, isAdmin ? 7 : 6, "Công thức: Giá nhập gốc × Số lượng đã bán ra\n(Chỉ tính cho hàng đã bán, không tính hàng tồn kho)");
            addCellComment(workbook, sheet, rowIdx - 1, isAdmin ? 8 : 7, "Công thức: Tổng Doanh thu - Giá vốn hàng ĐÃ BÁN");
            addCellComment(workbook, sheet, rowIdx - 1, isAdmin ? 9 : 8, "Công thức: (Lợi nhuận gộp / Tổng Doanh thu) × 100%");

            // Freeze header row
            sheet.createFreezePane(0, rowIdx);

            int stt = 1;
            long grandTotalOrders = 0;
            long grandTotalItems = 0;
            BigDecimal grandTotalRevenue = BigDecimal.ZERO;
            BigDecimal grandTotalCollected = BigDecimal.ZERO;
            BigDecimal grandTotalCost = BigDecimal.ZERO;

            // Sort time periods
            List<String> sortedPeriods = new java.util.ArrayList<>(groupedData.keySet());
            java.util.Collections.sort(sortedPeriods);

            for (String periodKey : sortedPeriods) {
                Map<String, List<Receipt>> branchMap = groupedData.get(periodKey);
                List<String> sortedBranches = new java.util.ArrayList<>(branchMap.keySet());
                java.util.Collections.sort(sortedBranches);

                for (String branch : sortedBranches) {
                    List<Receipt> periodReceipts = branchMap.get(branch);

                    long orderCount = periodReceipts.size();
                    long totalItems = periodReceipts.stream()
                            .flatMap(r -> r.getDetails().stream())
                            .mapToLong(ReceiptDetail::getQuantity)
                            .sum();

                    BigDecimal totalRevenue = periodReceipts.stream()
                            .flatMap(r -> r.getDetails().stream())
                            .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalCollected = periodReceipts.stream()
                            .filter(r -> "PAID".equals(r.getPaymentStatus()) || "Đã thanh toán".equals(r.getPaymentStatus()))
                            .flatMap(r -> r.getDetails().stream())
                            .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalCost = periodReceipts.stream()
                            .flatMap(r -> r.getDetails().stream())
                            .map(d -> {
                                BigDecimal importPrice = d.getProduct().getImportPrice();
                                if (importPrice == null) importPrice = BigDecimal.ZERO;
                                return importPrice.multiply(BigDecimal.valueOf(d.getQuantity()));
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal profit = totalRevenue.subtract(totalCost);
                    double profitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                            ? profit.doubleValue() / totalRevenue.doubleValue() : 0;

                    grandTotalOrders += orderCount;
                    grandTotalItems += totalItems;
                    grandTotalRevenue = grandTotalRevenue.add(totalRevenue);
                    grandTotalCollected = grandTotalCollected.add(totalCollected);
                    grandTotalCost = grandTotalCost.add(totalCost);

                    Row row = sheet.createRow(rowIdx++);
                    int col = 0;
                    setCellValue(row, col++, stt++, borderCenterStyle);
                    setCellValue(row, col++, periodKey, borderStyle);
                    if (isAdmin) {
                        setCellValue(row, col++, branch, borderStyle);
                    }
                    setCellValue(row, col++, orderCount, borderCenterStyle);
                    setCellValue(row, col++, totalItems, borderCenterStyle);
                    setCellValue(row, col++, totalRevenue.doubleValue(), moneyStyle);
                    setCellValue(row, col++, totalCollected.doubleValue(), moneyStyle);
                    setCellValue(row, col++, totalCost.doubleValue(), moneyStyle);
                    setCellValue(row, col++, profit.doubleValue(), moneyStyle);
                    setCellValue(row, col++, profitMargin, percentStyle);
                }
            }

            // Total row
            BigDecimal grandProfit = grandTotalRevenue.subtract(grandTotalCost);
            double grandProfitMargin = grandTotalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? grandProfit.doubleValue() / grandTotalRevenue.doubleValue() : 0;

            Row totalRow = sheet.createRow(rowIdx++);
            int col = 0;
            int mergeEnd = isAdmin ? 2 : 1;
            for (int i = 0; i <= mergeEnd; i++) {
                totalRow.createCell(i).setCellStyle(totalRowRightStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, mergeEnd));
            totalRow.getCell(0).setCellValue("TỔNG CỘNG:");
            col = mergeEnd + 1;
            setCellValue(totalRow, col++, grandTotalOrders, totalRowCenterStyle);
            setCellValue(totalRow, col++, grandTotalItems, totalRowCenterStyle);
            setCellValue(totalRow, col++, grandTotalRevenue.doubleValue(), totalRowMoneyStyle);
            setCellValue(totalRow, col++, grandTotalCollected.doubleValue(), totalRowMoneyStyle);
            setCellValue(totalRow, col++, grandTotalCost.doubleValue(), totalRowMoneyStyle);
            setCellValue(totalRow, col++, grandProfit.doubleValue(), totalRowMoneyStyle);
            setCellValue(totalRow, col++, grandProfitMargin, totalRowPercentStyle);

            // Signature section
            writeSignatureSection(sheet, rowIdx + 2, boldCenterStyle, italicCenterStyle, isAdmin ? 8 : 7);

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }

    // ================================================================
    //  REVENUE PRODUCT DETAIL SHEET
    // ================================================================
    private void buildRevenueProductSheet(Workbook workbook, List<Receipt> receipts, boolean isAdmin,
            String reportDate, String reporterName, String branchName,
            CellStyle titleStyle, CellStyle boldStyle, CellStyle boldCenterStyle, CellStyle italicCenterStyle,
            CellStyle headerStyle, CellStyle borderStyle, CellStyle borderCenterStyle, CellStyle moneyStyle,
            CellStyle percentStyle,
            CellStyle totalRowStyle, CellStyle totalRowMoneyStyle, CellStyle totalRowCenterStyle,
            CellStyle totalRowPercentStyle, CellStyle totalRowRightStyle) {

        Sheet sheet = workbook.createSheet("Chi tiết Hàng hóa");

        String[] headers;
        if (isAdmin) {
            headers = new String[]{"STT", "Tên Chi nhánh", "Tên Sản phẩm", "Danh mục", "Giá nhập gốc (VNĐ)",
                    "Giá bán (VNĐ)", "SL Đã bán", "Tổng Doanh thu (VNĐ)", "Tổng Lợi nhuận (VNĐ)", "% Đóng góp LN"};
        } else {
            headers = new String[]{"STT", "Tên Sản phẩm", "Danh mục", "Giá nhập gốc (VNĐ)",
                    "Giá bán (VNĐ)", "SL Đã bán", "Tổng Doanh thu (VNĐ)", "Tổng Lợi nhuận (VNĐ)", "% Đóng góp LN"};
        }

        int rowIdx = writeReportHeader(sheet, "BÁO CÁO CHI TIẾT BÁN HÀNG THEO SẢN PHẨM",
                reportDate, reporterName, branchName, headers,
                titleStyle, boldStyle, italicCenterStyle, headerStyle);

        // Thêm ghi chú cho cột % Đóng góp LN
        int profitContribColIdx = isAdmin ? 9 : 8;
        addCellComment(workbook, sheet, rowIdx - 1, profitContribColIdx,
                "Công thức: (Lợi nhuận sản phẩm / Tổng lợi nhuận toàn bộ) × 100%\n(Cho biết sản phẩm này đóng góp bao nhiêu % vào tổng lợi nhuận)");

        sheet.createFreezePane(0, rowIdx);

        // Group by branch (if admin) and product
        // Key: branchName -> productId -> aggregated data
        Map<String, Map<Integer, double[]>> productStats = new java.util.LinkedHashMap<>();
        Map<Integer, String[]> productInfo = new java.util.HashMap<>(); // productId -> [name, category]
        Map<Integer, BigDecimal> productImportPrices = new java.util.HashMap<>();

        for (Receipt receipt : receipts) {
            String bName = receipt.getSourceBranch() != null ? receipt.getSourceBranch().getName() : "N/A";
            if (!isAdmin) bName = "_SINGLE_";

            productStats.computeIfAbsent(bName, k -> new java.util.LinkedHashMap<>());

            for (ReceiptDetail detail : receipt.getDetails()) {
                Product product = detail.getProduct();
                int pid = product.getId();

                productInfo.putIfAbsent(pid, new String[]{
                        product.getName(),
                        product.getCategory() != null ? product.getCategory().getName() : "N/A"
                });
                productImportPrices.putIfAbsent(pid, product.getImportPrice() != null ? product.getImportPrice() : BigDecimal.ZERO);

                double[] stats = productStats.get(bName).computeIfAbsent(pid, k -> new double[4]);
                // [0] = avg sell price accumulator, [1] = total qty, [2] = total revenue, [3] = total cost
                BigDecimal importPrice = product.getImportPrice() != null ? product.getImportPrice() : BigDecimal.ZERO;
                double revenue = detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())).doubleValue();
                double cost = importPrice.multiply(BigDecimal.valueOf(detail.getQuantity())).doubleValue();

                stats[0] += detail.getPrice().doubleValue() * detail.getQuantity(); // weighted price accumulator
                stats[1] += detail.getQuantity();
                stats[2] += revenue;
                stats[3] += cost;
            }
        }

        // ── Tính tổng Lợi nhuận toàn bộ TRƯỚC để tính % Đóng góp ──
        double grandTotalProfitDouble = 0;
        for (Map<Integer, double[]> products : productStats.values()) {
            for (double[] stats : products.values()) {
                grandTotalProfitDouble += (stats[2] - stats[3]);
            }
        }

        int stt = 1;
        long grandTotalQty = 0;
        BigDecimal grandTotalRevenue = BigDecimal.ZERO;
        BigDecimal grandTotalProfit = BigDecimal.ZERO;

        List<String> sortedBranches = new java.util.ArrayList<>(productStats.keySet());
        java.util.Collections.sort(sortedBranches);

        for (String branch : sortedBranches) {
            Map<Integer, double[]> products = productStats.get(branch);

            // Sort by profit descending (most profitable first)
            List<Map.Entry<Integer, double[]>> sortedProducts = new java.util.ArrayList<>(products.entrySet());
            sortedProducts.sort((a, b) -> Double.compare((b.getValue()[2] - b.getValue()[3]), (a.getValue()[2] - a.getValue()[3])));

            for (Map.Entry<Integer, double[]> entry : sortedProducts) {
                int pid = entry.getKey();
                double[] stats = entry.getValue();
                String[] info = productInfo.get(pid);
                BigDecimal importPrice = productImportPrices.get(pid);

                double avgPrice = stats[1] > 0 ? stats[0] / stats[1] : 0;
                double profit = stats[2] - stats[3];
                // % Đóng góp LN: tỷ lệ lợi nhuận sản phẩm này / tổng lợi nhuận toàn bộ
                double profitContribution = grandTotalProfitDouble != 0 ? profit / grandTotalProfitDouble : 0;

                grandTotalQty += (long) stats[1];
                grandTotalRevenue = grandTotalRevenue.add(BigDecimal.valueOf(stats[2]));
                grandTotalProfit = grandTotalProfit.add(BigDecimal.valueOf(profit));

                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                setCellValue(row, c++, stt++, borderCenterStyle);
                if (isAdmin) {
                    setCellValue(row, c++, branch, borderStyle);
                }
                setCellValue(row, c++, info[0], borderStyle);       // Tên SP
                setCellValue(row, c++, info[1], borderStyle);       // Danh mục
                setCellValue(row, c++, importPrice.doubleValue(), moneyStyle); // Giá nhập
                setCellValue(row, c++, avgPrice, moneyStyle);       // Giá bán bình quân
                setCellValue(row, c++, (long) stats[1], borderCenterStyle); // SL đã bán
                setCellValue(row, c++, stats[2], moneyStyle);       // Doanh thu
                setCellValue(row, c++, profit, moneyStyle);         // Lợi nhuận
                setCellValue(row, c++, profitContribution, percentStyle); // % Đóng góp LN
            }
        }

        // Total row
        Row totalRow = sheet.createRow(rowIdx++);
        int mergeEnd = isAdmin ? 5 : 4;
        for (int i = 0; i <= mergeEnd; i++) {
            totalRow.createCell(i).setCellStyle(totalRowRightStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, mergeEnd));
        totalRow.getCell(0).setCellValue("TỔNG CỘNG:");

        int c = mergeEnd + 1;
        setCellValue(totalRow, c++, grandTotalQty, totalRowCenterStyle);
        setCellValue(totalRow, c++, grandTotalRevenue.doubleValue(), totalRowMoneyStyle);
        setCellValue(totalRow, c++, grandTotalProfit.doubleValue(), totalRowMoneyStyle);
        // Tổng % Đóng góp luôn = 100%
        setCellValue(totalRow, c++, 1.0, totalRowPercentStyle);

        writeSignatureSection(sheet, rowIdx + 2, boldCenterStyle, italicCenterStyle, isAdmin ? 9 : 8);

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }


    // ================================================================
    //  TRANSFER TIME SHEETS (Head Branch)
    // ================================================================
    private void buildTransferTimeSheets(Workbook workbook, List<Receipt> receipts,
            String reportDate, String reporterName, String branchName, String period,
            java.time.LocalDate startDate, java.time.LocalDate endDate,
            CellStyle titleStyle, CellStyle boldStyle, CellStyle boldCenterStyle, CellStyle italicCenterStyle,
            CellStyle headerStyle, CellStyle borderStyle, CellStyle borderCenterStyle, CellStyle moneyStyle,
            CellStyle totalRowStyle, CellStyle totalRowMoneyStyle, CellStyle totalRowCenterStyle, CellStyle totalRowRightStyle) {

        String[][] sheetConfigs;
        if (period == null || period.isEmpty()) period = "month";
        switch (period) {
            case "today":
            case "week":
            case "month":
            case "custom":
                sheetConfigs = new String[][]{{"Theo Ngày", "DAY"}};
                break;
            case "quarter":
                sheetConfigs = new String[][]{{"Theo Tháng", "MONTH"}};
                break;
            case "year":
                sheetConfigs = new String[][]{{"Theo Tháng", "MONTH"}, {"Theo Quý", "QUARTER"}};
                break;
            default:
                sheetConfigs = new String[][]{{"Theo Tháng", "MONTH"}};
        }

        for (String[] config : sheetConfigs) {
            Sheet sheet = workbook.createSheet(config[0]);

            Map<String, Map<String, List<Receipt>>> groupedData = groupReceiptsByTimeAndBranch(receipts, config[1], true);

            String[] headers = {"STT", "Thời gian", "Chi nhánh nhận", "Số lệnh Điều chuyển",
                    "Tổng SL Phân bổ", "Tổng Giá trị xuất kho (VNĐ)"};

            String finalTitle = "BÁO CÁO LUÂN CHUYỂN NỘI BỘ (" + config[0].toUpperCase() + ")";
            if ("custom".equals(period) && startDate != null && endDate != null) {
                java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                finalTitle = "BÁO CÁO LUÂN CHUYỂN (Từ " + startDate.format(df) + " đến " + endDate.format(df) + ")";
            }

            int rowIdx = writeReportHeader(sheet, finalTitle,
                    reportDate, reporterName, branchName, headers,
                    titleStyle, boldStyle, italicCenterStyle, headerStyle);

            addCellComment(workbook, sheet, rowIdx - 1, 5, "Tính bằng: Giá nhập gốc × Số lượng phân bổ\n(Phản ánh giá trị tài sản luân chuyển, không phải doanh thu)");

            sheet.createFreezePane(0, rowIdx);

            int stt = 1;
            long grandTotalOrders = 0;
            long grandTotalItems = 0;
            BigDecimal grandTotalValue = BigDecimal.ZERO;

            List<String> sortedPeriods = new java.util.ArrayList<>(groupedData.keySet());
            java.util.Collections.sort(sortedPeriods);

            for (String periodKey : sortedPeriods) {
                Map<String, List<Receipt>> branchMap = groupedData.get(periodKey);
                List<String> sortedBranches = new java.util.ArrayList<>(branchMap.keySet());
                java.util.Collections.sort(sortedBranches);

                for (String destBranch : sortedBranches) {
                    List<Receipt> periodReceipts = branchMap.get(destBranch);

                    long orderCount = periodReceipts.size();
                    long totalItems = periodReceipts.stream()
                            .flatMap(r -> r.getDetails().stream())
                            .mapToLong(ReceiptDetail::getQuantity)
                            .sum();

                    BigDecimal totalValue = periodReceipts.stream()
                            .flatMap(r -> r.getDetails().stream())
                            .map(d -> {
                                BigDecimal ip = d.getProduct().getImportPrice();
                                if (ip == null) ip = BigDecimal.ZERO;
                                return ip.multiply(BigDecimal.valueOf(d.getQuantity()));
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    grandTotalOrders += orderCount;
                    grandTotalItems += totalItems;
                    grandTotalValue = grandTotalValue.add(totalValue);

                    Row row = sheet.createRow(rowIdx++);
                    setCellValue(row, 0, stt++, borderCenterStyle);
                    setCellValue(row, 1, periodKey, borderStyle);
                    setCellValue(row, 2, destBranch, borderStyle);
                    setCellValue(row, 3, orderCount, borderCenterStyle);
                    setCellValue(row, 4, totalItems, borderCenterStyle);
                    setCellValue(row, 5, totalValue.doubleValue(), moneyStyle);
                }
            }

            // Total row
            Row totalRow = sheet.createRow(rowIdx++);
            for (int i = 0; i <= 2; i++) {
                totalRow.createCell(i).setCellStyle(totalRowRightStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 2));
            totalRow.getCell(0).setCellValue("TỔNG CỘNG:");
            setCellValue(totalRow, 3, grandTotalOrders, totalRowCenterStyle);
            setCellValue(totalRow, 4, grandTotalItems, totalRowCenterStyle);
            setCellValue(totalRow, 5, grandTotalValue.doubleValue(), totalRowMoneyStyle);

            writeSignatureSection(sheet, rowIdx + 2, boldCenterStyle, italicCenterStyle, 5);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }

    // ================================================================
    //  TRANSFER PRODUCT DETAIL SHEET
    // ================================================================
    private void buildTransferProductSheet(Workbook workbook, List<Receipt> receipts,
            String reportDate, String reporterName, String branchName,
            CellStyle titleStyle, CellStyle boldStyle, CellStyle boldCenterStyle, CellStyle italicCenterStyle,
            CellStyle headerStyle, CellStyle borderStyle, CellStyle borderCenterStyle, CellStyle moneyStyle,
            CellStyle totalRowStyle, CellStyle totalRowMoneyStyle, CellStyle totalRowCenterStyle, CellStyle totalRowRightStyle) {

        Sheet sheet = workbook.createSheet("Chi tiết Hàng Luân chuyển");

        String[] headers = {"STT", "Chi nhánh nhận", "Tên Sản phẩm", "SL Phân bổ", "Tổng Giá trị (VNĐ)"};

        int rowIdx = writeReportHeader(sheet, "BÁO CÁO CHI TIẾT HÀNG LUÂN CHUYỂN THEO SẢN PHẨM",
                reportDate, reporterName, branchName, headers,
                titleStyle, boldStyle, italicCenterStyle, headerStyle);

        sheet.createFreezePane(0, rowIdx);

        // Group by destBranch -> product
        Map<String, Map<Integer, double[]>> branchProductMap = new java.util.LinkedHashMap<>();
        Map<Integer, String> productNames = new java.util.HashMap<>();

        for (Receipt receipt : receipts) {
            String destName = receipt.getDestBranch() != null ? receipt.getDestBranch().getName() : "N/A";
            branchProductMap.computeIfAbsent(destName, k -> new java.util.LinkedHashMap<>());

            for (ReceiptDetail detail : receipt.getDetails()) {
                int pid = detail.getProduct().getId();
                productNames.putIfAbsent(pid, detail.getProduct().getName());
                BigDecimal importPrice = detail.getProduct().getImportPrice() != null
                        ? detail.getProduct().getImportPrice() : BigDecimal.ZERO;

                double[] stats = branchProductMap.get(destName).computeIfAbsent(pid, k -> new double[2]);
                stats[0] += detail.getQuantity(); // qty
                stats[1] += importPrice.multiply(BigDecimal.valueOf(detail.getQuantity())).doubleValue(); // value
            }
        }

        int stt = 1;
        long grandTotalQty = 0;
        BigDecimal grandTotalValue = BigDecimal.ZERO;

        List<String> sortedBranches = new java.util.ArrayList<>(branchProductMap.keySet());
        java.util.Collections.sort(sortedBranches);

        for (String destBranch : sortedBranches) {
            Map<Integer, double[]> products = branchProductMap.get(destBranch);
            List<Map.Entry<Integer, double[]>> sortedProducts = new java.util.ArrayList<>(products.entrySet());
            sortedProducts.sort((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]));

            for (Map.Entry<Integer, double[]> entry : sortedProducts) {
                double[] stats = entry.getValue();
                Row row = sheet.createRow(rowIdx++);
                setCellValue(row, 0, stt++, borderCenterStyle);
                setCellValue(row, 1, destBranch, borderStyle);
                setCellValue(row, 2, productNames.get(entry.getKey()), borderStyle);
                setCellValue(row, 3, (long) stats[0], borderCenterStyle);
                setCellValue(row, 4, stats[1], moneyStyle);

                grandTotalQty += (long) stats[0];
                grandTotalValue = grandTotalValue.add(BigDecimal.valueOf(stats[1]));
            }
        }

        // Total row
        Row totalRow = sheet.createRow(rowIdx++);
        for (int i = 0; i <= 2; i++) {
            totalRow.createCell(i).setCellStyle(totalRowRightStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 2));
        totalRow.getCell(0).setCellValue("TỔNG CỘNG:");
        setCellValue(totalRow, 3, grandTotalQty, totalRowCenterStyle);
        setCellValue(totalRow, 4, grandTotalValue.doubleValue(), totalRowMoneyStyle);

        writeSignatureSection(sheet, rowIdx + 2, boldCenterStyle, italicCenterStyle, 4);

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ================================================================
    //  HELPER: Group receipts by time period and branch
    // ================================================================
    private Map<String, Map<String, List<Receipt>>> groupReceiptsByTimeAndBranch(
            List<Receipt> receipts, String groupType, boolean groupByBranch) {

        Map<String, Map<String, List<Receipt>>> result = new java.util.TreeMap<>();

        for (Receipt receipt : receipts) {
            if (receipt.getCreatedAt() == null) continue;
            LocalDateTime dt = receipt.getCreatedAt();

            String periodStr;
            switch (groupType) {
                case "RECEIPT":
                    periodStr = String.format("Mã %05d (%02d:%02d)", receipt.getId(), dt.getHour(), dt.getMinute());
                    break;
                case "DAY":
                    periodStr = String.format("Ngày %02d/%02d/%d", dt.getDayOfMonth(), dt.getMonthValue(), dt.getYear());
                    break;
                case "WEEK":
                    java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;
                    int weekNum = dt.get(weekFields.weekOfWeekBasedYear());
                    int weekYear = dt.get(weekFields.weekBasedYear());
                    periodStr = String.format("Tuần %02d - %d", weekNum, weekYear);
                    break;
                case "MONTH":
                    periodStr = String.format("Tháng %02d/%d", dt.getMonthValue(), dt.getYear());
                    break;
                case "QUARTER":
                    int quarter = (dt.getMonthValue() - 1) / 3 + 1;
                    int startMonth = (quarter - 1) * 3 + 1;
                    int endMonth = startMonth + 2;
                    periodStr = String.format("Quý %d (T%d-T%d/%d)", quarter, startMonth, endMonth, dt.getYear());
                    break;
                case "YEAR":
                    periodStr = String.format("Năm %d", dt.getYear());
                    break;
                default:
                    periodStr = dt.toString();
            }

            // For TRANSFER reports, use destBranch; for EXPORT, use sourceBranch
            String branchLabel;
            if (groupByBranch) {
                if (receipt.getType() == ReceiptType.TRANSFER) {
                    branchLabel = receipt.getDestBranch() != null ? receipt.getDestBranch().getName() : "N/A";
                } else {
                    branchLabel = receipt.getSourceBranch() != null ? receipt.getSourceBranch().getName() : "N/A";
                }
            } else {
                branchLabel = "_ALL_";
            }

            result.computeIfAbsent(periodStr, k -> new java.util.TreeMap<>())
                    .computeIfAbsent(branchLabel, k -> new java.util.ArrayList<>())
                    .add(receipt);
        }

        return result;
    }

    // ================================================================
    //  HELPER: Write report header (Company info, Title, Metadata, Column Headers)
    // ================================================================
    private int writeReportHeader(Sheet sheet, String title,
            String reportDate, String reporterName, String branchName,
            String[] headers,
            CellStyle titleStyle, CellStyle boldStyle, CellStyle italicCenterStyle, CellStyle headerStyle) {

        Row r0 = sheet.createRow(0);
        Cell c00 = r0.createCell(0);
        c00.setCellValue("CÔNG TY TNHH WAREHUB");
        c00.setCellStyle(boldStyle);

        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("Chi nhánh: " + branchName);

        Row r3 = sheet.createRow(3);
        Cell titleCell = r3.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, headers.length - 1));

        Row r4 = sheet.createRow(4);
        Cell infoCell = r4.createCell(0);
        infoCell.setCellValue("Ngày xuất: " + reportDate + " - Người lập biểu: " + reporterName);
        infoCell.setCellStyle(italicCenterStyle);
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, headers.length - 1));

        Row headerRow = sheet.createRow(6);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        return 7; // next row index after headers
    }

    // ================================================================
    //  HELPER: Add Cell Comment
    // ================================================================
    private void addCellComment(Workbook workbook, Sheet sheet, int rowIdx, int colIdx, String commentText) {
        try {
            org.apache.poi.ss.usermodel.Drawing<?> drawing = sheet.createDrawingPatriarch();
            org.apache.poi.ss.usermodel.CreationHelper factory = workbook.getCreationHelper();
            org.apache.poi.ss.usermodel.ClientAnchor anchor = factory.createClientAnchor();
            anchor.setCol1(colIdx);
            anchor.setRow1(rowIdx);
            anchor.setCol2(colIdx + 4);
            anchor.setRow2(rowIdx + 4);

            Comment comment = drawing.createCellComment(anchor);
            comment.setString(factory.createRichTextString(commentText));

            Row row = sheet.getRow(rowIdx);
            if (row != null) {
                Cell cell = row.getCell(colIdx);
                if (cell != null) {
                    cell.setCellComment(comment);
                }
            }
        } catch (Exception e) {
            // Silently ignore comment creation errors
        }
    }

    // ================================================================
    //  HELPER: Write signature section
    // ================================================================
    private void writeSignatureSection(Sheet sheet, int startRow, CellStyle boldCenterStyle, CellStyle italicCenterStyle, int lastCol) {
        int midCol = lastCol / 2;
        int endCol = lastCol;

        Row signLabelRow = sheet.createRow(startRow);
        Cell sig1 = signLabelRow.createCell(0);
        sig1.setCellValue("Người lập biểu");
        sig1.setCellStyle(boldCenterStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, midCol - 1));

        Cell sig2 = signLabelRow.createCell(midCol + 1);
        sig2.setCellValue("Giám đốc phê duyệt");
        sig2.setCellStyle(boldCenterStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, midCol + 1, endCol));

        Row signSubRow = sheet.createRow(startRow + 1);
        Cell sub1 = signSubRow.createCell(0);
        sub1.setCellValue("(Ký, ghi rõ họ tên)");
        sub1.setCellStyle(italicCenterStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 1, 0, midCol - 1));

        Cell sub2 = signSubRow.createCell(midCol + 1);
        sub2.setCellValue("(Ký, đóng dấu, họ tên)");
        sub2.setCellStyle(italicCenterStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 1, midCol + 1, endCol));
    }

    // ================================================================
    //  HELPER: Set cell value with style
    // ================================================================
    private void setCellValue(Row row, int colIdx, Object value, CellStyle style) {
        Cell cell = row.createCell(colIdx);
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value != null) {
            cell.setCellValue(value.toString());
        }
        cell.setCellStyle(style);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.Map<Integer, java.util.List<java.math.BigDecimal>> getBranchSalesTrend30Days(User currentUser) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgoStart = now.minusDays(29).toLocalDate().atStartOfDay();

        List<Receipt> exportReceipts;
        java.util.List<Branch> branchesToQuery;

        boolean isGlobalUser = currentUser.getRole() == UserRole.ADMIN || 
                               (currentUser.getBranch() != null && currentUser.getBranch().getId() == 1);

        if (isGlobalUser) {
            exportReceipts = receiptRepository.findByTypeAndStatus(ReceiptType.EXPORT, ReceiptStatus.COMPLETED);
            branchesToQuery = branchRepository.findAll();
        } else {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) {
                exportReceipts = java.util.Collections.emptyList();
                branchesToQuery = java.util.Collections.emptyList();
            } else {
                exportReceipts = receiptRepository.findByTypeAndStatusAndSourceBranchId(
                        ReceiptType.EXPORT, ReceiptStatus.COMPLETED, myBranchId);
                branchesToQuery = branchRepository.findById(myBranchId)
                        .map(java.util.List::of)
                        .orElse(java.util.Collections.emptyList());
            }
        }

        java.util.Map<Integer, java.util.List<java.math.BigDecimal>> branchSalesMap = new java.util.HashMap<>();
        
        for (Branch b : branchesToQuery) {
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

    // ================================================================
    //  REVENUE SUMMARY: Tuần / Tháng / Quý / Năm (Real-time)
    // ================================================================
    @Override
    public java.util.Map<String, Object> getRevenueSummary(User currentUser, Integer branchId) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new org.springframework.security.access.AccessDeniedException("Staff không có quyền xem báo cáo doanh thu.");
        }

        // Determine which branch(es) to query
        List<Receipt> allExportReceipts;
        if (currentUser.getRole() == UserRole.ADMIN) {
            if (branchId != null) {
                // Admin chọn xem một chi nhánh cụ thể
                allExportReceipts = receiptRepository.findByTypeAndStatusAndSourceBranchId(
                        ReceiptType.EXPORT, ReceiptStatus.COMPLETED, branchId);
            } else {
                // Admin xem toàn hệ thống
                allExportReceipts = receiptRepository.findByTypeAndStatus(ReceiptType.EXPORT, ReceiptStatus.COMPLETED);
            }
        } else {
            // Manager chỉ xem được chi nhánh của mình
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) {
                allExportReceipts = java.util.Collections.emptyList();
            } else {
                allExportReceipts = receiptRepository.findByTypeAndStatusAndSourceBranchId(
                        ReceiptType.EXPORT, ReceiptStatus.COMPLETED, myBranchId);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // ── Tuần hiện tại (Mon–Sun) & tuần trước ──────────────────────
        java.time.LocalDate today = now.toLocalDate();
        java.time.DayOfWeek dow = today.getDayOfWeek();
        int dayValue = dow.getValue(); // Mon=1 ... Sun=7
        java.time.LocalDate thisWeekStart  = today.minusDays(dayValue - 1);
        java.time.LocalDate lastWeekStart  = thisWeekStart.minusWeeks(1);
        java.time.LocalDate lastWeekEnd    = thisWeekStart.minusDays(1);

        // ── Tháng hiện tại & tháng trước ──────────────────────────────
        java.time.LocalDate thisMonthStart = today.withDayOfMonth(1);
        java.time.LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
        java.time.LocalDate lastMonthEnd   = thisMonthStart.minusDays(1);

        // ── Quý hiện tại ──────────────────────────────────────────────
        int currentMonth = today.getMonthValue();
        int thisQuarter  = (currentMonth - 1) / 3 + 1;
        java.time.LocalDate thisQuarterStart = java.time.LocalDate.of(today.getYear(), (thisQuarter - 1) * 3 + 1, 1);

        // ── Năm hiện tại ──────────────────────────────────────────────
        java.time.LocalDate thisYearStart = java.time.LocalDate.of(today.getYear(), 1, 1);

        BigDecimal weekRevenue      = BigDecimal.ZERO;
        BigDecimal lastWeekRevenue  = BigDecimal.ZERO;
        BigDecimal monthRevenue     = BigDecimal.ZERO;
        BigDecimal lastMonthRevenue = BigDecimal.ZERO;
        BigDecimal quarterRevenue   = BigDecimal.ZERO;
        BigDecimal yearRevenue      = BigDecimal.ZERO;

        // Lợi nhuận gộp theo kỳ (= Doanh thu - Giá vốn hàng ĐÃ BÁN)
        BigDecimal weekProfit       = BigDecimal.ZERO;
        BigDecimal monthProfit      = BigDecimal.ZERO;
        BigDecimal quarterProfit    = BigDecimal.ZERO;
        BigDecimal yearProfit       = BigDecimal.ZERO;

        // Tổng thực thu (chỉ tính các phiếu đã thanh toán)
        BigDecimal weekCollected       = BigDecimal.ZERO;
        BigDecimal monthCollected      = BigDecimal.ZERO;
        BigDecimal quarterCollected    = BigDecimal.ZERO;
        BigDecimal yearCollected       = BigDecimal.ZERO;

        for (Receipt r : allExportReceipts) {
            if (r.getCreatedAt() == null) continue;
            java.time.LocalDate rDate = r.getCreatedAt().toLocalDate();

            boolean isPaid = "PAID".equals(r.getPaymentStatus()) || "Đã thanh toán".equals(r.getPaymentStatus());

            BigDecimal amount = r.getDetails().stream()
                    .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal cost = r.getDetails().stream()
                    .map(d -> {
                        BigDecimal ip = d.getProduct() != null && d.getProduct().getImportPrice() != null
                                ? d.getProduct().getImportPrice() : BigDecimal.ZERO;
                        return ip.multiply(BigDecimal.valueOf(d.getQuantity()));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal profit = amount.subtract(cost);

            // Tuần này
            if (!rDate.isBefore(thisWeekStart) && !rDate.isAfter(today)) {
                weekRevenue = weekRevenue.add(amount);
                weekProfit  = weekProfit.add(profit);
                if (isPaid) weekCollected = weekCollected.add(amount);
            }
            // Tuần trước
            if (!rDate.isBefore(lastWeekStart) && !rDate.isAfter(lastWeekEnd)) {
                lastWeekRevenue = lastWeekRevenue.add(amount);
            }
            // Tháng này
            if (!rDate.isBefore(thisMonthStart) && !rDate.isAfter(today)) {
                monthRevenue = monthRevenue.add(amount);
                monthProfit  = monthProfit.add(profit);
                if (isPaid) monthCollected = monthCollected.add(amount);
            }
            // Tháng trước
            if (!rDate.isBefore(lastMonthStart) && !rDate.isAfter(lastMonthEnd)) {
                lastMonthRevenue = lastMonthRevenue.add(amount);
            }
            // Quý này
            if (!rDate.isBefore(thisQuarterStart) && !rDate.isAfter(today)) {
                quarterRevenue = quarterRevenue.add(amount);
                quarterProfit  = quarterProfit.add(profit);
                if (isPaid) quarterCollected = quarterCollected.add(amount);
            }
            // Năm nay
            if (!rDate.isBefore(thisYearStart) && !rDate.isAfter(today)) {
                yearRevenue = yearRevenue.add(amount);
                yearProfit  = yearProfit.add(profit);
                if (isPaid) yearCollected = yearCollected.add(amount);
            }
        }

        // Tính % thay đổi doanh thu (null nếu không có dữ liệu kỳ trước)
        Double weekChangePct  = calcChangePct(weekRevenue,  lastWeekRevenue);
        Double monthChangePct = calcChangePct(monthRevenue, lastMonthRevenue);

        // Tính Margin % = Lợi nhuận / Doanh thu (trả về null nếu doanh thu = 0 để FE ẩn đi)
        Double weekMargin    = weekRevenue.compareTo(BigDecimal.ZERO)    > 0
                ? weekProfit.doubleValue()    / weekRevenue.doubleValue()    * 100 : null;
        Double monthMargin   = monthRevenue.compareTo(BigDecimal.ZERO)   > 0
                ? monthProfit.doubleValue()   / monthRevenue.doubleValue()   * 100 : null;
        Double quarterMargin = quarterRevenue.compareTo(BigDecimal.ZERO) > 0
                ? quarterProfit.doubleValue() / quarterRevenue.doubleValue() * 100 : null;
        Double yearMargin    = yearRevenue.compareTo(BigDecimal.ZERO)    > 0
                ? yearProfit.doubleValue()    / yearRevenue.doubleValue()    * 100 : null;

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("weekRevenue",      weekRevenue);
        result.put("lastWeekRevenue",  lastWeekRevenue);
        result.put("weekChangePct",    weekChangePct);
        result.put("weekProfit",       weekProfit);
        result.put("weekCollected",    weekCollected);
        result.put("weekMargin",       weekMargin);
        result.put("monthRevenue",     monthRevenue);
        result.put("lastMonthRevenue", lastMonthRevenue);
        result.put("monthChangePct",   monthChangePct);
        result.put("monthProfit",      monthProfit);
        result.put("monthCollected",   monthCollected);
        result.put("monthMargin",      monthMargin);
        result.put("quarterRevenue",   quarterRevenue);
        result.put("quarterProfit",    quarterProfit);
        result.put("quarterCollected", quarterCollected);
        result.put("quarterMargin",    quarterMargin);
        result.put("yearRevenue",      yearRevenue);
        result.put("yearProfit",       yearProfit);
        result.put("yearCollected",    yearCollected);
        result.put("yearMargin",       yearMargin);
        return result;
    }

    private Double calcChangePct(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : null;
        }
        return current.subtract(previous)
                .divide(previous, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
}

