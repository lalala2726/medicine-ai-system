from pydantic import BaseModel, Field


class PageRequest(BaseModel):
    """
    分页请求参数模型
    
    用于处理分页查询的请求参数，包含页号和每页大小信息。
    
    Attributes:
        page_num (int): 页号，从1开始，必须大于等于1
        page_size (int): 每页大小，必须在1-100之间
    """
    page_num: int = Field(..., ge=1, description="页号")
    page_size: int = Field(..., ge=1, le=100, description="每页大小")
