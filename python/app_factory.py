import time

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, Response

from routers.engine import router as engine_router


def _build_validation_message(exc: RequestValidationError) -> list[str]:
    messages: list[str] = []
    for error in exc.errors():
        loc = [str(part) for part in error.get("loc", []) if part != "body"]
        field_name = loc[-1] if loc else "request"
        error_type = error.get("type")
        if error_type == "missing":
            messages.append(f"{field_name} 필드는 필수입니다.")
            continue
        messages.append(str(error.get("msg", "요청 데이터가 올바르지 않습니다.")))
    return list(dict.fromkeys(messages))


def create_app(include_ocr: bool = True) -> FastAPI:
    app = FastAPI(
        title="부동산 권리 분석 서비스",
        description=(
            "Java Spring Boot 메인 서버와 통신하는 FastAPI 엔진 서버.\n\n"
            "- `POST /engine/analyze` : 파싱된 등기부 → risk / recovery / diff JSON\n"
            "- `POST /engine/generate-pdf` : 분석 JSON → PDF 보고서 바이너리"
        ),
        version="1.0.0",
    )

    @app.middleware("http")
    async def add_process_time_header(request: Request, call_next) -> Response:
        start = time.perf_counter()
        response = await call_next(request)
        elapsed = time.perf_counter() - start
        response.headers["X-Process-Time"] = f"{elapsed:.3f}"
        return response

    @app.exception_handler(RequestValidationError)
    async def handle_request_validation_error(request: Request, exc: RequestValidationError) -> JSONResponse:
        messages = _build_validation_message(exc)
        return JSONResponse(
            status_code=422,
            content={
                "code": "INVALID_PDF_REQUEST",
                "message": messages[0] if len(messages) == 1 else "; ".join(messages),
                "details": messages,
            },
        )

    app.include_router(engine_router)

    if include_ocr:
        from routers.ocr import router as ocr_router
        app.include_router(ocr_router)

    @app.get("/health", summary="헬스체크", tags=["Infra"])
    async def health_check() -> dict:
        return {"status": "ok", "service": "pdf-generator", "port": 8000}

    return app
