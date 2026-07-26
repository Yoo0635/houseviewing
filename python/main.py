"""
부동산 권리 분석 PDF 생성 마이크로서비스 — FastAPI 진입점

실행: uvicorn main:app --reload --port 8000
"""
from app_factory import create_app

app = create_app()
