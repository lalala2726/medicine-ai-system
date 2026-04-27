package com.zhangyichuang.medicine.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author Chuang
 * <p>
 * created on 2026/2/1
 */
@Data
@Schema(description = "解析药品信息请求参数")
public class ParserMedicineInfoRequest {

    @Schema(description = "图片地址列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com/image1.jpg")
    private List<String> imageUrls;

}
