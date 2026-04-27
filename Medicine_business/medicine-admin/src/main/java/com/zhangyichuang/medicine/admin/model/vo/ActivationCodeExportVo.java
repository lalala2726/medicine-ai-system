package com.zhangyichuang.medicine.admin.model.vo;

import lombok.Builder;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.fesod.sheet.annotation.format.DateTimeFormat;

import java.util.Date;

/**
 * 激活码导出视图对象。
 */
@Data
@Builder
public class ActivationCodeExportVo {

    /**
     * 激活码明文。
     */
    @ExcelProperty(value = "激活码", index = 0)
    private String plainCode;

    /**
     * 激活码状态文案。
     */
    @ExcelProperty(value = "状态", index = 1)
    private String statusText;

    /**
     * 成功使用次数。
     */
    @ExcelProperty(value = "使用次数", index = 2)
    private Integer successUseCount;

    /**
     * 最近一次成功激活时间。
     */
    @ExcelProperty(value = "最近激活时间", index = 3)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date lastSuccessTime;

    /**
     * 最近一次成功激活客户端 IP。
     */
    @ExcelProperty(value = "最近激活 IP", index = 4)
    private String lastSuccessClientIp;

    /**
     * 最近一次成功激活用户名。
     */
    @ExcelProperty(value = "最近激活用户", index = 5)
    private String lastSuccessUserName;
}
