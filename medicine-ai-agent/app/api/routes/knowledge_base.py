from typing import Literal, Optional

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field, field_validator, model_validator

from app.core.exception.exceptions import ServiceException
from app.core.security import allow_system
from app.schemas.response import ApiResponse
from app.services.knowledge_base_service import (
    create_collection,
    delete_documents,
    delete_knowledge,
    load_collection_state,
    list_knowledge_chunks,
    release_collection_state,
    search_knowledge_fragments,
    update_document_status,
    update_document_status_by_vector_id,
)

router = APIRouter(prefix="/knowledge_base", tags=["知识库管理"])


class CreateCollectionRequest(BaseModel):
    """创建知识库请求参数"""
    knowledge_name: str = Field(
        ...,
        pattern=r"^[A-Za-z][A-Za-z0-9_]*$",
        description="知识库名称（英文/数字/下划线，字母开头）"
    )
    embedding_dim: int = Field(..., gt=0, description="向量维度")
    description: Optional[str] = Field(default="", description="知识库描述")

    @field_validator("embedding_dim")
    @classmethod
    def validate_embedding_dim(cls, value: int) -> int:
        """验证向量维度"""
        if value < 128 or value > 4096:
            raise ServiceException("向量维度必须在 128 到 4096 之间")
        if value % 2 != 0:
            raise ServiceException("向量维度必须能被 2 整除")
        return value


@router.post(path="", summary="创建知识库")
@allow_system
async def create_knowledge_base(request: CreateCollectionRequest) -> ApiResponse[dict]:
    """
    创建知识库

    Args:
        request: 创建知识库请求参数

    Returns:
        ApiResponse[dict]: 创建成功响应
    """
    create_collection(
        request.knowledge_name,
        request.embedding_dim,
        request.description or "",
    )
    return ApiResponse.success(
        data={"knowledge_name": request.knowledge_name},
        message="创建成功",
    )


class DeleteKnowledgeRequest(BaseModel):
    """删除知识库请求参数"""
    knowledge_name: str = Field(
        ..., pattern=r"^[A-Za-z][A-Za-z0-9_]*$", description="知识库名称"
    )


@router.delete("", summary="删除知识库")
@allow_system
async def delete_knowledge_base(
        request: DeleteKnowledgeRequest,
) -> ApiResponse[dict]:
    """
    删除知识库

    Args:
        request: 删除知识库请求参数

    Returns:
        ApiResponse[dict]: 删除成功响应
    """
    delete_knowledge(request.knowledge_name)
    return ApiResponse.success(
        data={"knowledge_name": request.knowledge_name},
        message="删除成功",
    )


class KnowledgeLoadRequest(BaseModel):
    """启用/关闭集合请求参数"""
    knowledge_name: str = Field(
        ...,
        pattern=r"^[A-Za-z][A-Za-z0-9_]*$",
        description="知识库名称",
    )


@router.post(path="/load", summary="启用知识库")
@allow_system
async def load_knowledge_base(
        request: KnowledgeLoadRequest,
) -> ApiResponse[dict]:
    """
    启用知识库对应集合（load collection）。

    Args:
        request: 启用请求参数。

    Returns:
        ApiResponse[dict]: 启用成功响应。
    """
    result = load_collection_state(request.knowledge_name)
    return ApiResponse.success(
        data=result,
        message="启用成功",
    )


@router.post(path="/release", summary="关闭知识库")
@allow_system
async def release_knowledge_base(
        request: KnowledgeLoadRequest,
) -> ApiResponse[dict]:
    """
    关闭知识库对应集合（release collection）。

    Args:
        request: 关闭请求参数。

    Returns:
        ApiResponse[dict]: 关闭成功响应。
    """
    result = release_collection_state(request.knowledge_name)
    return ApiResponse.success(
        data=result,
        message="关闭成功",
    )


class ListDocumentChunksRequest(BaseModel):
    """分页查询文档切片请求参数"""
    knowledge_name: str = Field(
        ..., pattern=r"^[A-Za-z][A-Za-z0-9_]*$", description="知识库名称"
    )
    document_id: int = Field(..., gt=0, description="文档ID")
    page: int = Field(default=1, gt=0, description="页码")
    page_size: int = Field(default=50, ge=1, le=100, description="每页数量")


class DocumentChunksPageResponse(BaseModel):
    """文档切片分页响应数据"""

    rows: list[dict] = Field(..., description="当前页数据列表")
    total: int = Field(..., description="数据总数")
    page_num: int = Field(..., description="当前页码")
    page_size: int = Field(..., description="每页数量")
    has_next: bool = Field(..., description="是否存在下一页")


class UpdateDocumentStatusRequest(BaseModel):
    """修改文档状态请求参数"""

    knowledge_name: str = Field(
        ...,
        pattern=r"^[A-Za-z][A-Za-z0-9_]*$",
        description="知识库名称",
    )
    vector_id: int = Field(..., gt=0, description="向量数据库主键ID")
    status: Literal[0, 1] = Field(..., description="状态：0启用，1禁用")


class UpdateChunkStatusByVectorIdRequest(BaseModel):
    """按向量主键修改切片状态请求参数"""

    vector_id: int = Field(..., gt=0, description="向量数据库主键ID")
    status: Literal[0, 1] = Field(..., description="状态：0启用，1禁用")


class DeleteDocumentsRequest(BaseModel):
    """批量删除文档请求参数"""

    knowledge_name: str = Field(
        ...,
        pattern=r"^[A-Za-z][A-Za-z0-9_]*$",
        description="知识库名称",
    )
    document_ids: list[int] = Field(
        ...,
        min_length=1,
        description="待删除的文档ID列表",
    )

    @field_validator("document_ids")
    @classmethod
    def validate_document_ids(cls, value: list[int]) -> list[int]:
        """验证文档 ID 列表并去重。"""
        normalized_ids: list[int] = []
        seen_ids: set[int] = set()
        for document_id in value:
            if document_id <= 0:
                raise ServiceException("文档ID必须大于 0")
            if document_id in seen_ids:
                continue
            seen_ids.add(document_id)
            normalized_ids.append(document_id)
        return normalized_ids


class KnowledgeSearchRequest(BaseModel):
    """结构化知识检索请求参数。"""

    question: str = Field(..., min_length=1, description="用户原始问题")
    knowledge_names: list[str] = Field(
        ...,
        min_length=1,
        description="本次检索使用的知识库名称列表",
    )
    embedding_model: str = Field(..., min_length=1, description="本次检索使用的向量模型名称")
    embedding_dim: int = Field(..., gt=0, description="本次检索使用的向量维度")
    ranking_enabled: bool = Field(..., description="本次检索是否启用重排")
    ranking_model: str | None = Field(default=None, description="本次检索使用的重排模型名称")
    top_k: int = Field(..., ge=1, le=100, description="本次检索最终返回条数")

    @field_validator("question")
    @classmethod
    def normalize_question(cls, value: str) -> str:
        """规范化问题文本。

        Args:
            value: 原始问题文本。

        Returns:
            str: 去除首尾空白后的问题文本。

        Raises:
            ValueError: 当问题为空时抛出。
        """

        normalized_question = value.strip()
        if not normalized_question:
            raise ValueError("question 不能为空")
        return normalized_question

    @field_validator("knowledge_names")
    @classmethod
    def normalize_knowledge_names(cls, value: list[str]) -> list[str]:
        """规范化知识库名称列表。

        Args:
            value: 原始知识库名称列表。

        Returns:
            list[str]: 去空白、去重且保持顺序的知识库名称列表。

        Raises:
            ValueError: 当知识库名称列表为空时抛出。
        """

        normalized_names: list[str] = []
        seen_names: set[str] = set()
        for knowledge_name in value:
            normalized_name = str(knowledge_name or "").strip()
            if not normalized_name or normalized_name in seen_names:
                continue
            seen_names.add(normalized_name)
            normalized_names.append(normalized_name)
        if not normalized_names:
            raise ValueError("knowledge_names 不能为空")
        return normalized_names

    @field_validator("embedding_model")
    @classmethod
    def normalize_embedding_model(cls, value: str) -> str:
        """规范化向量模型名称。

        Args:
            value: 原始向量模型名称。

        Returns:
            str: 去除首尾空白后的向量模型名称。

        Raises:
            ValueError: 当向量模型名称为空时抛出。
        """

        normalized_model = value.strip()
        if not normalized_model:
            raise ValueError("embedding_model 不能为空")
        return normalized_model

    @field_validator("ranking_model")
    @classmethod
    def normalize_ranking_model(cls, value: str | None) -> str | None:
        """规范化重排模型名称。

        Args:
            value: 原始重排模型名称。

        Returns:
            str | None: 去除首尾空白后的重排模型名称；为空时返回 ``None``。
        """

        if value is None:
            return None
        normalized_model = value.strip()
        return normalized_model or None

    @model_validator(mode="after")
    def validate_ranking_config(self) -> "KnowledgeSearchRequest":
        """校验重排开关与模型配置是否匹配。

        Returns:
            KnowledgeSearchRequest: 当前模型实例。

        Raises:
            ServiceException: 当重排配置不合法时抛出。
        """

        if not self.ranking_enabled and self.ranking_model is not None:
            raise ServiceException("ranking_enabled=false 时不允许传入 ranking_model")
        if self.ranking_enabled and self.ranking_model is None:
            raise ServiceException("ranking_enabled=true 时必须传入 ranking_model")
        return self


class KnowledgeSearchHitResponse(BaseModel):
    """结构化知识检索命中结果。"""

    knowledge_name: str = Field(..., description="命中结果所在的知识库名称")
    content: str = Field(..., description="命中的知识文本内容")
    score: float = Field(..., description="命中的相似度分数")
    document_id: int | None = Field(default=None, description="命中的业务文档 ID")
    chunk_index: int | None = Field(default=None, description="命中的切片序号")
    char_count: int | None = Field(default=None, description="命中的切片字符数")


class KnowledgeSearchResponseData(BaseModel):
    """结构化知识检索响应数据。"""

    hits: list[KnowledgeSearchHitResponse] = Field(
        default_factory=list,
        description="按命中顺序返回的知识片段列表",
    )


@router.get("/document/chunks/list", summary="分页查询文档切片")
@allow_system
async def list_document_chunks(
        request: ListDocumentChunksRequest = Depends(),
) -> ApiResponse[DocumentChunksPageResponse]:
    """
    分页查询文档切片

    Args:
        request: 分页查询请求参数

    Returns:
        ApiResponse[DocumentChunksPageResponse]: 分页响应数据
    """
    rows, total = list_knowledge_chunks(
        knowledge_name=request.knowledge_name,
        document_id=request.document_id,
        page_num=request.page,
        page_size=request.page_size,
    )
    has_next = (request.page * request.page_size) < total
    return ApiResponse.success(
        data=DocumentChunksPageResponse(
            rows=rows,
            total=total,
            page_num=request.page,
            page_size=request.page_size,
            has_next=has_next,
        )
    )


@router.put("/document/status", summary="修改文档状态")
@allow_system
async def update_document_chunk_status(
        request: UpdateDocumentStatusRequest,
) -> ApiResponse[dict]:
    """
    按 Milvus 主键修改文档切片状态。

    Args:
        request: 修改状态请求参数。

    Returns:
        ApiResponse[dict]: 更新成功响应。
    """
    current_vector_id = update_document_status(
        knowledge_name=request.knowledge_name,
        primary_id=request.vector_id,
        status=request.status,
    )
    return ApiResponse.success(
        data={
            "knowledge_name": request.knowledge_name,
            "vector_id": current_vector_id,
            "status": request.status,
        },
        message="更新成功",
    )


@router.put("/document/chunk/status", summary="按向量主键修改切片状态")
@allow_system
async def update_chunk_status_by_vector_id(
        request: UpdateChunkStatusByVectorIdRequest,
) -> ApiResponse[dict]:
    """
    按 Milvus 主键修改文档切片状态，自动定位所属知识库。

    Args:
        request: 修改状态请求参数。

    Returns:
        ApiResponse[dict]: 更新成功响应。
    """
    knowledge_name, current_vector_id = update_document_status_by_vector_id(
        primary_id=request.vector_id,
        status=request.status,
    )
    return ApiResponse.success(
        data={
            "knowledge_name": knowledge_name,
            "vector_id": current_vector_id,
            "status": request.status,
        },
        message="更新成功",
    )


@router.delete("/document", summary="批量删除文档")
@allow_system
async def delete_document_chunk(
        request: DeleteDocumentsRequest,
) -> ApiResponse[dict]:
    """
    批量删除文档及其在知识库中的全部切片记录。

    Args:
        request: 批量删除请求参数

    Returns:
        ApiResponse[dict]: 删除成功响应
    """
    delete_documents(
        knowledge_name=request.knowledge_name,
        document_ids=request.document_ids,
    )
    return ApiResponse.success(
        data={
            "document_ids": request.document_ids,
            "knowledge_name": request.knowledge_name,
        },
        message="删除成功",
    )


@router.post("/search", summary="结构化知识检索")
@allow_system
def search_knowledge(
        request: KnowledgeSearchRequest,
) -> ApiResponse[KnowledgeSearchResponseData]:
    """执行结构化知识检索并返回命中片段。

    Args:
        request: 结构化知识检索请求参数。

    Returns:
        ApiResponse[KnowledgeSearchResponseData]: 结构化知识检索结果。
    """

    hits = search_knowledge_fragments(
        question=request.question,
        knowledge_names=request.knowledge_names,
        embedding_model=request.embedding_model,
        embedding_dim=request.embedding_dim,
        ranking_enabled=request.ranking_enabled,
        ranking_model=request.ranking_model,
        top_k=request.top_k,
    )
    return ApiResponse.success(
        data=KnowledgeSearchResponseData(
            hits=[
                KnowledgeSearchHitResponse(**hit.to_dict())
                for hit in hits
            ]
        ),
        message="检索成功",
    )
