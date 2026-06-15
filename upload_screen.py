import os
import sys
import json
import base64
import urllib.request
import urllib.parse

# Configurations
WORKSPACE_DIR = os.path.dirname(os.path.abspath(__file__))
FILE_TO_UPLOAD = os.path.join(WORKSPACE_DIR, "stitch_h_th_ng_qu_n_l_chi_nh_nh", "login.html")
PROJECT_ID = "6899988817137237077"
SCREEN_TITLE = "Đăng nhập"

def get_api_key_from_mcp_config():
    """Tự động tìm kiếm API Key của Stitch trong file cấu hình mcp_config.json của Antigravity."""
    user_profile = os.environ.get("USERPROFILE", "")
    if not user_profile:
        user_profile = os.path.expanduser("~")
        
    candidate_paths = [
        os.path.join(user_profile, ".gemini", "config", "mcp_config.json"),
        os.path.join(user_profile, ".gemini", "antigravity", "mcp_config.json"),
        os.path.join(user_profile, ".gemini", "antigravity-ide", "mcp_config.json"),
    ]
    
    for path in candidate_paths:
        if os.path.exists(path):
            try:
                with open(path, "r", encoding="utf-8") as f:
                    data = json.load(f)
                
                mcp_servers = data.get("mcpServers", {})
                stitch_config = mcp_servers.get("stitch", {})
                
                # 1. Tìm trong env
                env = stitch_config.get("env", {})
                for k, v in env.items():
                    if "key" in k.lower() or "api" in k.lower():
                        return v
                
                # 2. Tìm trong args
                args = stitch_config.get("args", [])
                for idx, arg in enumerate(args):
                    if arg in ("--api-key", "-api-key", "api-key") and idx + 1 < len(args):
                        return args[idx + 1]
                        
            except Exception as e:
                print(f"[!] Lỗi khi đọc file {path}: {e}")
                
    return None

def main():
    print("==================================================")
    # Configure console encoding to avoid Windows Unicode errors
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    print("[*] Đang tự động tìm Stitch API Key từ cấu hình Antigravity...")
    api_key = get_api_key_from_mcp_config()
    
    if not api_key:
        print("[-] Không tìm thấy Stitch API Key tự động.")
        print("[*] Vui lòng nhập API Key thủ công hoặc chạy lại khi đã cấu hình MCP.")
        return
        
    print("[+] Tìm thấy API Key thành công!")
    print(f"[*] File cần tải lên: {FILE_TO_UPLOAD}")
    print(f"[*] ID dự án: {PROJECT_ID}")
    
    if not os.path.exists(FILE_TO_UPLOAD):
        print(f"[-] Lỗi: Không tìm thấy file {FILE_TO_UPLOAD}")
        return
        
    # Đọc và mã hóa HTML
    with open(FILE_TO_UPLOAD, "rb") as f:
        b64_data = base64.b64encode(f.read()).decode("utf-8")
        
    # Payload
    screen_request = {
        "screen": {
            "htmlCode": {
                "fileContentBase64": b64_data,
                "mimeType": "text/html",
            },
            "screenType": "DOCUMENT",
            "isCreatedByClient": True,
            "title": SCREEN_TITLE
        }
    }
    
    payload = {
        "parent": f"projects/{PROJECT_ID}",
        "requests": [screen_request],
        "createScreenInstances": True,
    }
    
    url = f"https://stitch.googleapis.com/v1/projects/{PROJECT_ID}/screens:batchCreate"
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={
            "Content-Type": "application/json",
            "X-Goog-Api-Key": api_key,
        },
        method="POST",
    )
    
    print("[*] Đang gửi yêu cầu tải lên màn hình...")
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            body = resp.read().decode("utf-8")
            response_data = json.loads(body)
            print("[+] Tải lên màn hình thành công!")
            print(json.dumps(response_data, indent=2, ensure_ascii=False))
    except Exception as e:
        if hasattr(e, "read"):
            error_body = e.read().decode("utf-8")
            print(f"[-] HTTP Error: {e.code} - {e.reason}")
            print(f"[-] Chi tiết: {error_body}")
        else:
            print(f"[-] Lỗi kết nối: {e}")

if __name__ == "__main__":
    main()
