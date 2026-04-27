package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.dto.KnowledgeBaseDocumentDto;
import com.zhangyichuang.medicine.admin.model.request.DocumentDeleteRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentListRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentUpdateFileNameRequest;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseImportRequest;
import com.zhangyichuang.medicine.admin.model.vo.KnowledgeBaseDocumentVo;
import com.zhangyichuang.medicine.admin.service.KbDocumentService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库文档管理控制器
 * <p>
 * 提供文档列表、详情、删除和导入能力。
 *
 * @author Chuang
 * <p>
 * created on 2025/12/6
 */
@RestController
@RequestMapping("/knowledge_base/document")
@RequiredArgsConstructor
@Tag(name = "知识库文档管理", description = "知识库文档接口")
@PreventDuplicateSubmit
public class KbDocumentController extends BaseController {

    private final KbDocumentService kbDocumentService;

    /**
     * 查询文档列表
     *
     * @param knowledgeBaseId 知识库ID
     * @param request         查询参数
     * @return 文档列表分页
     */
    @GetMapping("/{knowledgeBaseId:\\d+}/list")
    @Operation(summary = "文档列表")
    @PreAuthorize("hasAuthority('system:kb_document:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listDocument(@PathVariable Long knowledgeBaseId,
                                                    @Validated DocumentListRequest request) {
        Page<KnowledgeBaseDocumentDto> page = kbDocumentService.listDocument(knowledgeBaseId, request);
        return getTableData(page, toKnowledgeBaseDocumentVo(page.getRecords()));
    }

    /**
     * 查询文档详情
     *
     * @param id 文档ID
     * @return 文档详情
     */
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "文档详情")
    @PreAuthorize("hasAuthority('system:kb_document:query') or hasRole('super_admin')")
    public AjaxResult<KnowledgeBaseDocumentVo> getDocumentById(@PathVariable Long id) {
        KnowledgeBaseDocumentDto document = kbDocumentService.getDocumentDetailById(id);
        return success(toKnowledgeBaseDocumentVo(document));
    }

    /**
     * 修改文档文件名
     *
     * @param request 修改请求
     * @return 修改结果
     */
    @PutMapping("/file_name")
    @Operation(summary = "修改文档文件名")
    @PreAuthorize("hasAuthority('system:kb_document:update') or hasRole('super_admin')")
    public AjaxResult<Void> updateDocumentFileName(@Validated @RequestBody DocumentUpdateFileNameRequest request) {
        return toAjax(kbDocumentService.updateDocumentFileName(request));
    }

    /**
     * 删除文档
     *
     * @param request 删除参数
     * @return 删除结果
     */
    @DeleteMapping
    @Operation(summary = "删除文档")
    @PreAuthorize("hasAuthority('system:kb_document:delete') or hasRole('super_admin')")
    public AjaxResult<Void> deleteDocument(@Validated @RequestBody DocumentDeleteRequest request) {
        boolean result = kbDocumentService.deleteDocuments(request);
        return toAjax(result);
    }

    /**
     * 导入文档
     *
     * @param request 导入请求
     * @return 导入结果
     */
    @PostMapping("/import")
    @Operation(summary = "导入文档")
    @PreAuthorize("hasAuthority('system:kb_document:import') or hasRole('super_admin')")
    public AjaxResult<Void> importDocument(@Validated @RequestBody KnowledgeBaseImportRequest request) {
        kbDocumentService.importDocument(request);
        return success();
    }

    private List<KnowledgeBaseDocumentVo> toKnowledgeBaseDocumentVo(List<KnowledgeBaseDocumentDto> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        List<KnowledgeBaseDocumentVo> rows = new ArrayList<>(records.size());
        for (KnowledgeBaseDocumentDto dto : records) {
            if (dto == null) {
                continue;
            }
            rows.add(toKnowledgeBaseDocumentVo(dto));
        }
        return rows;
    }

    private KnowledgeBaseDocumentVo toKnowledgeBaseDocumentVo(KnowledgeBaseDocumentDto dto) {
        KnowledgeBaseDocumentVo vo = copyProperties(dto, KnowledgeBaseDocumentVo.class);
        vo.setChunkCount(dto.getChunkCount());
        return vo;
    }

}
