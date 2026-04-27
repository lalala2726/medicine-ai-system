from fastapi import APIRouter

from app.api.routes.admin_assistant import router as assistant_router
from app.api.routes.client_assistant import router as client_assistant_router
from app.api.routes.image_parse import router as image_router
from app.api.routes.knowledge_base import router as knowledge_base_router
from app.api.routes.speech_stt import router as speech_stt_router

api_router = APIRouter()
api_router.include_router(image_router)
api_router.include_router(assistant_router)
api_router.include_router(client_assistant_router)
api_router.include_router(speech_stt_router)
api_router.include_router(knowledge_base_router)
