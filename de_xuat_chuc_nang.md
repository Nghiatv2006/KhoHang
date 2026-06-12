# Đề Xuất Chức Năng Nâng Cao — Hệ Thống Quản Lý Kho Nhiều Chi Nhánh

> Phạm vi: Đồ án ~2 tháng. Các chức năng dưới đây được chọn lọc để **nâng cao rõ rệt giá trị hệ thống** nhưng vẫn **khớp với kiến trúc hiện tại** (JavaFX desktop + Spring Boot + Spring Data JPA + PostgreSQL), không đập đi xây lại.

---

## 0. Cơ bản phải có

- Đăng nhập, phân quyền ADMIN / MANAGER / STAFF
- Quản lý sản phẩm, danh mục
- Tồn kho theo lô (branch + product + NSX + HSD)
- Phiếu kho: Nhập / Xuất / Điều chuyển / Cân bằng tăng-giảm
- Lịch sử giao dịch, quản lý người dùng, quản lý chi nhánh
- Cảnh báo tồn kho thấp theo ngưỡng

Các đề xuất dưới đây xây trực tiếp trên nền này.

---

## NHÓM 1 — Báo cáo & Phân tích (BI)

### 1.1. Dashboard Tổng quan có biểu đồ
- Thẻ số liệu nhanh: tổng giá trị tồn kho, số phiếu trong tháng, số mặt hàng dưới ngưỡng, số lô sắp hết hạn.
- Biểu đồ (JavaFX `BarChart` / `PieChart` / `LineChart`):
  - Cơ cấu tồn kho theo danh mục (Pie).
  - Giá trị tồn theo từng chi nhánh (Bar).
  - Xu hướng Nhập vs Xuất theo 12 tháng (Line).
- Lọc theo chi nhánh / khoảng thời gian.

### 1.2. Báo cáo Tồn kho & Định giá - tính sau
- Báo cáo tồn kho tại một thời điểm (tổng số lượng × đơn giá = giá trị tồn).
- Tính **giá vốn xuất kho** theo phương pháp FIFO hoặc Bình quân gia quyền.
- Báo cáo Nhập–Xuất–Tồn (mẫu kế toán kho chuẩn): tồn đầu kỳ, nhập trong kỳ, xuất trong kỳ, tồn cuối kỳ.

### 1.3. Xuất báo cáo ra file
- Xuất CSV (thuần Java) cho mọi bảng.
- Xuất Excel (Apache POI) có định dạng, tiêu đề, tổng cộng.
- Xuất / In phiếu kho ra PDF (iText hoặc JasperReports) — dùng khi giao nhận hàng.

---

## NHÓM 2 — Nghiệp vụ kho nâng cao

### 2.1. Quy trình duyệt phiếu (Approval Workflow)
> Tận dụng cột `status` (DRAFT / COMPLETED / CANCELLED) đã có sẵn trong schema nhưng chưa khai thác.
- STAFF lập phiếu ở trạng thái **DRAFT** (chưa trừ kho).
- MANAGER / ADMIN xem và **Duyệt** → mới thực sự cập nhật tồn kho (COMPLETED), hoặc **Từ chối** (CANCELLED).
- Phiếu DRAFT có thể sửa/xóa; phiếu COMPLETED thì khóa.

### 2.2. Quản lý Nhà cung cấp & Khách hàng ( cân nhắc khách hàng )
- Thêm 2 thực thể `Supplier` và `Customer`.
- Phiếu nhập gắn nhà cung cấp, phiếu xuất gắn khách hàng.
- Lịch sử giao dịch theo từng đối tác, công nợ cơ bản.

### 2.3. Cảnh báo & Quản lý Hạn sử dụng (FEFO)
- Danh sách lô hàng **sắp hết hạn** (HSD trong vòng N ngày) — bảng riêng + tô màu.
- Khi xuất hàng có HSD: tự gợi ý **xuất lô hết hạn sớm nhất trước** (First-Expired-First-Out).
- Cảnh báo hàng **đã hết hạn** còn tồn trong kho.

### 2.4. Kiểm kê kho (Stocktake) 
- Tạo phiên kiểm kê: hệ thống liệt kê tồn theo sổ sách, người dùng nhập số đếm thực tế.
- Tự tính chênh lệch, sinh phiếu cân bằng (ADJUST_IN/OUT) tương ứng.
- Lưu biên bản kiểm kê ( chưa có trong project , hiện tại có mỗi ghi chú ).

### 2.5. Đề xuất nhập hàng tự động (Reorder)
- Khi tồn ≤ ngưỡng, hệ thống tổng hợp danh sách hàng cần nhập theo chi nhánh.
- Gợi ý số lượng đặt dựa trên tốc độ bán trung bình (từ lịch sử xuất).

---

## NHÓM 3 — Vận hành, Bảo mật & Trải nghiệm

### 3.1. Nhật ký hoạt động (Audit Log)
- Ghi lại mọi thao tác quan trọng: ai, làm gì, lúc nào, trên bản ghi nào.
- Bảng `audit_logs`, màn hình tra cứu cho ADMIN.
- Phục vụ truy vết và đúng tinh thần "Worklog".

### 3.2. Quản lý tài khoản cá nhân
- Tự đổi mật khẩu.
- Quên mật khẩu → ADMIN cấp lại.
- Quy tắc mật khẩu mạnh.

### 3.3. Đa ngôn ngữ (i18n)
- Việt / Anh qua `ResourceBundle`, chuyển ngôn ngữ ngay trong app.

### 3.4. Quét mã vạch / QR ( ưu tiên thấp )
- Dùng webcam (thư viện ZXing) quét mã sản phẩm để chọn nhanh khi lập phiếu, thay vì gõ tay.
- Sinh mã QR cho mỗi sản phẩm để in nhãn.

### 3.5. Sao lưu & Phục hồi dữ liệu
- Chức năng backup database ra file (pg_dump) và phục hồi, lên lịch định kỳ.

---

## NHÓM 4 — Kỹ thuật & Chất lượng (điểm cộng học thuật)

### 4.1. Kiểm thử tự động
- Unit Test (JUnit + Mockito) cho tầng Service, đặc biệt `ReceiptService` (logic trừ/cộng kho, rollback).
- Test các ràng buộc nghiệp vụ: không xuất quá tồn, điều chuyển 2 chiều đúng số lượng.

### 4.2. Tài liệu hóa
- Sơ đồ Use Case, ERD, Activity Diagram (Sequence cho luồng lập phiếu).
- README + hướng dẫn cài đặt + tài khoản mẫu (đã có).

---

## Lộ trình đề xuất theo tuần (8 tuần)

| Tuần | Nội dung |
|------|----------|
| 1 | Hoàn thiện CRUD còn thiếu + Unit Test nền tảng |
| 2 | Quy trình duyệt phiếu (DRAFT → COMPLETED/CANCELLED) |
| 3 | Nhà cung cấp / Khách hàng + gắn vào phiếu |
| 4 | Quản lý HSD nâng cao (FEFO) + cảnh báo hết hạn |
| 5 | Kiểm kê kho + đề xuất nhập hàng tự động |
| 6 | Dashboard biểu đồ + báo cáo Nhập-Xuất-Tồn |
| 7 | Xuất Excel/PDF + Audit Log + đổi mật khẩu |
| 8 | Quét mã vạch (nếu kịp) + kiểm thử tổng thể + viết báo cáo |

---

## Top ưu tiên nếu phải chọn

1. **Quy trình duyệt phiếu** — tận dụng schema có sẵn, thể hiện tư duy nghiệp vụ rõ nhất.
2. **Dashboard biểu đồ + Báo cáo Nhập-Xuất-Tồn + Xuất file** — nổi bật khi demo, đúng yêu cầu báo cáo.
3. **Quản lý HSD nâng cao (FEFO) + Kiểm kê** — khai thác đúng thiết kế tồn kho theo lô.
4. **Nhà cung cấp / Khách hàng** — mở rộng nghiệp vụ thực tế.
5. **Audit Log + Đổi mật khẩu** — hoàn thiện tính chuyên nghiệp.

---

## PHỤ LỤC — Kiến trúc Triển khai: Tách Backend REST API

> Phương án triển khai khuyến nghị khi đóng gói thành `.exe` và cần nhiều máy/chi nhánh ở xa nhau cùng dùng chung dữ liệu một cách an toàn.

### Mô hình

```
[App JavaFX .exe]  --HTTP/HTTPS-->  [Spring Boot REST API]  --JDBC-->  [PostgreSQL]
   (client, nhiều máy)                  (server, 1 nơi)                  (1 nơi)
```

- App desktop **không** truy vấn SQL trực tiếp nữa, mà gọi **API qua HTTP**.
- Toàn bộ logic nghiệp vụ và kết nối DB nằm tập trung ở server.
- Dữ liệu vẫn là một kho trung tâm duy nhất → mọi máy luôn nhất quán tức thời (không cần "đồng bộ").

### Vì sao chọn cách này

- **An toàn:** Client không cầm mật khẩu DB, DB không phơi ra Internet. Client chỉ biết địa chỉ API.
- **Tập trung nghiệp vụ:** Validate, trừ/cộng kho, kiểm soát quyền đều ở server → khó gian lận, dễ bảo trì.
- **Mở rộng:** Sau này thêm web/mobile chỉ việc gọi cùng bộ API.
- **Bảo mật nâng cao:** Dễ gắn xác thực bằng token (JWT), ghi log, giới hạn truy cập theo vai trò ở tầng API.

### Các bước chuyển đổi (từ kiến trúc hiện tại)

1. **Thêm Spring Web** vào server: `spring-boot-starter-web`.
2. **Viết REST Controller** bọc các Service đã có:
   - `POST /api/auth/login` → trả về token + thông tin user.
   - `GET /api/products`, `POST /api/products`, ... (CRUD sản phẩm).
   - `GET /api/inventories?branchId=...` (tồn kho).
   - `POST /api/receipts` (lập phiếu — gọi `ReceiptService.createReceipt`).
   - `GET /api/receipts`, `GET /api/branches`, `GET /api/users`, ...
3. **Bảo mật API:** thêm Spring Security + JWT; mỗi request đính kèm token ở header `Authorization`.
4. **Sửa client JavaFX:** thay các lời gọi Service trực tiếp bằng gọi HTTP (dùng `java.net.http.HttpClient` hoặc thư viện như Retrofit/OkHttp), parse JSON (Jackson).
5. **Tài liệu hóa API:** thêm Swagger UI (springdoc-openapi) để test và mô tả API — đúng công cụ syllabus gợi ý (Postman/Swagger).

### Đóng gói & cấu hình

- **Server:** đóng gói JAR, chạy trên 1 máy/VPS/cloud; là nơi duy nhất giữ mật khẩu DB.
- **Client (.exe):** dùng `jpackage` (JDK 17) tạo `.exe`/`.msi` kèm JRE; chỉ cần cấu hình **địa chỉ API server** (đọc từ file config ngoài, không hardcode).
- Nên bật **HTTPS** cho API khi chạy thật để mã hóa dữ liệu truyền đi.

### So với 2 cách đơn giản hơn

| Tiêu chí | Cách 1: DB chung LAN | Cách 2: DB cloud | Cách 3: REST API |
|----------|:---:|:---:|:---:|
| Công sức | Thấp | Thấp | Cao |
| Phạm vi | Chỉ LAN | Toàn Internet | Toàn Internet |
| An toàn | Thấp | Thấp–TB | Cao |
| Client cầm mật khẩu DB | Có | Có | Không |
| Điểm cộng kiến trúc | Ít | Ít | Nhiều |

→ **Khuyến nghị:** Dùng Cách 2 để demo nhanh; nâng lên **Cách 3** nếu muốn đúng chuẩn production và ăn điểm kiến trúc cho đồ án.

---

## PHỤ LỤC — So sánh chi tiết: Cách 2 (DB Cloud) vs Cách 3 (REST API)

### Bản chất

**Cách 2 — DB Cloud (client nối thẳng DB)**
```
[App .exe] ──JDBC qua Internet──> [PostgreSQL trên cloud]
```
App JavaFX cầm tài khoản DB và bắn SQL thẳng tới database trên cloud.

**Cách 3 — REST API (có server trung gian)**
```
[App .exe] ──HTTP──> [Spring Boot API] ──JDBC──> [PostgreSQL]
```
App gọi API qua HTTP; server là nơi duy nhất nối DB và xử lý nghiệp vụ.

### Bảng so sánh

| Tiêu chí | Cách 2 (DB Cloud) | Cách 3 (REST API) |
|---|---|---|
| Công sức | Rất thấp — đổi 1 dòng `DB_URL` | Cao — viết Controller, đổi client sang gọi HTTP, thêm JWT |
| Thời gian | Vài phút | 1–2 tuần |
| Client cầm mật khẩu DB | Có (rủi ro) | Không |
| DB phơi ra Internet | Có (rủi ro) | Không — chỉ API lộ ra |
| An toàn dữ liệu | Thấp–Trung bình | Cao |
| Logic nghiệp vụ | Nằm ở client | Tập trung ở server |
| Kiểm soát quyền | Client tự check (dễ lách) | Server ép buộc (chặt) |
| Mở rộng web/mobile | Không tiện | Sẵn sàng, dùng chung API |
| Điểm cộng kiến trúc | Ít | Nhiều |
| Rủi ro phát sinh bug | Thấp | Cao (serialize JSON, lazy-load, token) |

### Cốt lõi để quyết định

- Về mục tiêu "nhiều máy dùng chung dữ liệu": **cả 2 đạt như nhau**, dữ liệu tập trung 1 chỗ, mọi máy thấy cùng số liệu tức thì.
- Khác biệt thật nằm ở **bảo mật và kiến trúc**:
  - Cách 2 có lỗ hổng: mỗi `.exe` chứa mật khẩu DB và DB mở cổng ra Internet → lộ là chiếm toàn bộ database.
  - Cách 3 bịt lỗ hổng đó: client chỉ biết địa chỉ API, thao tác qua token, server kiểm soát hết.

### Khuyến nghị
- Ưu tiên hoàn thành nhanh + nhiều tính năng nghiệp vụ → **Cách 2** (nêu rõ hạn chế bảo mật trong báo cáo).
- Muốn ăn điểm kiến trúc + còn thời gian + dự định làm web/mobile → **Cách 3**.
- App desktop nội bộ thuần túy: Cách 2 là đủ. Cách 3 chỉ thực sự đáng khi có kế hoạch mở rộng nền tảng.

---

## PHỤ LỤC — Lộ trình triển khai tính năng (sắp xếp từ DỄ đến KHÓ)

> Thứ tự gợi ý làm tuần tự. Mỗi mức nên hoàn thành và kiểm thử xong trước khi sang mức sau.

### MỨC 1 — Dễ (hoàn thiện nền tảng, ít rủi ro)
1. **Đổi mật khẩu cá nhân** — chỉ thêm form + 1 method ở `UserService`. Làm quen luồng mà không đụng nghiệp vụ phức tạp.
2. **Xuất CSV** — xuất bảng tồn kho / lịch sử ra file. Thuần Java, không cần thư viện.
3. **Cảnh báo & danh sách hàng sắp/đã hết hạn** — chỉ là query lọc theo HSD trên dữ liệu sẵn có + tô màu.
4. **Unit Test cho Service** — đặc biệt `ReceiptService`. Vừa dễ vừa là yêu cầu bắt buộc của syllabus.

→ *Mục tiêu: làm chủ codebase, củng cố phần đã có, lấy điểm chắc chắn.*

### MỨC 2 — Trung bình (thêm nghiệp vụ, dùng schema sẵn có)
5. **Quy trình duyệt phiếu (DRAFT → COMPLETED/CANCELLED)** — tận dụng cột `status` đã có. Sửa `ReceiptService` để DRAFT chưa trừ kho, thêm nút Duyệt/Từ chối.
6. **Kiểm kê kho (Stocktake)** — liệt kê tồn sổ sách, nhập đếm thực tế, tự sinh phiếu cân bằng.
7. **Dashboard biểu đồ** — `BarChart`/`PieChart`/`LineChart` từ dữ liệu tồn kho và lịch sử.
8. **Báo cáo Nhập–Xuất–Tồn + xuất Excel (Apache POI)** — báo cáo kế toán kho chuẩn.

→ *Mục tiêu: tăng giá trị nghiệp vụ và tính trực quan khi demo.*

### MỨC 3 — Khó (mở rộng mô hình dữ liệu & thuật toán)
9. **Quản lý Nhà cung cấp & Khách hàng** — thêm 2 entity mới, gắn vào phiếu, sửa schema + nhiều màn hình.
10. **Định giá xuất kho FIFO / Bình quân gia quyền** — logic tính giá vốn, cần xử lý theo lô cẩn thận.
11. **Xuất / In phiếu ra PDF (JasperReports/iText)** — thiết kế mẫu in, tích hợp thư viện.
12. **Audit Log** — chặn ngang các thao tác để ghi log (AOP hoặc thủ công), thêm màn hình tra cứu.

→ *Mục tiêu: chiều sâu nghiệp vụ và kỹ thuật.*

### MỨC 4 — Rất khó (thay đổi kiến trúc / phần cứng)
13. **Quét mã vạch / QR bằng webcam (ZXing)** — tích hợp camera với JavaFX, xử lý luồng ảnh.
14. **Đa ngôn ngữ (i18n)** — refactor toàn bộ chuỗi text ra `ResourceBundle`.
15. **Tách Backend REST API (Cách 3)** — viết tầng API + JWT, đổi toàn bộ client sang gọi HTTP. **Làm cuối cùng** vì ảnh hưởng toàn hệ thống.

→ *Mục tiêu: nâng tầm kiến trúc — chỉ làm khi các mức dưới đã ổn định.*

### Nguyên tắc thứ tự
- Làm **MỨC 1 trước** để vững nền và lấy điểm chắc.
- **Tách REST API (#15) để sau cùng**: nếu làm sớm rồi mới thêm tính năng, mỗi tính năng mới đều phải code 2 lần (cả API lẫn client). Làm xong nghiệp vụ rồi mới tách sẽ gọn hơn nhiều.
- Sau mỗi mức nên commit + kiểm thử trước khi đi tiếp.
