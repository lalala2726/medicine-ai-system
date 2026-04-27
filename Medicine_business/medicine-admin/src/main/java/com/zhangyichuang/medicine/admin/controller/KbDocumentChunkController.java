package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.DocumentChunkAddRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentChunkListRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentChunkUpdateContentRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentChunkUpdateStatusRequest;
import com.zhangyichuang.medicine.admin.model.vo.DocumentChunkVo;
import com.zhangyichuang.medicine.admin.service.KbDocumentChunkService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.entity.KbDocumentChunk;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库文档切片管理控制器。
 */
@RestController
@RequestMapping("/knowledge_base/document/chunk")
@RequiredArgsConstructor
@Tag(name = "知识库文档切片管理", description = "知识库文档切片接口")
@PreventDuplicateSubmit
public class KbDocumentChunkController extends BaseController {

    private final KbDocumentChunkService kbDocumentChunkService;

    /**
     * 分页查询指定文档下的切片列表。
     *
     * @param documentId 文档ID
     * @param request    查询参数
     * @return 切片分页结果
     */
    @GetMapping("/{documentId:\\d+}/list")
    @Operation(summary = "文档切片列表")
    @PreAuthorize("hasAuthority('system:kb_document_chunk:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listDocumentChunk(@PathVariable Long documentId,
                                                         @Validated DocumentChunkListRequest request) {
        Page<KbDocumentChunk> page = kbDocumentChunkService.listDocumentChunk(documentId, request);
        List<DocumentChunkVo> rows = copyListProperties(page, DocumentChunkVo.class);
        return getTableData(page, rows);
    }

    /**
     * 新增单个手工补充切片，并触发异步向量化。
     *
     * @param request 新增请求
     * @return 新增结果
     */
    @PostMapping
    @Operation(summary = "新增文档切片")
    @PreAuthorize("hasAuthority('system:kb_document_chunk:add') or hasRole('super_admin')")
    public AjaxResult<Void> addDocumentChunk(@Validated @RequestBody DocumentChunkAddRequest request) {
        return toAjax(kbDocumentChunkService.addDocumentChunk(request));
    }

    /**
     * 修改单个切片内容，并触发异步向量重建。
     *
     * @param request 修改请求
     * @return 修改结果
     */
    @PutMapping("/content")
    @Operation(summary = "修改文档切片内容")
    @PreAuthorize("hasAuthority('system:kb_document_chunk:update') or hasRole('super_admin')")
    public AjaxResult<Void> updateDocumentChunkContent(@Validated @RequestBody DocumentChunkUpdateContentRequest request) {
        return toAjax(kbDocumentChunkService.updateDocumentChunkContent(request));
    }

    /**
     * 修改单个切片状态。
     *
     * @param request 修改请求
     * @return 修改结果
     */
    @PutMapping("/status")
    @Operation(summary = "修改文档切片状态")
    @PreAuthorize("hasAuthority('system:kb_document_chunk:update') or hasRole('super_admin')")
    public AjaxResult<Void> updateDocumentChunkStatus(@Validated @RequestBody DocumentChunkUpdateStatusRequest request) {
        return toAjax(kbDocumentChunkService.updateDocumentChunkStatus(request));
    }

    /**
     * 删除单个切片。
     *
     * @param id 切片ID
     * @return 删除结果
     */
    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "删除文档切片")
    @PreAuthorize("hasAuthority('system:kb_document_chunk:delete') or hasRole('super_admin')")
    public AjaxResult<Void> deleteDocumentChunk(@PathVariable Long id) {
        return toAjax(kbDocumentChunkService.deleteDocumentChunk(id));
    }
}
