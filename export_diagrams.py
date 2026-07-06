import os
import sys
import zlib
import base64
import urllib.request
import urllib.parse
import shutil
import subprocess
import time

# Đảm bảo terminal output mã hóa UTF-8 để không bị lỗi Unicode trên Windows
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

# Configurations
WORKSPACE_DIR = os.path.dirname(os.path.abspath(__file__))
DIAGRAMS_DIR = os.path.join(WORKSPACE_DIR, "diagrams")
USECASES_DIR = os.path.join(WORKSPACE_DIR, "UseCase")

OUTPUT_DIAGRAMS_DIR = os.path.join(WORKSPACE_DIR, "exported_images", "diagrams")
OUTPUT_USECASES_DIR = os.path.join(WORKSPACE_DIR, "exported_images", "usecases")

# Draw.io background: 'none' for transparent, or hex color like '#ffffff'
BACKGROUND_COLOR = "#ffffff" 

def find_drawio_cli():
    """Tự động tìm kiếm đường dẫn draw.io.exe trên Windows."""
    # 1. Kiểm tra trong PATH
    drawio_path = shutil.which("draw.io") or shutil.which("draw.io.exe")
    if drawio_path:
        return drawio_path

    # 2. Kiểm tra các đường dẫn mặc định trên Windows
    user_profile = os.environ.get("USERPROFILE", "")
    program_files = os.environ.get("PROGRAMFILES", "C:\\Program Files")
    program_files_x86 = os.environ.get("PROGRAMFILES(X86)", "C:\\Program Files (x86)")
    local_app_data = os.environ.get("LOCALAPPDATA", "")

    candidate_paths = [
        os.path.join(program_files, "draw.io", "draw.io.exe"),
        os.path.join(program_files_x86, "draw.io", "draw.io.exe"),
    ]
    if user_profile:
        candidate_paths.append(os.path.join(user_profile, "AppData", "Local", "Programs", "draw.io", "draw.io.exe"))
    if local_app_data:
        candidate_paths.append(os.path.join(local_app_data, "Programs", "draw.io", "draw.io.exe"))

    for path in candidate_paths:
        if os.path.exists(path):
            return path
            
    return None

def export_via_cli(cli_path, input_path, output_path):
    """Xuất file .drawio sang PNG sử dụng CLI của Draw.io Desktop."""
    try:
        # draw.io -x -f png -o <output> <input>
        cmd = [cli_path, "-x", "-f", "png", "-o", output_path, input_path]
        result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=30)
        if result.returncode == 0:
            return True, None
        else:
            return False, f"CLI error (code {result.returncode}): {result.stderr or result.stdout}"
    except Exception as e:
        return False, str(e)

def export_via_api(input_path, output_path):
    """Xuất file .drawio sang PNG thông qua API online của Draw.io (exp.draw.io)."""
    try:
        with open(input_path, "r", encoding="utf-8") as f:
            xml_data = f.read()

        # Nén raw deflate (không có zlib header/footer)
        compressor = zlib.compressobj(zlib.Z_DEFAULT_COMPRESSION, zlib.DEFLATED, -15)
        deflated_data = compressor.compress(xml_data.encode("utf-8")) + compressor.flush()

        # Mã hóa Base64
        b64_data = base64.b64encode(deflated_data).decode("utf-8")

        # Payload gửi đến API
        payload = {
            "format": "png",
            "xml": b64_data,
            "bg": BACKGROUND_COLOR,
            "w": "0",
            "h": "0",
            "base64": "0"  # Trả về binary trực tiếp
        }
        
        data = urllib.parse.urlencode(payload).encode("utf-8")
        url = "https://exp.draw.io/"
        req = urllib.request.Request(url, data=data, headers={"User-Agent": "Mozilla/5.0"})
        
        # Gọi API với timeout 15 giây
        with urllib.request.urlopen(req, timeout=15) as response:
            image_bytes = response.read()
            
        with open(output_path, "wb") as f:
            f.write(image_bytes)
            
        return True, None
    except Exception as e:
        return False, str(e)

def main():
    print("==================================================")
    print("       DRAW.IO DIAGRAM EXPORT SCRIPT              ")
    print("==================================================")
    
    # 1. Tìm Draw.io CLI
    cli_path = find_drawio_cli()
    if cli_path:
        print(f"[+] Tìm thấy Draw.io Desktop tại: {cli_path}")
        print("[+] Sẽ sử dụng Local CLI để xuất ảnh nhanh hơn và offline.")
        use_cli = True
    else:
        print("[-] Không tìm thấy Draw.io Desktop cài đặt cục bộ.")
        print("[*] Sẽ sử dụng Online API (https://exp.draw.io) để xuất ảnh (yêu cầu kết nối Internet).")
        use_cli = False
    
    # Tạo thư mục đầu ra
    os.makedirs(OUTPUT_DIAGRAMS_DIR, exist_ok=True)
    os.makedirs(OUTPUT_USECASES_DIR, exist_ok=True)
    
    # Thu thập danh sách file cần chuyển đổi
    # Cấu trúc: (file_nguon, file_dich)
    tasks = []
    
    # Activity Diagrams (Tìm kiếm đệ quy)
    if os.path.exists(DIAGRAMS_DIR):
        for root_dir, dirs, files in os.walk(DIAGRAMS_DIR):
            for file in files:
                if file.endswith(".drawio"):
                    input_file = os.path.join(root_dir, file)
                    rel_path = os.path.relpath(root_dir, DIAGRAMS_DIR)
                    if rel_path == ".":
                        out_dir = OUTPUT_DIAGRAMS_DIR
                    else:
                        out_dir = os.path.join(OUTPUT_DIAGRAMS_DIR, rel_path)
                    os.makedirs(out_dir, exist_ok=True)
                    output_name = file[:-7] + ".png"
                    output_file = os.path.join(out_dir, output_name)
                    tasks.append((input_file, output_file, "Activity Diagram"))
                
    # UseCase Diagrams (Tìm kiếm đệ quy)
    if os.path.exists(USECASES_DIR):
        for root_dir, dirs, files in os.walk(USECASES_DIR):
            for file in files:
                if file.endswith(".drawio"):
                    input_file = os.path.join(root_dir, file)
                    rel_path = os.path.relpath(root_dir, USECASES_DIR)
                    if rel_path == ".":
                        out_dir = OUTPUT_USECASES_DIR
                    else:
                        out_dir = os.path.join(OUTPUT_USECASES_DIR, rel_path)
                    os.makedirs(out_dir, exist_ok=True)
                    output_name = file[:-7] + ".png"
                    output_file = os.path.join(out_dir, output_name)
                    tasks.append((input_file, output_file, "UseCase Diagram"))

    total_tasks = len(tasks)
    print(f"\n[+] Tìm thấy tổng cộng {total_tasks} file .drawio cần xuất ảnh.")
    if total_tasks == 0:
        print("[-] Không tìm thấy file .drawio nào.")
        return

    success_count = 0
    fail_count = 0
    failures = []

    print("\nBắt đầu xuất ảnh...")
    for idx, (input_path, output_path, group) in enumerate(tasks, start=1):
        rel_in = os.path.relpath(input_path, WORKSPACE_DIR)
        rel_out = os.path.relpath(output_path, WORKSPACE_DIR)
        
        print(f"[{idx}/{total_tasks}] [{group}] Đang xử lý: {os.path.basename(input_path)}...", end="", flush=True)
        
        start_time = time.time()
        if use_cli:
            success, err = export_via_cli(cli_path, input_path, output_path)
        else:
            success, err = export_via_api(input_path, output_path)
            # Thêm độ trễ nhỏ để tránh spam API liên tục
            time.sleep(0.5)
            
        elapsed = time.time() - start_time
        
        if success:
            print(f" OK ({elapsed:.2f}s)")
            success_count += 1
        else:
            print(f" THẤT BẠI! Lỗi: {err}")
            fail_count += 1
            failures.append((rel_in, err))

    print("\n==================================================")
    print("               KẾT QUẢ XUẤT ẢNH                   ")
    print("==================================================")
    print(f"- Thành công: {success_count}/{total_tasks}")
    print(f"- Thất bại: {fail_count}/{total_tasks}")
    print(f"- Thư mục ảnh diagrams: {OUTPUT_DIAGRAMS_DIR}")
    print(f"- Thư mục ảnh usecases: {OUTPUT_USECASES_DIR}")
    
    if failures:
        print("\nDanh sách các file bị lỗi:")
        for file, err in failures:
            print(f"  * {file}: {err}")
            
    print("==================================================")

if __name__ == "__main__":
    main()
