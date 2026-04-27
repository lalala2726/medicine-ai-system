package com.zhangyichuang.medicine.client.rpc;

import com.zhangyichuang.medicine.client.model.request.MallProductSearchRequest;
import com.zhangyichuang.medicine.client.model.vo.AssistantProductPurchaseCardsVo;
import com.zhangyichuang.medicine.client.model.vo.MallProductSearchTagFilterOptionVo;
import com.zhangyichuang.medicine.client.model.vo.MallProductSearchTagFilterVo;
import com.zhangyichuang.medicine.client.model.vo.MallProductSearchVo;
import com.zhangyichuang.medicine.client.service.MallProductService;
import com.zhangyichuang.medicine.client.service.MallProductTagService;
import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.model.constants.MallProductTagConstants;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.model.entity.MallProductTag;
import com.zhangyichuang.medicine.model.request.ClientAgentProductSearchRequest;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;
import com.zhangyichuang.medicine.rpc.client.ClientAgentProductRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 客户端智能体商品 RPC Provider。
 */
@DubboService(interfaceClass = ClientAgentProductRpcService.class, group = "medicine-client", version = "1.0.0")
@RequiredArgsConstructor
public class ClientAgentProductRpcServiceImpl implements ClientAgentProductRpcService {

    /**
     * 客户端智能体商品搜索单页最大返回条数。
     */
    private static final int MAX_PAGE_SIZE = 20;

    /**
     * 商品上架状态。
     */
    private static final int PRODUCT_STATUS_ON_SALE = 1;

    /**
     * 客户端商品服务。
     */
    private final MallProductService mallProductService;

    /**
     * 客户端商品标签服务。
     */
    private final MallProductTagService mallProductTagService;

    /**
     * 搜索商品并映射为智能体使用的精简结果。
     *
     * @param request 商品搜索参数
     * @return 搜索结果分页
     */
    @Override
    public PageResult<ClientAgentProductSearchDto> searchProducts(ClientAgentProductSearchRequest request) {
        ClientAgentProductSearchRequest safeRequest = request == null ? new ClientAgentProductSearchRequest() : request;

        MallProductSearchRequest query = new MallProductSearchRequest();
        query.setKeyword(normalizeText(safeRequest.getKeyword()));
        query.setCategoryName(normalizeText(safeRequest.getCategoryName()));
        query.setEfficacy(normalizeText(safeRequest.getUsage()));
        query.setTagIds(resolveTagIdsByNames(safeRequest.getTagNames()));
        query.setPageNum(Math.max(safeRequest.getPageNum(), 1));
        query.setPageSize(Math.min(Math.max(safeRequest.getPageSize(), 1), MAX_PAGE_SIZE));

        PageResult<MallProductSearchVo> result = mallProductService.search(query);
        if (result == null) {
            return new PageResult<>((long) query.getPageNum(), (long) query.getPageSize(), 0L, List.of());
        }
        List<ClientAgentProductSearchDto> rows = result.getRows() == null ? List.of() : result.getRows().stream()
                .map(this::toSearchDto)
                .toList();

        return new PageResult<>(result.getPageNum(), result.getPageSize(), result.getTotal(), rows);
    }

    /**
     * 查询客户端智能体商品搜索标签目录。
     *
     * @return 标签分组列表
     */
    @Override
    public List<ClientAgentProductSearchTagFilterDto> listProductSearchTagFilters() {
        List<MallProductSearchTagFilterVo> filterGroups = mallProductService.listAllEnabledSearchTagFilters();
        if (filterGroups == null || filterGroups.isEmpty()) {
            return List.of();
        }
        return filterGroups.stream()
                .filter(Objects::nonNull)
                .map(this::toProductSearchTagFilterDto)
                .toList();
    }

    /**
     * 批量查询统一药品详情。
     *
     * @param productIds 商品ID列表
     * @return 商品详情列表
     */
    @Override
    public List<ClientAgentProductDetailDto> getProductDetails(List<Long> productIds) {
        validateProductIds(productIds);
        return productIds.stream()
                .map(this::requireVisibleProduct)
                .map(this::toProductDetailDto)
                .toList();
    }

    /**
     * 查询商品购买卡片补全结果。
     *
     * @param productIds 商品ID列表
     * @return 商品购买卡片补全结果
     */
    @Override
    public ClientAgentProductCardsDto getProductCards(List<Long> productIds) {
        AssistantProductPurchaseCardsVo cards = mallProductService.getAssistantProductPurchaseCards(productIds);
        return toProductCardsDto(cards);
    }

    /**
     * 查询商品购买卡片补全结果。
     *
     * @param items 商品购买项列表
     * @return 商品购买卡片补全结果
     */
    @Override
    public ClientAgentProductPurchaseCardsDto getProductPurchaseCards(List<ClientAgentProductPurchaseQueryDto> items) {
        List<Long> productIds = items == null ? List.of() : items.stream()
                .filter(Objects::nonNull)
                .map(ClientAgentProductPurchaseQueryDto::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return emptyProductPurchaseCardsDto();
        }

        AssistantProductPurchaseCardsVo cards = mallProductService.getAssistantProductPurchaseCards(productIds);
        return toProductPurchaseCardsDto(cards, items);
    }

    /**
     * 将客户端商品卡片结果映射为 RPC DTO。
     *
     * @param source 客户端商品卡片结果
     * @return RPC DTO
     */
    private ClientAgentProductCardsDto toProductCardsDto(AssistantProductPurchaseCardsVo source) {
        if (source == null) {
            return emptyProductCardsDto();
        }
        List<ClientAgentProductCardsDto.ClientAgentProductItemDto> items =
                source.getItems() == null ? List.of() : source.getItems().stream()
                        .map(this::toProductItemDto)
                        .toList();
        return ClientAgentProductCardsDto.builder()
                .totalPrice(source.getTotalPrice())
                .items(items)
                .build();
    }

    /**
     * 将客户端商品卡片单项映射为 RPC DTO。
     *
     * @param source 客户端商品卡片单项
     * @return RPC DTO
     */
    private ClientAgentProductCardsDto.ClientAgentProductItemDto toProductItemDto(
            AssistantProductPurchaseCardsVo.AssistantProductPurchaseItemVo source
    ) {
        if (source == null) {
            return null;
        }
        return ClientAgentProductCardsDto.ClientAgentProductItemDto.builder()
                .id(source.getId())
                .name(source.getName())
                .image(source.getImage())
                .price(source.getPrice())
                .spec(source.getSpec())
                .efficacy(source.getEfficacy())
                .drugCategory(source.getDrugCategory())
                .stock(source.getStock())
                .build();
    }

    /**
     * 将客户端购买卡片结果映射为 RPC DTO。
     *
     * @param source  客户端商品卡片基础信息
     * @param queries 商品购买项列表
     * @return RPC DTO
     */
    private ClientAgentProductPurchaseCardsDto toProductPurchaseCardsDto(
            AssistantProductPurchaseCardsVo source,
            List<ClientAgentProductPurchaseQueryDto> queries
    ) {
        if (source == null || source.getItems() == null || source.getItems().isEmpty() || queries == null || queries.isEmpty()) {
            return emptyProductPurchaseCardsDto();
        }

        Map<String, AssistantProductPurchaseCardsVo.AssistantProductPurchaseItemVo> itemMap = source.getItems().stream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.hasText(item.getId()))
                .collect(Collectors.toMap(
                        AssistantProductPurchaseCardsVo.AssistantProductPurchaseItemVo::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<ClientAgentProductPurchaseCardsDto.ClientAgentProductPurchaseItemDto> items = new ArrayList<>();
        BigDecimal totalPrice = scaledPrice(BigDecimal.ZERO);
        for (ClientAgentProductPurchaseQueryDto query : queries) {
            if (query == null || query.getProductId() == null || query.getQuantity() == null || query.getQuantity() <= 0) {
                continue;
            }

            AssistantProductPurchaseCardsVo.AssistantProductPurchaseItemVo sourceItem =
                    itemMap.get(String.valueOf(query.getProductId()));
            if (sourceItem == null) {
                continue;
            }

            BigDecimal unitPrice = parsePrice(sourceItem.getPrice());
            totalPrice = totalPrice.add(unitPrice.multiply(BigDecimal.valueOf(query.getQuantity())));
            items.add(toProductPurchaseItemDto(sourceItem, query.getQuantity()));
        }

        return ClientAgentProductPurchaseCardsDto.builder()
                .totalPrice(scaledPrice(totalPrice))
                .items(items)
                .build();
    }

    /**
     * 将客户端商品搜索结果映射为 RPC DTO。
     *
     * @param source 客户端商品搜索结果
     * @return RPC DTO
     */
    private ClientAgentProductSearchDto toSearchDto(MallProductSearchVo source) {
        if (source == null) {
            return null;
        }
        return ClientAgentProductSearchDto.builder()
                .productId(source.getProductId())
                .productName(source.getProductName())
                .commonName(source.getCommonName())
                .categoryNames(source.getCategoryNames())
                .brand(source.getBrand())
                .drugCategory(source.getDrugCategory())
                .tagNames(source.getTagNames())
                .efficacy(source.getEfficacy())
                .taboo(source.getTaboo())
                .price(source.getPrice())
                .build();
    }

    /**
     * 将商城商品详情映射为统一药品详情 DTO。
     *
     * @param source 商城商品详情
     * @return 统一药品详情 DTO
     */
    private ClientAgentProductDetailDto toProductDetailDto(MallProductDetailDto source) {
        if (source == null) {
            return null;
        }
        DrugDetailDto drugDetail = source.getDrugDetail();
        return ClientAgentProductDetailDto.builder()
                .productId(source.getId())
                .productName(source.getName())
                .categoryNames(source.getCategoryNames())
                .unit(source.getUnit())
                .price(source.getPrice())
                .stock(source.getStock())
                .sales(source.getSales())
                .deliveryType(source.getDeliveryType())
                .status(source.getStatus())
                .commonName(drugDetail == null ? null : drugDetail.getCommonName())
                .brand(drugDetail == null ? null : drugDetail.getBrand())
                .drugCategory(drugDetail == null ? null : drugDetail.getDrugCategory())
                .tagNames(extractTagNames(source.getTags()))
                .efficacy(drugDetail == null ? null : drugDetail.getEfficacy())
                .taboo(drugDetail == null ? null : drugDetail.getTaboo())
                .precautions(drugDetail == null ? null : drugDetail.getPrecautions())
                .usageMethod(drugDetail == null ? null : drugDetail.getUsageMethod())
                .adverseReactions(drugDetail == null ? null : drugDetail.getAdverseReactions())
                .warmTips(drugDetail == null ? null : drugDetail.getWarmTips())
                .isOutpatientMedicine(drugDetail == null ? null : drugDetail.getIsOutpatientMedicine())
                .composition(drugDetail == null ? null : drugDetail.getComposition())
                .characteristics(drugDetail == null ? null : drugDetail.getCharacteristics())
                .packaging(drugDetail == null ? null : drugDetail.getPackaging())
                .validityPeriod(drugDetail == null ? null : drugDetail.getValidityPeriod())
                .storageConditions(drugDetail == null ? null : drugDetail.getStorageConditions())
                .productionUnit(drugDetail == null ? null : drugDetail.getProductionUnit())
                .approvalNumber(drugDetail == null ? null : drugDetail.getApprovalNumber())
                .executiveStandard(drugDetail == null ? null : drugDetail.getExecutiveStandard())
                .originType(drugDetail == null ? null : drugDetail.getOriginType())
                .instruction(drugDetail == null ? null : drugDetail.getInstruction())
                .build();
    }

    /**
     * 将商品搜索标签筛选分组映射为 RPC DTO。
     *
     * @param source 商品搜索标签筛选分组
     * @return RPC DTO
     */
    private ClientAgentProductSearchTagFilterDto toProductSearchTagFilterDto(
            MallProductSearchTagFilterVo source
    ) {
        if (source == null) {
            return null;
        }
        return ClientAgentProductSearchTagFilterDto.builder()
                .typeId(source.getTypeId())
                .typeCode(source.getTypeCode())
                .typeName(source.getTypeName())
                .options(
                        source.getOptions() == null
                                ? List.of()
                                : source.getOptions().stream()
                                .filter(Objects::nonNull)
                                .map(this::toProductSearchTagFilterOptionDto)
                                .toList()
                )
                .build();
    }

    /**
     * 将商品搜索标签筛选项映射为 RPC DTO。
     *
     * @param source 商品搜索标签筛选项
     * @return RPC DTO
     */
    private ClientAgentProductSearchTagFilterOptionDto toProductSearchTagFilterOptionDto(
            MallProductSearchTagFilterOptionVo source
    ) {
        if (source == null) {
            return null;
        }
        return ClientAgentProductSearchTagFilterOptionDto.builder()
                .tagId(source.getTagId())
                .tagName(source.getTagName())
                .count(source.getCount())
                .build();
    }

    /**
     * 将客户端购买卡片结果映射为 RPC DTO。
     *
     * @param source 客户端购买卡片结果
     * @return RPC DTO
     */
    private ClientAgentProductPurchaseCardsDto.ClientAgentProductPurchaseItemDto toProductPurchaseItemDto(
            AssistantProductPurchaseCardsVo.AssistantProductPurchaseItemVo source,
            Integer quantity
    ) {
        if (source == null) {
            return null;
        }
        BigDecimal price = parsePrice(source.getPrice());
        return ClientAgentProductPurchaseCardsDto.ClientAgentProductPurchaseItemDto.builder()
                .id(source.getId())
                .name(source.getName())
                .image(source.getImage())
                .price(price)
                .quantity(quantity)
                .spec(source.getSpec())
                .efficacy(source.getEfficacy())
                .drugCategory(source.getDrugCategory())
                .stock(source.getStock())
                .build();
    }

    /**
     * 提取商品标签名称列表。
     *
     * @param tags 商品标签列表
     * @return 标签名称列表
     */
    private List<String> extractTagNames(List<MallProductTagVo> tags) {
        if (tags == null) {
            return null;
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(MallProductTagVo::getName)
                .filter(StringUtils::hasText)
                .toList();
    }

    /**
     * 查询并校验商品是否对客户端智能体可见。
     *
     * @param productId 商品ID
     * @return 商品详情
     */
    private MallProductDetailDto requireVisibleProduct(Long productId) {
        MallProductDetailDto detail = mallProductService.getProductAndDrugInfoById(productId);
        if (!Integer.valueOf(PRODUCT_STATUS_ON_SALE).equals(detail.getStatus())) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "商品不存在");
        }
        return detail;
    }

    /**
     * 校验批量商品ID入参。
     *
     * @param productIds 商品ID列表
     * @return 无
     */
    private void validateProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "商品ID列表不能为空");
        }
        boolean hasInvalidProductId = productIds.stream()
                .anyMatch(productId -> productId == null || productId <= 0);
        if (hasInvalidProductId) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "商品ID必须大于0");
        }
    }

    /**
     * 构造空商品卡片结果。
     *
     * @return 空商品卡片 DTO
     */
    private ClientAgentProductCardsDto emptyProductCardsDto() {
        return ClientAgentProductCardsDto.builder()
                .totalPrice("0.00")
                .items(List.of())
                .build();
    }

    /**
     * 构造空购买卡片结果。
     *
     * @return 空购买卡片 DTO
     */
    private ClientAgentProductPurchaseCardsDto emptyProductPurchaseCardsDto() {
        return ClientAgentProductPurchaseCardsDto.builder()
                .totalPrice(scaledPrice(BigDecimal.ZERO))
                .items(List.of())
                .build();
    }

    /**
     * 将价格文本解析为数值价格。
     *
     * @param priceText 价格文本
     * @return 数值价格
     */
    private BigDecimal parsePrice(String priceText) {
        if (!StringUtils.hasText(priceText)) {
            return scaledPrice(BigDecimal.ZERO);
        }
        return scaledPrice(new BigDecimal(priceText));
    }

    /**
     * 统一价格精度，保留两位小数。
     *
     * @param price 原始价格
     * @return 格式化后的价格
     */
    private BigDecimal scaledPrice(BigDecimal price) {
        return (price == null ? BigDecimal.ZERO : price).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 统一清洗文本参数。
     *
     * @param value 待清洗文本
     * @return 去除首尾空白后的文本，空白文本返回 null
     */
    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 根据标签名称列表查询对应的启用标签ID列表。
     *
     * @param tagNames 标签名称列表
     * @return 匹配的启用标签ID列表，名称无匹配则返回空列表
     */
    private List<Long> resolveTagIdsByNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }
        List<String> distinctNames = tagNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (distinctNames.isEmpty()) {
            return List.of();
        }
        return mallProductTagService.lambdaQuery()
                .in(MallProductTag::getName, distinctNames)
                .eq(MallProductTag::getStatus, MallProductTagConstants.STATUS_ENABLED)
                .list()
                .stream()
                .map(MallProductTag::getId)
                .toList();
    }
}
