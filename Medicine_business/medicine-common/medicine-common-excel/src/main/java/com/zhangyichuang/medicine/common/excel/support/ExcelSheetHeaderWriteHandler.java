package com.zhangyichuang.medicine.common.excel.support;

import org.apache.fesod.sheet.write.handler.SheetWriteHandler;
import org.apache.fesod.sheet.write.handler.context.SheetWriteHandlerContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.util.Assert;

import java.awt.Color;
import java.util.List;

/**
 * Excel 说明头写入处理器。
 */
public class ExcelSheetHeaderWriteHandler implements SheetWriteHandler {

    /**
     * 说明头背景颜色。
     */
    private static final Color SHEET_HEADER_BACKGROUND_COLOR = new Color(234, 233, 233);

    /**
     * 说明头文本列表。
     */
    private final List<String> sheetHeaderLines;

    /**
     * 说明头合并列数。
     */
    private final int columnCount;

    /**
     * 构造 Excel 说明头写入处理器。
     *
     * @param sheetHeaderLines 说明头文本列表
     * @param columnCount      说明头合并列数
     */
    public ExcelSheetHeaderWriteHandler(List<String> sheetHeaderLines, int columnCount) {
        Assert.notEmpty(sheetHeaderLines, "sheetHeaderLines must not be empty");
        Assert.isTrue(columnCount > 0, "columnCount must be greater than 0");
        this.sheetHeaderLines = sheetHeaderLines;
        this.columnCount = columnCount;
    }

    /**
     * 在工作表创建完成后写入说明头。
     *
     * @param context 工作表写入上下文
     * @return 无返回值
     */
    @Override
    public void afterSheetCreate(SheetWriteHandlerContext context) {
        Sheet sheet = context.getWriteSheetHolder().getSheet();
        for (int rowIndex = 0; rowIndex < sheetHeaderLines.size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex);
            row.setHeightInPoints(22F);
            Cell cell = row.createCell(0, CellType.STRING);
            cell.setCellValue(sheetHeaderLines.get(rowIndex));
            if (columnCount > 1) {
                sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, columnCount - 1));
            }
        }
        sheet.createFreezePane(0, sheetHeaderLines.size() + 1);
    }

    /**
     * 在工作表写入完成后同步说明头样式。
     *
     * @param context 工作表写入上下文
     * @return 无返回值
     */
    @Override
    public void afterSheetDispose(SheetWriteHandlerContext context) {
        Sheet sheet = context.getWriteSheetHolder().getSheet();
        CellStyle headerStyle = resolveHeaderStyle(sheet);
        for (int rowIndex = 0; rowIndex < sheetHeaderLines.size(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                Cell cell = row.getCell(columnIndex);
                if (cell == null) {
                    cell = row.createCell(columnIndex, CellType.BLANK);
                }
                if (columnIndex == 0) {
                    cell.setCellValue(sheetHeaderLines.get(rowIndex));
                }
                cell.setCellStyle(headerStyle);
            }
        }
    }

    /**
     * 解析说明头单元格样式。
     *
     * @param sheet 工作表对象
     * @return 说明头单元格样式
     */
    private CellStyle resolveHeaderStyle(Sheet sheet) {
        CellStyle referenceStyle = resolveReferenceHeaderStyle(sheet);
        Workbook workbook = sheet.getWorkbook();
        if (referenceStyle == null) {
            return createFallbackHeaderStyle(workbook);
        }
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.cloneStyleFrom(referenceStyle);
        headerStyle.setWrapText(true);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setAlignment(HorizontalAlignment.LEFT);
        applyHeaderBackgroundColor(headerStyle);
        return headerStyle;
    }

    /**
     * 解析真实字段表头样式。
     *
     * @param sheet 工作表对象
     * @return 真实字段表头样式
     */
    private CellStyle resolveReferenceHeaderStyle(Sheet sheet) {
        Row headerRow = sheet.getRow(sheetHeaderLines.size());
        if (headerRow == null) {
            return null;
        }
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            Cell headerCell = headerRow.getCell(columnIndex);
            if (headerCell != null && headerCell.getCellStyle() != null) {
                return headerCell.getCellStyle();
            }
        }
        return null;
    }

    /**
     * 创建说明头兜底样式。
     *
     * @param workbook 工作簿对象
     * @return 说明头兜底样式
     */
    private CellStyle createFallbackHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);

        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFont(font);
        cellStyle.setWrapText(true);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setAlignment(HorizontalAlignment.LEFT);
        applyHeaderBackgroundColor(cellStyle);
        return cellStyle;
    }

    /**
     * 应用说明头背景颜色。
     *
     * @param cellStyle 单元格样式
     * @return 无返回值
     */
    private void applyHeaderBackgroundColor(CellStyle cellStyle) {
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        if (cellStyle instanceof XSSFCellStyle xssfCellStyle) {
            xssfCellStyle.setFillForegroundColor(
                    new XSSFColor(SHEET_HEADER_BACKGROUND_COLOR, new DefaultIndexedColorMap())
            );
        }
    }
}
