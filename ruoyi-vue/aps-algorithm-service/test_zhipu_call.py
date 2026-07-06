import json
import os
import time
import urllib.error
import urllib.request


ZHIPU_ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
MODEL_NAME = "glm-4-flash"


def main():
    api_key = os.getenv("ZHIPU_API_KEY", "").strip()
    print(f"ZHIPU_API_KEY loaded: {bool(api_key)}")
    if not api_key:
        print("success: false")
        print("errorType: MissingApiKey")
        print("message: Please set ZHIPU_API_KEY environment variable.")
        return

    payload = {
        "model": MODEL_NAME,
        "messages": [
            {"role": "user", "content": "请回复：连接成功"}
        ],
        "temperature": 0.1,
    }
    request = urllib.request.Request(
        ZHIPU_ENDPOINT,
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            body = json.loads(response.read().decode("utf-8"))
        elapsed_ms = int((time.perf_counter() - started) * 1000)
        content = body.get("choices", [{}])[0].get("message", {}).get("content", "")
        print("success: true")
        print(f"model: {MODEL_NAME}")
        print(f"elapsedMs: {elapsed_ms}")
        print(f"response: {content}")
    except Exception as exc:
        elapsed_ms = int((time.perf_counter() - started) * 1000)
        print("success: false")
        print(f"elapsedMs: {elapsed_ms}")
        print(f"errorType: {type(exc).__name__}")
        if isinstance(exc, urllib.error.HTTPError):
            print(f"httpStatus: {exc.code}")
        print(f"message: {exc}")


if __name__ == "__main__":
    main()
