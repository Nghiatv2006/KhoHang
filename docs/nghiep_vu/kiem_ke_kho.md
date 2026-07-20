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

Sếp nhận được phiếu báo lệch. Sếp chỉ có quyền **đọc** số liệu, **KHÔNG ĐƯỢC SỬA** số của lính. Sếp ngồi phòng soi Camera hoặc đối chất hồ sơ, rồi tung ra 1 trong 2 quyền lực sinh sát:

### Quyền Lực 1: Tát gáo nước lạnh (Yêu cầu đếm lại)
* **Nguyên nhân A – Sai be bét:** Sếp nhìn lướt thấy sai hàng loạt, Sếp không rảnh đi đếm hộ.
* **Nguyên nhân B – Đếm ngu lẻ tẻ:** Sếp đi Spot Check đột xuất và tìm thấy hàng (tụi lính lười không chịu bới kỹ hoặc mắt mù). Hàng thực tế còn đủ hoặc khác số lính báo.
* **Hành động:** Sếp bấm **"Yêu cầu đếm lại"**, ghi rõ lý do (VD: *"Tao đếm lại đủ hàng, mày đếm sai"* hoặc *"Tao đếm chỉ còn 5, không phải 8 như mày báo"*). Phiếu bị đạp văng về lại `DRAFT`.
* **Kết quả:** Ép tụi nhân viên (cả Kế toán lẫn Thủ kho) phải tự lôi nhau xuống bới cái kho lên đếm lại, **TỰ TAY sửa số**, tự nộp lại. Ai nhập số thì người đó chịu trách nhiệm với con số đó.
    - *Nếu đếm lại thấy đủ*: Lính sửa số cho khớp, bấm Nộp -> Phần mềm tự chốt sổ `COMPLETED` ngay, không cần quay lại Sếp.
    - *Nếu đếm lại vẫn thiếu*: Lính sửa số đúng thực tế, bấm Nộp -> Phiếu lại bay lên bàn Sếp (`PENDING_APPROVAL`) để Sếp xử lý tiếp.

### Quyền Lực 2: Trích xuất Camera & Bắt đền (Duyệt chênh lệch)
* **Nguyên nhân:** Mất hàng thật sự. Tụi lính bới tung kho không thấy, Sếp Spot check cũng không thấy. Sếp check **Camera** để điều tra hành vi.
* **Hành động:** Xác định được nguyên nhân, Sếp bấm **"Duyệt chênh lệch"**. BẮT BUỘC ghi rõ Lý do và chỉ định tên đứa phải đền tiền. 
    * Nhân viên chi nhánh mình trộm (Kế toán, Sale...): Chọn tên nhân viên trong hệ thống (những người có tài khoản) để lưu án phạt/trừ lương.
    * Nhân viên kho trộm: Chọn mục "Người ngoài/Tự ghi", gõ tay tên *"Ông Bảo thủ kho"* vì ông này không có tài khoản app nhưng có chữ ký trên tờ giấy đếm mù -> Bắt đền.
    * Người ngoài trộm (Khách hàng, Shipper...): Gõ tay *"Khách hàng / Bị trộm"*, Công ty tự chịu trách nhiệm hạch toán lỗ.
* **Kết quả:** Phiếu chốt sổ `COMPLETED`. Phần mềm tự động sinh phiếu xuất/nhập trừ kho để khớp số thực tế, lưu vĩnh viễn bản án phạt tiền.

---

## BƯỚC 3: QUYỀN HỦY (CANCELLED)
* Sếp phát hiện đợt kiểm kê tạo ra sai quy trình nặng nề hoặc phiếu rác, Sếp bấm **Hủy** để dọn dẹp hệ thống.
