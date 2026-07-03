# Quy tắc an toàn khi thao tác Git trong Pair-Programming

Để bảo vệ thành quả lao động của người dùng và tránh làm mất các đoạn code chưa commit (uncommitted changes), trợ lý AI cần tuân thủ nghiêm ngặt các bước sau:

## 1. Nguyên tắc cốt lõi
* **Không bao giờ** chạy lệnh `git checkout` hoặc `git reset` trực tiếp trên một file/thư mục khi chưa kiểm tra trạng thái thay đổi của file đó.
* Việc tự ý khôi phục file có thể xóa sạch công sức code cả ngày của người dùng nếu họ chưa commit hoặc chưa push lên repository.

## 2. Quy trình xử lý lỗi an toàn
Khi phát hiện code bị lỗi hoặc muốn hoàn tác các thay đổi do AI thực hiện:

### Bước 1: Kiểm tra thay đổi bằng `git diff`
Chạy lệnh kiểm tra sự khác biệt của file so với commit gần nhất:
```powershell
git diff <đường-dẫn-file>
```

### Bước 2: Phân tích kết quả diff
* **Trường hợp A (Có thay đổi của User)**: Nếu trong file có các dòng code do người dùng chỉnh sửa mà chưa commit, **tuyệt đối không dùng git checkout**. Thay vào đó, AI phải tự dùng các công cụ chỉnh sửa file (ví dụ `replace_file_content`) để sửa chữa thủ công các phần lỗi của mình, giữ nguyên code của người dùng.
* **Trường hợp B (Chỉ có thay đổi lỗi của AI)**: Nếu kết quả diff xác nhận 100% các thay đổi chỉ gồm những đoạn lỗi do chính AI vừa thêm vào ở các lượt trước (và người dùng không có thay đổi nào chưa commit trong file đó), AI mới có thể tiến hành khôi phục.

* Ưu tiên việc sửa chữa lỗi cú pháp bằng cách thay thế nội dung (replace content) chính xác thay vì rollback toàn bộ file.

## 3. Nguyên tắc khi giải thích code và phát hiện lỗi ngoài phạm vi
* **Không tự ý sửa đổi code khi chỉ được hỏi giải thích**: Khi người dùng đặt câu hỏi tìm hiểu cơ chế hoạt động (ví dụ: "sao lại có tính năng này...", "đoạn này chạy thế nào..."), AI chỉ được phân tích và giải thích rõ bản chất hoạt động của code. Tuyệt đối không tự ý thực hiện bất kỳ thao tác chỉnh sửa file nào.
* **Không tự ý sửa lỗi nằm ngoài phạm vi yêu cầu**: Trong quá trình đọc code phụ trợ, nếu phát hiện lỗi logic hoặc bug ở các file khác, AI chỉ được báo cáo/cảnh báo vấn đề đó cho người dùng. Không tự ý sửa đổi khi chưa nhận được yêu cầu cụ thể.
* **Xác nhận nghiệp vụ trước khi kết luận lỗi**: Nhiều đoạn code hoạt động có vẻ bất thường nhưng thực chất là thiết kế nghiệp vụ đặc thù của dự án. AI không được tự áp đặt logic chủ quan để chỉnh sửa khi chưa xác nhận mong muốn của người dùng.

## 4. Quy tắc tương tác và phản hồi câu hỏi của người dùng
* **Trả lời trực tiếp và ngay lập tức**: Khi người dùng đặt câu hỏi (giải thích nghiệp vụ, hỏi lý do, nguồn gốc code...), AI phải phản hồi trực tiếp ngay lập tức. Tuyệt đối không tự ý chạy các công cụ phụ trợ ngầm (truy vấn DB, chạy lệnh hệ thống...) làm chậm hoặc trì hoãn câu trả lời, trừ khi người dùng yêu cầu rõ ràng.
* **Không tự ý truy cập cơ sở dữ liệu (Database)**: Không tự ý truy cập database để kiểm tra trạng thái dữ liệu khi chưa giải thích và xin phép người dùng. Hãy ưu tiên đọc và phân tích mã nguồn trước để tìm câu trả lời.
* **Không tự mở trình duyệt (Browser)**: Tuyệt đối không sử dụng công cụ mở/điều khiển trình duyệt ngầm với bất kỳ mục đích nào. Nếu cần kiểm tra giao diện, kiểm thử tính năng hoặc xem log lỗi trình duyệt, hãy giải thích rõ và hướng dẫn, yêu cầu người dùng tự thực hiện trên máy của họ.
* **Không tự ý đọc code hoặc chạy công cụ vô tội vạ**: Khi người dùng chưa đưa ra yêu cầu tiếp theo hoặc đang trong quá trình trao đổi thảo luận, AI không được tự ý gọi các công cụ đọc file (`view_file`), tìm kiếm (`grep_search`), hay bất kỳ công cụ nào khác để quét code ngoài phạm vi câu hỏi hiện tại. Chỉ hành động khi nhận được yêu cầu rõ ràng từ người dùng.

## 5. Quy tắc cấu hình Bảo mật JWT (JSON Web Token)
Để đảm bảo trải nghiệm phát triển mượt mà ở môi trường cục bộ (Localhost) và tính an toàn bảo mật cao khi triển khai (Production AWS):
* **Localhost (Dev):** Khóa bí mật `jwt.secret` (đọc từ biến môi trường `JWT_SECRET`) phải là **khóa cố định** để khi nhà phát triển sửa code Java/restart server không bị văng đăng xuất (logout).
* **AWS (Production):** Khóa `jwt.secret` phải được cấu hình là `"generate-on-startup"`. Khi đó, backend sẽ tự động sinh khóa ngẫu nhiên mỗi lần khởi động lại ứng dụng, ép toàn bộ thiết bị đang đăng nhập phải đăng xuất để bảo mật.

