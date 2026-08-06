# WareHub — Hệ thống Quản lý Kho hàng Đa Chi nhánh

WareHub là hệ thống quản lý kho hàng tập trung và hỗ trợ điều chuyển phân phối hàng hóa dành cho doanh nghiệp vận hành theo mô hình **Chi nhánh Tổng (Kho trung tâm)** và nhiều **Chi nhánh Con (Kho vệ tinh)**.

---

## 📋 Mục lục
- [Giới thiệu Dự án](#-giới-thiệu-dự-án)
- [Yêu cầu Môi trường (Prerequisites)](#-yêu-cầu-môi-trường-prerequisites)
- [Công nghệ & Thư viện sử dụng](#-công-nghệ--thư-viện-sử-dụng)
- [Cấu trúc Thư mục Dự án](#-cấu-trúc-thư-mục-dự-án)
- [Cấu hình Biến môi trường (.env)](#-cấu-hình-biến-môi-trường-env)
- [Hướng dẫn Cài đặt & Khởi chạy](#-hướng-dẫn-cài-đặt--khởi-chạy)
- [Dịch vụ bên thứ ba (Third-party Services)](#-dịch-vụ-bên-thứ-ba-third-party-services)

---

## 🌟 Giới thiệu Dự án

**WareHub** giải quyết bài toán quản lý kho đa chi nhánh cho doanh nghiệp với các đặc điểm cốt lõi:
- **Phân phối Đa chi nhánh:** Quản lý kho tổng (nhập từ nhà cung cấp, chuyển hàng xuống chi nhánh con) và các kho con (nhận hàng, xuất bán lẻ).
- **Kiểm soát Tồn kho chi tiết:** Quản lý và kiểm soát chính xác số lượng tồn kho của từng mặt hàng (thiết bị điện tử, iPhone, phụ kiện...) tại từng chi nhánh tổng và chi nhánh con.
- **Quy trình Phê duyệt Phân quyền:** Phân quyền minh bạch giữa Quản lý (Manager) và Nhân viên (Staff).
- **Kiểm kê & Tự động cân bằng:** So sánh thực tế kiểm đếm với sổ sách và tự động sinh phiếu cân bằng kho (`ADJUST_IN` / `ADJUST_OUT`).
- **Quản lý Công nợ & Báo cáo:** Theo dõi công nợ khách hàng (AR) và hệ thống biểu đồ trực quan hóa dữ liệu kinh doanh.

---

## 💻 Yêu cầu Môi trường (Prerequisites)

Trước khi tiến hành cài đặt và khởi chạy dự án, máy tính của bạn cần được cài đặt sẵn các môi trường sau:

| Công cụ / Môi trường | Phiên bản khuyến nghị | Mục đích sử dụng |
| :--- | :--- | :--- |
| **Node.js** | `>= 18.0.0` | Môi trường thực thi JavaScript cho Frontend |
| **npm** | `>= 9.0.0` | Trình quản lý gói thư viện Node.js |
| **Java Development Kit (JDK)** | `17` (OpenJDK / Eclipse Temurin) | Môi trường biên dịch và chạy Spring Boot Backend |
| **PostgreSQL** | `>= 14.0` | Hệ quản trị cơ sở dữ liệu quan hệ chính |
| **Git** | Bản mới nhất | Quản lý mã nguồn dự án |

---

## 🛠️ Công nghệ & Thư viện sử dụng

### 1. Backend (Spring Boot API)
- **Java Version:** 17
- **Framework:** Spring Boot `3.5.14`
- **Spring Starters & Modules:**
  - `spring-boot-starter-web`: Xây dựng RESTful Web API.
  - `spring-boot-starter-data-jpa`: Tương tác cơ sở dữ liệu qua ORM Hibernate / JPA.
  - `spring-boot-starter-security`: Xác thực và phân quyền người dùng (RBAC: ADMIN, MANAGER, STAFF).
  - `spring-boot-starter-mail`: Dịch vụ gửi email thông báo tự động.
- **Database Driver:** PostgreSQL JDBC Driver (`org.postgresql:postgresql`)
- **Bảo mật JWT:** JJWT `0.11.5` (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- **Xử lý Tài liệu & Báo cáo:**
  - **Apache POI** (`5.2.3` - `poi`, `poi-ooxml`): Đọc/ghi dữ liệu và xuất báo cáo file Excel (`.xlsx`).
  - **OpenHTMLtoPDF** (`1.0.10` - `openhtmltopdf-core`, `openhtmltopdf-pdfbox`): Render template HTML thành tệp PDF.

### 2. Frontend (VueJS Single Page Application)
- **Framework & Core:** Vue 3 (`3.5.34` - Composition API), TypeScript (`6.0.2`)
- **Build Tool:** Vite (`8.0.12`), `vue-tsc`
- **Routing:** Vue Router (`4.6.4`)
- **Styling & Design:** Tailwind CSS (`4.3.1`), PostCSS (`8.5.15`), Autoprefixer
- **HTTP Client:** Axios (`1.17.0`)
- **Biểu đồ & Trực quan hóa (Charts):** Apache ECharts (`6.1.0`), `vue-echarts` (`8.0.1`)
- **Hiệu ứng & 3D:** Anime.js (`4.5.0`), Three.js (`0.184.0`)
- **Xuất PDF & Hình ảnh ở Client:** `jspdf` (`4.2.1`), `html2canvas` (`1.4.1`)

---

## 📁 Cấu trúc Thư mục Dự án

```text
WareHub/
├── build.gradle              # Cấu hình Gradle & Dependencies Backend
├── settings.gradle           # Cấu hình tên project Gradle
├── Dockerfile / docker-compose.yml # Thẻ đóng gói Docker container
├── full_schema.sql           # Schema SQL khởi tạo bảng CSDL PostgreSQL
├── seed_data.sql             # Dữ liệu khởi tạo mẫu
├── init_db.bat / init_db.ps1 # Script tự động tạo database & nạp SQL
├── run_warehub.bat           # Script khởi chạy đồng thời Backend + Frontend
├── run_backend.bat           # Script khởi chạy Spring Boot (Auto-detect JDK 17)
├── run_frontend.bat          # Script quản lý & chạy Vite Frontend
│
├── src/                      # MÃ NGUỒN BACKEND (Spring Boot)
│   └── main/java/com/example/Hehe/
│       ├── config/           # Cấu hình Security, CORS, Mail, WebMvc...
│       ├── controller/       # REST API Controllers (Auth, Product, Receipt...)
│       ├── dto/              # Data Transfer Objects (Request/Response)
│       ├── exception/        # Xử lý ngoại lệ toàn cục (Global Exception Handler)
│       ├── model/            # JPA Entities (Branch, Product, Inventory, Receipt...)
│       ├── repository/       # JPA Repositories truy vấn CSDL
│       ├── security/         # JWT Token Provider, Auth Filters, Security Config
│       ├── service/          # Logic nghiệp vụ hệ thống
│       └── util/             # Utility classes (PDF/Excel Generator...)
│
├── VueJs/                    # MÃ NGUỒN FRONTEND (Vue 3 SPA)
│   ├── package.json          # Danh sách thư viện & Scripts NPM
│   ├── vite.config.ts        # Cấu hình Vite Build Tool
│   ├── tailwind.config.js    # Cấu hình giao diện Tailwind CSS
│   └── src/
│       ├── api.ts            # Cấu hình Axios & Endpoints
│       ├── assets/           # CSS, logo, hình ảnh tĩnh
│       ├── components/       # Các component UI tái sử dụng
│       ├── layouts/          # Giao diện khung (MainLayout, AuthLayout)
│       ├── router/           # Định tuyến ứng dụng (Vue Router)
│       ├── views/            # Các trang giao diện chính
│       └── utils/            # Helper functions
│
├── docs/ / UseCase/          # Tài liệu mô tả Use Case & Nghiệp vụ
└── diagrams/                 # Sơ đồ ERD & Thiết kế hệ thống
```

---

## ⚙️ Cấu hình Biến môi trường (`.env`)

Trước khi khởi chạy dự án, sao chép tệp `.env.example` thành `.env` (hoặc tạo tệp `.env` mới tại thư mục gốc) và khai báo các biến môi trường sau:

```env
# 1. Spring Boot Server Port
SERVER_PORT=8080

# 2. Cấu hình Cơ sở dữ liệu PostgreSQL
DB_HOST=localhost
DB_PORT=5432
DB_NAME=warehouse_db
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# 3. Cấu hình Bảo mật JWT
JWT_SECRET=your_jwt_secret_key_min_32_chars
JWT_EXPIRATION=86400000

# 4. Tài khoản Admin mặc định (dùng để tham chiếu khi chạy seed_data)
DEFAULT_ADMIN_USERNAME=your_admin_username
DEFAULT_ADMIN_PASSWORD=your_admin_password

# 5. Cấu hình Gửi Email (SMTP Gmail)
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_email_app_password

# 6. Backup HMAC Secret
BACKUP_SECRET=your_backup_hmac_secret_key

# 7. AbstractAPI Email Verification Key
ABSTRACT_API_KEY=your_abstract_api_key
```

---

## 🚀 Hướng dẫn Cài đặt & Khởi chạy

### Cách 1: Khởi chạy bằng IntelliJ IDEA & Terminal (Khuyên dùng cho Developer)

1. **Khởi tạo Cơ sở dữ liệu:**
   * Click đúp chuột vào tệp `init_db.bat` ở thư mục gốc. Script sẽ tự động kích hoạt dịch vụ PostgreSQL (nếu bị tắt), tạo database `warehouse_db` và nạp toàn bộ cấu trúc bảng cùng dữ liệu mẫu từ `full_schema.sql` và `seed_data.sql`.

2. **Khởi chạy Backend (Spring Boot):**
   * Mở thư mục dự án WareHub trong **IntelliJ IDEA**.
   * IntelliJ sẽ tự động nhận diện dự án Gradle và tải toàn bộ các phụ thuộc (dependencies).
   * Để ứng dụng nạp biến môi trường từ `.env`: cài đặt plugin **EnvFile** trong IntelliJ (hoặc cấu hình đường dẫn tệp `.env` trong phần *Environment Variables* của **Run/Debug Configurations**).
   * Mở tệp `src/main/java/com/example/Hehe/HeheApplication.java` và nhấn nút **Run** (hoặc tổ hợp phím `Shift + F10`).
   * Backend API sẽ lắng nghe tại cổng `http://localhost:8080`.

3. **Khởi chạy Frontend (Vue 3):**
   * Mở cửa sổ **Terminal** tích hợp trong IntelliJ IDEA (hoặc CMD/PowerShell), di chuyển vào thư mục `VueJs`:
     ```powershell
     cd VueJs
     npm install     # Tải phụ thuộc (Chỉ thực hiện ở lần chạy đầu tiên)
     npm run dev     # Khởi chạy Vite Dev Server
     ```
   * Mở trình duyệt và truy cập `http://localhost:3000`.

---

### Cách 2: Khởi chạy nhanh bằng Bộ Script `.bat` (Windows)

1. **Khởi tạo Cơ sở dữ liệu:** Click đúp file `init_db.bat`.
2. **Tải tài nguyên Frontend (Chỉ lần đầu):** Click đúp file `run_frontend.bat` -> Nhập phím `1` (`npm install`).
3. **Khởi chạy đồng thời cả hệ thống:** Click đúp file `run_warehub.bat` để tự động bật Backend và Frontend trong 2 cửa sổ console riêng biệt.

---

## 🌐 Dịch vụ bên thứ ba (Third-party Services)

- **[Abstract API - Email Reputation API](https://www.abstractapi.com/api/email-verification-validation-api):** Được tích hợp tại [UserController.java](file:///d:/IT/Hehe/src/main/java/com/example/Hehe/controller/UserController.java) để kiểm tra khả năng gửi/nhận thư (`email_deliverability`) của email người dùng.
- **Gmail SMTP Server:** Dịch vụ gửi email tự động thông qua Spring Mail.

Mọi thắc mắc có thể liên hệ đến email : trannghia2006nd@gmail.com
