#!/usr/bin/env python3
import json
import os
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse


COUNTERS = {
    "kakao_calls": 0,
    "diff_analysis_calls": 0,
    "diff_pdf_calls": 0,
}
LOCK = threading.Lock()


def delay_seconds(name):
    return int(os.environ.get(name, "0")) / 1000


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/v2/local/search/address.json":
            self.increment("kakao_calls")
            time.sleep(delay_seconds("KAKAO_DELAY_MS"))
            return self.json_response({
                "documents": [{
                    "address": {
                        "address_name": "서울특별시 강남구 테헤란로 427",
                        "region_1depth_name": "서울",
                        "region_2depth_name": "강남구",
                        "region_3depth_name": "삼성동",
                        "main_address_no": "143",
                        "sub_address_no": "48"
                    }
                }]
            })
        if path == "/metrics":
            with LOCK:
                body = dict(COUNTERS)
            return self.json_response(body)
        return self.not_found()

    def do_POST(self):
        path = urlparse(self.path).path
        if path == "/reset":
            with LOCK:
                for key in COUNTERS:
                    COUNTERS[key] = 0
            return self.json_response({"reset": True})
        if path == "/engine/analyze/mock":
            self.increment("diff_analysis_calls")
            time.sleep(delay_seconds("DIFF_ANALYSIS_DELAY_MS"))
            return self.json_response({
                "riskLevel": "SAFE",
                "rawData": "{\"risk\":\"safe\",\"source\":\"performance-stub\"}",
                "mainReason": "성능 테스트용 동일 등기 변동",
                "ltvScore": 80
            })
        if path == "/engine/generate-pdf/diff":
            self.increment("diff_pdf_calls")
            time.sleep(delay_seconds("DIFF_PDF_DELAY_MS"))
            body = b"%PDF-1.4\n% performance stub\n"
            self.send_response(200)
            self.send_header("Content-Type", "application/pdf")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        return self.not_found()

    def increment(self, key):
        with LOCK:
            COUNTERS[key] += 1

    def json_response(self, data, status=200):
        body = json.dumps(data).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def not_found(self):
        self.json_response({"message": "not found", "path": self.path}, 404)

    def log_message(self, format, *args):
        return


if __name__ == "__main__":
    port = int(os.environ.get("PORT", "18081"))
    server = ThreadingHTTPServer(("0.0.0.0", port), Handler)
    print(f"external-api-stub listening on {port}", flush=True)
    server.serve_forever()
