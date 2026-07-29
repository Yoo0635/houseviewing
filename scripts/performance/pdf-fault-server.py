import os
import time

from fastapi import FastAPI, Response

app = FastAPI()


@app.get("/health")
def health():
    return {"status": "UP", "mode": os.getenv("PDF_FAULT_MODE", "success")}


@app.post("/engine/generate-pdf")
def generate_pdf():
    mode = os.getenv("PDF_FAULT_MODE", "success")
    if mode == "503":
        return Response(content='{"message":"forced pdf failure"}', status_code=503, media_type="application/json")
    if mode == "timeout":
        time.sleep(float(os.getenv("PDF_FAULT_SLEEP_SECONDS", "20")))
    return Response(content=b"%PDF-1.4\n%house-viewing-test\n", media_type="application/pdf")


@app.post("/engine/generate-pdf/diff")
def generate_diff_pdf():
    return generate_pdf()
