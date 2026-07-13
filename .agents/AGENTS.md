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

## 6. Quy tắc khi thực hiện Merge (Gộp nhánh)
* **Luôn lập đề xuất/kế hoạch trước khi Merge:** Khi nhận được yêu cầu gộp nhánh (merge), đặc biệt là các nhánh có nguy cơ gây xung đột (conflict) hoặc thay đổi cấu trúc mã nguồn, AI phải liệt kê các file bị ảnh hưởng, phân tích các xung đột tiềm tàng và đề xuất phương án giải quyết cụ thể trước khi chạy lệnh merge chính thức.
* **Đợi xác nhận từ người dùng:** Tuyệt đối không tự ý thực hiện commit merge, sửa file xung đột hoặc push code lên các nhánh chung mà chưa được người dùng duyệt qua và đồng ý với phương án đề xuất.
* **Bắt buộc kiểm tra biên dịch (Build) trước khi staging hoặc commit:** Sau khi sửa code hoặc giải quyết xung đột, **trước khi** chạy lệnh `git add` hoặc `git commit`, AI bắt buộc phải chạy lệnh build thử cả Backend và Frontend (ví dụ: `./gradlew build` hoặc `npm run build`) để đề phòng lỗi cú pháp/biên dịch làm hỏng nhánh chung khi push lên.
* **Không tự ý sáng tạo hoặc viết lại logic mới khi giải quyết xung đột (Conflict Resolution):** Khi giải quyết conflict, chỉ được phép chọn giữ nguyên code của nhánh nguồn (HEAD), nhánh gộp (incoming branch), hoặc kết hợp chính xác code của cả hai bên để đảm bảo tính năng hoạt động đúng thiết kế. Tuyệt đối không tự ý thay đổi cơ chế hoạt động, đổi tên biến/tham số không cần thiết, hoặc viết lại code/class mới hoàn toàn lệch khỏi ý đồ thiết kế ban đầu của các nhà phát triển.


# Quy tắc thiết kế Giao diện (UI/UX Premium Standards)

Để đảm bảo giao diện luôn đạt tiêu chuẩn thẩm mỹ cao cấp (Premium) của dự án và không bị sáo rỗng:

## 1. Màu sắc và Ánh sáng
* **Cấm tuyệt đối AI Purple/Neon**: Không sử dụng dải chuyển sắc tím rực rỡ, không dùng bóng viền phát sáng (neon/outer glow shadows) hoặc các hiệu ứng chuyển động dải màu neon rườm rà.
* **Đơn sắc cao cấp**: Chỉ dùng tối đa 1 màu nhấn (Accent) bão hòa thấp (<80%). Sử dụng nền trung tính (Zinc/Slate) và không dùng màu đen thuần (#000000).

## 2. Tiêu đề và Typography
* **Không chèn biểu tượng tiêu đề cột**: Tiêu đề các cột bảng dữ liệu phải tinh giản, không chèn các icon nhựa (`<i>`) bên cạnh chữ.
* **Định dạng chữ**: Tiêu đề bảng sử dụng chữ viết hoa nhỏ, giãn chữ (`tracking-wider text-[11px] font-semibold text-slate-400 dark:text-slate-500`).
* **Cấm phông chữ Inter**: Không dùng phông chữ `Inter` cho các văn bản hiển thị cao cấp.

## 3. Bố cục và Cấu trúc Bảng
* **Bảng dữ liệu mật độ cao (High-density)**: Không lạm dụng việc bọc các dòng thành thẻ card bo tròn riêng lẻ dính bóng đổ. Thay vào đó, hãy dùng đường chia dòng ngang mảnh (`border-b border-slate-100 dark:border-slate-800/40`) để giao diện được thông thoáng và chuyên nghiệp.
* **Cấm tuyệt đối Emoji**: Không sử dụng biểu tượng cảm xúc (emoji) ở bất kỳ phần nào trên giao diện ứng dụng.

# Quy tắc khi bổ sung tính năng mới (Tránh phá hoại code cũ)

## 1. Không tái sử dụng hoặc ghi đè (override) logic có sẵn sai mục đích
* **Tôn trọng luồng xử lý gốc**: Khi thêm tính năng mới, tuyệt đối không được ghi đè, xóa, hoặc tận dụng (hijack) các khối code đang hoạt động bình thường cho mục đích khác ngoài thiết kế ban đầu.
* **Tạo luồng xử lý độc lập**: Phải tự xây dựng trạng thái (state), biến cục bộ, và khối `if/else` biệt lập cho tính năng mới. Giữ nguyên 100% logic cũ.

## 2. Cẩn trọng tuyệt đối với từ ngữ hiển thị (UI/UX)
* **Không dùng từ ngữ gây hoang mang**: Từ ngữ hiển thị phải phản ánh đúng bản chất và phạm vi thực tế của hành động (tạm thời hay vĩnh viễn, cá nhân hay hệ thống). Tránh dùng từ ngữ nặng nề hoặc có nghĩa vĩnh viễn cho các trạng thái tạm thời.
* **Xác nhận nghiệp vụ trước khi viết thông báo**: Luôn cân nhắc ảnh hưởng của từ ngữ đến người dùng cuối và bộ phận Hỗ trợ (Support) trước khi quyết định nội dung thông báo.

# Quy tắc tối ưu hóa thao tác hệ thống (Hạn chế Git và Quét file)

Để đảm bảo hiệu năng và tránh gây khó chịu cho người dùng, AI phải tuân thủ nghiêm ngặt ranh giới thao tác sau:

1. **Hạn chế tối đa MỌI câu lệnh Git:**
   - Tuyệt đối không được tự ý/liên tục gọi BẤT KỲ lệnh `git` nào (bao gồm cả `git status`, `git diff`, `git log`, `git show`, `git branch`...).
   - Chỉ sử dụng Git ở mức độ **ít nhất có thể** hoặc khi có yêu cầu cụ thể của người dùng (như gộp nhánh, khôi phục code).

2. **Hạn chế tối đa việc đọc và tìm kiếm file diện rộng:**
   - Hạn chế tối đa việc sử dụng công cụ `grep_search` và `view_file` một cách vô tội vạ.
   - Khi cần phân tích, chỉ được phép khoanh vùng và đọc **1 đến 2 file thực sự liên quan trực tiếp** đến vấn đề đang thảo luận. Không mở rộng phạm vi đọc file sang các file xung quanh nếu không có lý do cực kỳ chính đáng.
   - Ưu tiên suy luận logic từ các file chính thay vì lạm dụng công cụ tìm kiếm càn quét toàn bộ thư mục.
