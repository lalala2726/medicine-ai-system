package com.zhangyichuang.medicine.common.excel.support;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.fesod.sheet.FesodSheet;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 * Excel 导出支持工具。
 */
public final class ExcelExportSupport {

    /**
     * Excel 响应内容类型。
     */
    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * 内容处置响应头名称。
     */
    private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";

    /**
     * 私有构造方法。
     */
    private ExcelExportSupport() {
    }

    /**
     * 将数据导出为 Excel。
     *
     * @param response  Http 响应对象
     * @param fileName  导出文件名
     * @param sheetName 工作表名称
     * @param headClass 导出表头类型
     * @param data      导出数据集合
     * @param <T>       导出数据类型
     * @throws IOException IO 异常
     */
    public static <T> void export(HttpServletResponse response,
                                  String fileName,
                                  String sheetName,
                                  Class<T> headClass,
                                  Collection<T> data) throws IOException {
        export(response, fileName, sheetName, headClass, data, List.of(), 0);
    }

    /**
     * 将带说明头的数据导出为 Excel。
     *
     * @param response         Http 响应对象
     * @param fileName         导出文件名
     * @param sheetName        工作表名称
     * @param headClass        导出表头类型
     * @param data             导出数据集合
     * @param sheetHeaderLines 说明头文本列表
     * @param columnCount      说明头合并列数
     * @param <T>              导出数据类型
     * @return 无返回值
     * @throws IOException IO 异常
     */
    public static <T> void export(HttpServletResponse response,
                                  String fileName,
                                  String sheetName,
                                  Class<T> headClass,
                                  Collection<T> data,
                                  List<String> sheetHeaderLines,
                                  int columnCount) throws IOException {
        Assert.notNull(response, "response must not be null");
        Assert.hasText(fileName, "fileName must not be blank");
        Assert.hasText(sheetName, "sheetName must not be blank");
        Assert.notNull(headClass, "headClass must not be null");
        Assert.notNull(data, "data must not be null");
        Assert.notNull(sheetHeaderLines, "sheetHeaderLines must not be null");
        if (!sheetHeaderLines.isEmpty()) {
            Assert.isTrue(columnCount > 0, "columnCount must be greater than 0");
        }

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(EXCEL_CONTENT_TYPE);
        response.setHeader(CONTENT_DISPOSITION_HEADER, buildContentDisposition(fileName));
        var sheetWriterBuilder = FesodSheet.write(response.getOutputStream(), headClass)
                .autoCloseStream(Boolean.FALSE);
        if (!sheetHeaderLines.isEmpty()) {
            sheetWriterBuilder
                    .relativeHeadRowIndex(sheetHeaderLines.size())
                    .registerWriteHandler(new ExcelSheetHeaderWriteHandler(sheetHeaderLines, columnCount));
        }
        sheetWriterBuilder.sheet(sheetName)
                .doWrite(data);
    }

    /**
     * 构建文件下载响应头。
     *
     * @param fileName 文件名
     * @return 内容处置响应头值
     */
    private static String buildContentDisposition(String fileName) {
        return "attachment; filename*=UTF-8''" + encodeFileName(fileName);
    }

    /**
     * 对文件名执行 UTF-8 编码。
     *
     * @param fileName 文件名
     * @return 编码后的文件名
     */
    private static String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
