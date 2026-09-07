#!/usr/bin/env python3
import json
import os
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse


COUNTERS = {"kakao_calls": 0, "analysis_calls": 0, "pdf_calls": 0}
LOCK = threading.Lock()


class Server(ThreadingHTTPServer):
    request_queue_size = 256
    daemon_threads = True


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/v2/local/search/address.json":
            return self.delayed_json("kakao_calls", "KAKAO_DELAY_MS", {
                "documents": [{"address": {
                    "address_name": "서울특별시 강남구 테헤란로 427",
                    "region_1depth_name": "서울",
                    "region_2depth_name": "강남구",
                    "region_3depth_name": "삼성동",
                    "main_address_no": "143",
                    "sub_address_no": "48"
                }}]
            })
        if path == "/metrics":
            with LOCK:
                return self.json_response(dict(COUNTERS))
        return self.json_response({"path": path}, 404)

    def do_POST(self):
        path = urlparse(self.path).path
        if path == "/reset":
            with LOCK:
                for key in COUNTERS:
                    COUNTERS[key] = 0
            return self.json_response({"reset": True})
        if path == "/engine/analyze":
            return self.delayed_json("analysis_calls", "ANALYSIS_DELAY_MS", {
                "riskLevel": "SAFE",
                "rawData": "{\"source\":\"free-diagnosis-k6\"}",
                "mainReason": "성능 검증용 분석",
                "ltvScore": 80
            })
        if path == "/engine/generate-pdf":
            self.increment("pdf_calls")
            time.sleep(int(os.environ.get("PDF_DELAY_MS", "0")) / 1000)
            body = b"%PDF-1.4\n% free diagnosis k6\n"
            self.send_response(200)
            self.send_header("Content-Type", "application/pdf")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            return self.wfile.write(body)
        return self.json_response({"path": path}, 404)

    def delayed_json(self, counter, delay, body):
        self.increment(counter)
        time.sleep(int(os.environ.get(delay, "0")) / 1000)
        return self.json_response(body)

    def increment(self, key):
        with LOCK:
            COUNTERS[key] += 1

    def json_response(self, body, status=200):
        data = json.dumps(body).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, format, *args):
        return


Server(("0.0.0.0", int(os.environ.get("PORT", "18081"))), Handler).serve_forever()
