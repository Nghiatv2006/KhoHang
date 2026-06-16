# ĐỀ XUẤT CHỨC NĂNG NÂNG CAO — HỆ THỐNG QUẢN LÝ KHO NHIỀU CHI NHÁNH

> **Phạm vi:** Đồ án ~2 tháng. Các chức năng dưới đây được lựa chọn kỹ lưỡng để nâng cao giá trị nghiệp vụ và tính học thuật của hệ thống, nhưng vẫn đảm bảo tính khả thi và tương thích hoàn toàn với kiến trúc công nghệ hiện tại (Spring Boot Backend + JavaFX Desktop Client / Vue.js Web Client + PostgreSQL).

---

## 1. DANH SÁCH CHỨC NĂNG CƠ BẢN (NỀN TẢNG CÓ SẴN)

Trước khi đi vào các tính năng nâng cao, hệ thống đảm bảo vận hành ổn định các nghiệp vụ cốt lõi sau:
*   **Xác thực & Phân quyền:** Đăng nhập hệ thống, phân quyền người dùng theo 3 vai trò: `ADMIN` (Quản trị viên), `MANAGER` (Quản lý chi nhánh), và `STAFF` (Nhân viên kho).
*   **Quản lý Danh mục:** CRUD Chi nhánh (`branches`), Danh mục (`categories`), và Sản phẩm (`products`).
*   **Quản lý Tồn kho theo Lô:** Quản lý số lượng tồn vật lý của từng sản phẩm tại từng chi nhánh dựa trên tổ hợp Lô sản xuất gồm Ngày sản xuất (NSX) và Hạn sử dụng (HSD).
*   **Giao dịch Kho (Phiếu kho):** Lập phiếu Nhập kho (`IMPORT`), Xuất kho (`EXPORT`), Điều chuyển nội bộ (`TRANSFER`), Cân bằng tăng (`ADJUST_IN`), và Cân bằng giảm (`ADJUST_OUT`).
*   **Cảnh báo Tồn kho thấp:** Tự động phát hiện và cảnh báo các mặt hàng có số lượng tồn kho chạm hoặc dưới ngưỡng tối thiểu (`low_stock_threshold`) được thiết lập riêng cho từng chi nhánh.

---

## 2. ĐỀ XUẤT CÁC TÍNH NĂNG NÂNG CAO & CHUYÊN SÂU

Dưới đây là các tính năng được bổ sung để nâng tầm đồ án, giải quyết trực tiếp các bài toán nghiệp vụ thực tế trong quản lý chuỗi cung ứng.

### 2.1. Quy trình phê duyệt Phiếu kho (Approval Workflow)
*   **Mục tiêu:** Kiểm soát chặt chẽ hàng hóa ra vào kho, ngăn chặn nhân viên tự ý thay đổi số liệu tồn kho vật lý.
*   **Nghiệp vụ chi tiết:**
    *   **Bước 1 (Lập phiếu):** `STAFF` tạo phiếu xuất/nhập/điều chuyển. Phiếu lưu ở trạng thái **`DRAFT` (Phiếu nháp)**. Ở trạng thái này, số lượng tồn kho thực tế chưa bị thay đổi. Phiếu nháp có thể tự do chỉnh sửa hoặc xóa.
    *   **Bước 2 (Kiểm duyệt):** `MANAGER` hoặc `ADMIN` nhận được thông báo có phiếu chờ duyệt. Tiến hành đối soát thông tin.
    *   **Bước 3 (Phê duyệt):** 
        *   Nếu **Đồng ý (Duyệt)**: Trạng thái phiếu chuyển thành **`COMPLETED`**. Lúc này hệ thống mới chính thức thực hiện cộng/trừ số lượng tồn kho tương ứng và khóa vĩnh viễn phiếu kho (không cho phép chỉnh sửa/xóa).
        *   Nếu **Từ chối**: Trạng thái phiếu chuyển thành **`CANCELLED`**. Phiếu bị hủy bỏ và lưu trữ ở dạng lịch sử để đối soát sau này.

### 2.2. Quy trình điều chuyển nhân sự 3 bước (3-Step Branch Transfer Requests)
*   **Mục tiêu:** Quản lý việc luân chuyển nhân sự giữa các kho, loại bỏ rủi ro tranh chấp hoặc nhân viên phản đối quyết định điều động.
*   **Nghiệp vụ chi tiết:**
    *   **Bước 1 (Đề xuất / Xác nhận từ Nhân viên):** Nhân viên (`STAFF`) tự lập đơn xin chuyển chi nhánh hoặc nếu `MANAGER` lập đơn điều động thì hệ thống yêu cầu tài khoản của `STAFF` đó phải bấm **Xác nhận đồng ý** trên giao diện của mình. Trạng thái đơn lúc này là `STAFF_CONFIRMED`.
    *   **Bước 2 (Manager thông qua):** Quản lý chi nhánh hiện tại của nhân viên duyệt đơn nội bộ để xác nhận bàn giao công việc. Đơn chuyển trạng thái sang `MANAGER_APPROVED`.
    *   **Bước 3 (Admin phê duyệt quyết định):** `ADMIN` xem xét tình hình nhân sự toàn chuỗi, thực hiện phê duyệt cuối cùng (`APPROVED`). 
        *   Ngay khi được duyệt, hệ thống tự động chạy Transaction để **cập nhật lại cột `branch_id` của tài khoản `Users` đó sang chi nhánh mới**. Nhân viên chính thức thuộc quyền quản lý và chỉ thao tác được dữ liệu tại chi nhánh mới từ lần đăng nhập sau.

### 2.3. Điều chuyển hàng hóa 2 bước (Two-Step Inventory Transfer)
*   **Mục tiêu:** Quản lý chính xác hàng hóa đang đi đường (In-Transit) giữa các chi nhánh, tránh việc hàng hóa "biến mất" ở kho này và lập tức "xuất hiện" ở kho kia khi chưa vận chuyển tới nơi.
*   **Nghiệp vụ chi tiết:**
    *   **Bước 1 (Xuất điều chuyển):** Chi nhánh nguồn xuất hàng đi $\rightarrow$ Số lượng tồn kho tại chi nhánh nguồn lập tức bị trừ, hàng được ghi nhận vào trạng thái **`IN_TRANSIT` (Đang đi đường)**.
    *   **Bước 2 (Xác nhận nhập điều chuyển):** Khi hàng hóa vật lý tới chi nhánh đích, nhân viên tại chi nhánh đích thực hiện kiểm đếm và bấm **Xác nhận nhận hàng**. Lúc này hàng mới chính thức cộng vào tồn kho của chi nhánh đích và hoàn tất phiếu (`COMPLETED`).
    *   **Xử lý hao hụt vận chuyển:** Nếu số lượng nhận thực tế ít hơn số lượng xuất đi $\rightarrow$ Hệ thống tự động ghi nhận lượng hao hụt vận chuyển vào chi tiết phiếu và sinh một phiếu điều chỉnh giảm (`ADJUST_OUT`) tương ứng để đảm bảo tính nhất quán của số liệu sổ sách.


### 2.4. Quản lý công nợ đối tác & Thanh toán (Partner Debt & Payment Management)
*   **Mục tiêu:** Quản lý dòng tiền mua bán hàng và kiểm soát dư nợ của Khách hàng/Nhà cung cấp.
*   **Nghiệp vụ chi tiết:**
    *   **Tự động ghi nhận nợ:**
        *   Khi duyệt phiếu nhập kho (`IMPORT` + trạng thái `COMPLETED` + thanh toán `UNPAID`) $\rightarrow$ Công nợ của Nhà cung cấp (`suppliers.debt`) tự động **tăng lên** (doanh nghiệp tăng khoản nợ phải trả).
        *   Khi duyệt phiếu xuất kho (`EXPORT` + trạng thái `COMPLETED` + thanh toán `UNPAID`) $\rightarrow$ Công nợ của Khách hàng (`customers.debt`) tự động **tăng lên** (doanh nghiệp tăng khoản phải thu).
    *   **Thanh toán công nợ:**
        *   Cung cấp màn hình quản lý hóa đơn/phiếu kho chưa thanh toán (`UNPAID`). 
        *   Khi người dùng thực hiện thanh toán (trả tiền cho NCC hoặc thu tiền khách hàng) $\rightarrow$ Chuyển trạng thái phiếu từ `UNPAID` sang `PAID`, đồng thời **giảm trừ công nợ** tương ứng của Khách hàng hoặc Nhà cung cấp trong CSDL.

### 2.5. Bảo mật phân quyền dữ liệu theo chi nhánh (Data Isolation)
*   **Mục tiêu:** Bảo mật dữ liệu kinh doanh nội bộ giữa các chi nhánh khác nhau.
*   **Nghiệp vụ chi tiết:**
    *   Tài khoản `MANAGER` và `STAFF` khi đăng nhập hệ thống sẽ **bị giới hạn quyền truy cập**: chỉ được phép xem báo cáo doanh thu, lịch sử phiếu kho, tồn kho thực tế và thông tin đối tác trực thuộc đúng chi nhánh làm việc của mình (`branch_id`). Các truy vấn SQL dưới Spring Boot sẽ tự động chèn thêm điều kiện `WHERE branch_id = :userBranchId`.
    *   Tài khoản `ADMIN` có quyền xem toàn cục, lọc và so sánh số liệu giữa tất cả các chi nhánh.

### 2.6. Quản lý vị trí kệ hàng & Theo dõi sức chứa kho (Bin Location & Capacity Management)
> [!NOTE]
> **Trạng thái đề xuất:** Tính năng dự phòng - Sẽ suy nghĩ và cân nhắc tích hợp sau.

*   **Mục tiêu:** Quản lý chi tiết vị trí sắp xếp vật lý của sản phẩm và kiểm soát không gian lưu trữ thực tế tại các chi nhánh để tối ưu hóa không gian.
*   **Nghiệp vụ chi tiết:**
    *   **Quản lý vị trí kệ hàng (Bin Location Management):** Phân chia kho thành các vị trí chi tiết (ví dụ: Dãy A - Kệ 01 - Tầng 2). Sản phẩm tồn kho và các chi tiết giao dịch (nhập, xuất, điều chuyển, kiểm kê) sẽ được liên kết trực tiếp tới vị trí kệ này (thông qua bảng mới `locations` và bổ sung `location_id` vào các bảng liên quan).
    *   **Theo dõi sức chứa kho (Warehouse Capacity Tracking):**
        *   Mỗi kệ hàng được cấu hình sức chứa tối đa (`max_capacity`).
        *   Khi duyệt phiếu nhập kho hoặc nhận hàng điều chuyển, hệ thống tự động kiểm tra xem lượng hàng nhập vào kệ có vượt quá sức chứa còn trống hay không để cảnh báo hoặc ngăn chặn.
        *   Giao diện hiển thị trực quan tỷ lệ lấp đầy (Occupancy Rate) của từng kệ hoặc từng chi nhánh kho.

---

## 3. BÁO CÁO PHÂN TÍCH (BI) & XUẤT NHẬP DỮ LIỆU FILE

### 3.1. Dashboard phân tích thông minh
*   Biểu đồ JavaFX trực quan hóa:
    *   Cơ cấu giá trị tồn kho theo từng danh mục sản phẩm (Biểu đồ tròn - PieChart).
    *   Tổng giá trị hàng tồn tại mỗi chi nhánh (Biểu đồ cột - BarChart).
    *   So sánh xu hướng Nhập vs Xuất hàng tháng để kiểm soát luồng hàng (Biểu đồ đường - LineChart).
*   Thẻ thống kê nhanh: Lô hàng sắp hết hạn, sản phẩm dưới ngưỡng cảnh báo, tổng giá trị tồn kho toàn chuỗi.
*   **Báo cáo hàng chậm tiêu thụ (Deadstock):** Liệt kê các lô hàng tồn kho đã nằm im quá 60 ngày không phát sinh giao dịch xuất bán để quản lý nhanh chóng đưa ra giải pháp xả hàng.

### 3.2. Sao lưu và Phục hồi dữ liệu trực tiếp trên giao diện Web (Web-Based Backup & Restore)
*   **Chức năng Sao lưu (Export Data):**
    *   Cho phép người dùng có vai trò `ADMIN` thực hiện sao lưu trực tiếp trên giao diện Web bằng cách bấm nút **"Sao lưu dữ liệu"**. 
    *   Spring Boot backend sẽ truy vấn toàn bộ dữ liệu trong các bảng CSDL, đóng gói thông tin thành một file định dạng chuẩn (JSON hoặc file SQL) và gửi về trình duyệt dưới dạng file tải xuống để lưu trực tiếp trên máy tính cá nhân của người dùng.
*   **Chức năng Phục hồi (Import Data):**
    *   Cho phép `ADMIN` khôi phục dữ liệu bằng cách tải file đã sao lưu trước đó từ máy tính cá nhân lên thông qua giao diện Web (sử dụng Form Upload File).
    *   Backend sẽ đọc file dữ liệu này và tự động phục hồi/ghi đè vào cơ sở dữ liệu PostgreSQL. Thao tác hoàn toàn diễn ra trên giao diện Web, không cần dùng công cụ bên ngoài.
*   **Nhập dữ liệu hàng loạt từ file Excel (Bulk Import Excel):**
    *   Cho phép Admin/Manager tải lên file Excel mẫu (.xlsx) chứa danh sách sản phẩm mới hoặc nhà cung cấp mới. Hệ thống sử dụng thư viện **Apache POI** để phân tích file và thêm hàng loạt bản ghi vào database trong một transaction, giúp tiết kiệm thời gian nhập tay trên UI.
*   **Xuất báo cáo định dạng chuyên nghiệp:**
    *   Xuất báo cáo Nhập - Xuất - Tồn và Danh sách hàng tồn kho ra file Excel (.xlsx) được định dạng bảng biểu, màu sắc, font chữ chuyên nghiệp.
    *   Xuất/In hóa đơn phiếu kho ra file **PDF** để ký nhận bàn giao hàng hóa vật lý.

---

## 4. KẾ HOẠCH TRIỂN KHAI THEO TUẦN (MỨC ĐỘ DỄ ĐẾN KHÓ)

Kế hoạch 8 tuần được phân chia để đảm bảo các chức năng được xây dựng tuần tự, giảm thiểu rủi ro xung đột code và dễ kiểm thử.

*   **Tuần 1-2 (Mức Dễ - Hoàn thiện nền tảng):**
    *   Thiết lập phân quyền bảo mật dữ liệu theo chi nhánh (`Data Isolation`).
    *   Viết chức năng đổi mật khẩu cá nhân, quản lý tài khoản người dùng.
    *   Viết Unit Test (JUnit + Mockito) kiểm thử các service cơ bản.
*   **Tuần 3-4 (Mức Trung bình - Quy trình nghiệp vụ kho):**
    *   Hiện thực hóa Quy trình duyệt phiếu kho 2 bước (DRAFT $\rightarrow$ COMPLETED/CANCELLED).
    *   Phát triển quy trình kiểm kê kho (Stocktake), tính toán chênh lệch và tự động sinh phiếu điều chỉnh.
    *   Quản lý hạn sử dụng theo lô và logic gợi ý xuất hàng hết hạn trước (FEFO).
*   **Tuần 5-6 (Mức Khó - Đối tác, Công nợ & Nhân sự):**
    *   Tích hợp Nhà cung cấp & Khách hàng. Xây dựng logic tự động tính công nợ (`debt`) và thanh toán phiếu kho.
    *   Phát triển quy trình 3 bước phê duyệt điều chuyển nhân sự (`Branch Transfer Requests`).
    *   Phát triển quy trình điều chuyển kho 2 bước (`IN_TRANSIT` $\rightarrow$ Nhận hàng $\rightarrow$ Xử lý hao hụt).
*   **Tuần 7-8 (Mức Rất khó - Phân tích, Backup & REST API):**
    *   Thiết kế Dashboard biểu đồ, Báo cáo hàng chậm tiêu thụ (Deadstock).
    *   Tích hợp tính năng Sao lưu & Phục hồi dữ liệu dạng file (.json / .sql) trực tiếp qua giao diện Web.
    *   Tích hợp thư viện Apache POI để Bulk Import/Export Excel.
    *   *Mở rộng (nếu còn thời gian):* Tách backend REST API cấp phát token bảo mật bằng JWT.
