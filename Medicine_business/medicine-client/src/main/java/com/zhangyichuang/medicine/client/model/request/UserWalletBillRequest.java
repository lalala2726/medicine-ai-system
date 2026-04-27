package com.zhangyichuang.medicine.client.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/6
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserWalletBillRequest extends PageRequest {

    @Schema(description = "开始时间", example = "2025-11-06 06:46:00")
    private Date startTime;

    @Schema(description = "结束时间", example = "2025-11-06 06:46:00")
    private Date endTime;
}
