# QUY TRÌNH NGHIỆP VỤ KIỂM KÊ KHO

Tài liệu này mô tả chính xác 100% quy trình vận hành và phân quyền trong module Kiểm kê kho, bao gồm các kịch bản bắt lỗi, trách nhiệm cá nhân và xử lý chênh lệch thực tế.

## BƯỚC 1: LÍNH LÀM (Trạng thái DRAFT)

**1. Khởi tạo & Đếm hàng:**
- **Nhân viên hệ thống (Kế toán)**: Là người duy nhất có quyền tạo phiếu kiểm kê trên phần mềm. In "Phiếu đếm mù" (không có số lượng dự kiến) đưa cho Nhân viên kho.
- **Nhân viên kho (Thủ kho)**: Cầm phiếu đi đếm vật lý thực tế, ghi số lượng vào giấy và **KÝ TÊN** xác nhận chịu trách nhiệm.

**2. Nhập số & Tự kiểm tra:**
- **Nhân viên hệ thống**: Nhập số lượng từ giấy vào phần mềm.
- **Tự dọn rác**: Nếu phần mềm báo LỆCH (số nhập khác số tồn hệ thống), Nhân viên hệ thống và Nhân viên kho **BẮT BUỘC** phải tự giác đối chiếu, xuống kho bới tìm lại. Ai đếm sai thì tự sửa, ai nhập sai thì tự sửa. Tuyệt đối không nộp lên khi chưa kiểm tra kỹ.

**3. Hoàn tất & Nộp:**
- Chỉ khi nào chắc chắn là mất hàng thật, hoặc tìm gãy lưng không ra, Kế toán mới bấm **"Hoàn tất & Nộp"**.
    - *Khớp 100%*: Phần mềm tự chốt sổ xanh lè (**COMPLETED**). Không cần Quản lý can thiệp.
    - *Bị Lệch*: Phiếu bị treo lên **PENDING_APPROVAL** (Báo động đỏ đẩy lên bàn Sếp).

---

## BƯỚC 2: SẾP XỬ LÝ (Trạng thái PENDING_APPROVAL)

Sếp nhận được phiếu báo lệch. **Sếp tuyệt đối không đi đếm thay cho lính!** Sếp chỉ ngồi phòng soi Camera hoặc đối chất hồ sơ, rồi tung ra 1 trong 3 quyền lực sinh sát:

### Quyền Lực 2: Tát gáo nước lạnh (Từ chối / Yêu cầu đếm lại)
* **Nguyên nhân:** Lỗi sai quy mô lớn. Sếp nhìn lướt thấy sai be bét hàng loạt, Sếp không rảnh đi đếm hộ.
* **Hành động:** Sếp bấm **"Từ chối"**, đạp cái phiếu văng về lại `DRAFT`.
* **Kết quả:** Ép tụi nhân viên (cả Kế toán lẫn Thủ kho) phải tự lôi nhau xuống bới cái kho lên đếm lại toàn bộ từ đầu, tự sửa lỗi, tự nộp lại. Đợt kiểm kê chưa kết thúc.

### Quyền Lực 1: Bắt quả tang Đếm ngu (Tự sửa số & Phạt KPI)
* **Nguyên nhân:** Lỗi đếm ngu lẻ tẻ (Không mất hàng). Sếp đi Spot Check đột xuất và tìm thấy hàng (tụi lính lười không chịu bới kỹ hoặc mắt mù).
* **Hành động:** Sếp lười gọi tụi nó lên, dùng **Quyền 1**, tự tay sửa số trên app cho khớp kho (ví dụ sửa 9 thành 10). Bấm **Duyệt**.
* **Kết quả:**
    - Phiếu chốt sổ `COMPLETED` ngay lập tức để tiết kiệm thời gian.
    - **Quy trách nhiệm:** Hệ thống BẮT BUỘC Sếp nhập Lý do (VD: *"Lính đếm ngu, Sếp tìm thấy"*) và ghi rõ hồ sơ phạt KPI cho **CẢ 2 ĐỨA** (Thằng kho vì tội đếm mù, Thằng kế toán vì tội lười không đi double-check mà bấm nộp báo hại Sếp). Không ai được tẩy trắng.

### Quyền Lực 3: Trích xuất Camera & Bắt đền (Duyệt chênh lệch)
* **Nguyên nhân:** Mất hàng thật sự. Tụi lính bới tung kho không thấy, Sếp Spot check cũng không thấy. Sếp check **Camera** để điều tra hành vi.
* **Hành động:** Xác định được nguyên nhân, Sếp bấm **"Duyệt chênh lệch"**. BẮT BUỘC ghi rõ Lý do và chỉ định tên đứa phải đền tiền. 
    * 👉 **Nhân viên chi nhánh mình trộm (Kế toán, Sale...):** Chọn tên nhân viên trong hệ thống (những người có tài khoản) để lưu án phạt/trừ lương.
    * 👉 **Nhân viên kho trộm:** Chọn mục "Người ngoài/Tự ghi", gõ tay tên *"Ông Bảo thủ kho"* vì ông này không có tài khoản app nhưng có chữ ký trên tờ giấy đếm mù -> Bắt đền.
    * 👉 **Người ngoài trộm (Khách hàng, Shipper...):** Gõ tay *"Khách hàng / Bị trộm"*, Công ty tự chịu trách nhiệm hạch toán lỗ.
* **Kết quả:** Phiếu chốt sổ `COMPLETED`. Phần mềm tự động sinh phiếu xuất/nhập trừ kho để khớp số thực tế, lưu vĩnh viễn bản án phạt tiền.

---

## BƯỚC 3: QUYỀN HỦY (CANCELLED)
* Sếp phát hiện đợt kiểm kê tạo ra sai quy trình nặng nề hoặc phiếu rác, Sếp bấm **Hủy** để dọn dẹp hệ thống.
