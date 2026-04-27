package com.zhangyichuang.medicine.client.rpc;

import com.zhangyichuang.medicine.client.service.MallAfterSaleService;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.AfterSaleTimelineDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentAfterSaleEligibilityDto;
import com.zhangyichuang.medicine.model.request.ClientAgentAfterSaleEligibilityRequest;
import com.zhangyichuang.medicine.model.vo.AfterSaleDetailVo;
import com.zhangyichuang.medicine.model.vo.AfterSaleTimelineVo;
import com.zhangyichuang.medicine.rpc.client.ClientAgentAfterSaleRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 客户端智能体售后 RPC Provider。
 */
@DubboService(interfaceClass = ClientAgentAfterSaleRpcService.class, group = "medicine-client", version = "1.0.0")
@RequiredArgsConstructor
public class ClientAgentAfterSaleRpcServiceImpl implements ClientAgentAfterSaleRpcService {

    /**
     * 客户端售后服务。
     */
    private final MallAfterSaleService mallAfterSaleService;

    /**
     * 按用户范围查询售后详情并转换为共享 DTO。
     *
     * @param afterSaleNo 售后单号
     * @param userId      当前用户ID
     * @return 售后详情 DTO
     */
    @Override
    public AfterSaleDetailDto getAfterSaleDetail(String afterSaleNo, Long userId) {
        AfterSaleDetailVo detailVo = mallAfterSaleService.getAfterSaleDetail(afterSaleNo, userId);
        return toAfterSaleDetailDto(detailVo);
    }

    /**
     * 校验当前用户订单或订单项是否满足售后资格。
     *
     * @param request 校验请求
     * @param userId  当前用户ID
     * @return 售后资格
     */
    @Override
    public ClientAgentAfterSaleEligibilityDto checkAfterSaleEligibility(ClientAgentAfterSaleEligibilityRequest request, Long userId) {
        ClientAgentAfterSaleEligibilityRequest safeRequest =
                request == null ? new ClientAgentAfterSaleEligibilityRequest() : request;
        return mallAfterSaleService.checkAfterSaleEligibility(safeRequest, userId);
    }

    /**
     * 将客户端售后详情转换为共享 DTO。
     *
     * @param source 客户端售后详情
     * @return 共享 DTO
     */
    private AfterSaleDetailDto toAfterSaleDetailDto(AfterSaleDetailVo source) {
        if (source == null) {
            return null;
        }
        AfterSaleDetailDto target = BeanCotyUtils.copyProperties(source, AfterSaleDetailDto.class);
        target.setProductInfo(toProductInfoDto(source.getProductInfo()));
        target.setTimeline(toTimelineDtos(source.getTimeline()));
        return target;
    }

    /**
     * 映射售后商品信息。
     *
     * @param source 客户端售后商品信息
     * @return 共享 DTO 商品信息
     */
    private AfterSaleDetailDto.ProductInfo toProductInfoDto(AfterSaleDetailVo.ProductInfo source) {
        return BeanCotyUtils.copyProperties(source, AfterSaleDetailDto.ProductInfo.class);
    }

    /**
     * 映射售后时间线列表。
     *
     * @param source 客户端售后时间线
     * @return 共享 DTO 时间线
     */
    private List<AfterSaleTimelineDto> toTimelineDtos(List<AfterSaleTimelineVo> source) {
        return BeanCotyUtils.copyListProperties(source, AfterSaleTimelineDto.class);
    }

}
