/**
 * generate_usecases.js
 * Sinh tự động Use Case phân rã: mỗi chức năng = 1 file riêng, mỗi file có đúng 1 Actor.
 *
 * Cấu trúc file .drawio:
 *   - 1 Actor bên trái (ngoài boundary)
 *   - System Boundary (hộp chứa tên chức năng)
 *   - Cột 1 (x=280): Use Case chính mà actor thực hiện trực tiếp
 *   - Cột 2 (x=510): Use Case phụ được gọi qua <<include>> hoặc <<extend>>
 */

const fs   = require('fs');
const path = require('path');

// ═══════════════════════════════════════════════════════════════════════════════
//  TIỆN ÍCH
// ═══════════════════════════════════════════════════════════════════════════════

function escapeXml(s) {
    return String(s || '').replace(/[<>&'"]/g, c => ({
        '<': '&lt;', '>': '&gt;', '&': '&amp;', "'": '&apos;', '"': '&quot;'
    }[c]));
}

/**
 * Sinh file .drawio Use Case phân rã 1 chức năng
 * @param {string} filePath   Đường dẫn đầu ra
 * @param {string} title      Tên tab trong Draw.io
 * @param {string} systemName Nhãn hộp System Boundary
 * @param {string} actorName  Tên Actor (đúng 1 vai trò)
 * @param {Array}  usecases   [{id, name, row, col}]  col=1 chính | col=2 phụ
 * @param {Array}  relations  [{src, tgt, type?}]
 *                            type: 'include'|'extend'|undefined (actor→UC = undefined)
 */
function generate(filePath, title, systemName, actorName, usecases, relations) {
    const ROW_H  = 100;
    const BX     = 230, BY = 30, BW = 530;
    const maxRow = usecases.reduce((m, u) => Math.max(m, u.row), 0);
    const BH     = Math.max((maxRow + 1.5) * ROW_H, 160);
    const actorY = BY + (BH / 2) - 45;
    const C1X = 255, C2X = 475;

    let cells = '';

    // Actor
    cells += `
        <mxCell id="act" value="${escapeXml(actorName)}" vertex="1" parent="1"
            style="shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;
                   outlineConnect=0;fillColor=#dae8fc;strokeColor=#6c8ebf;strokeWidth=2;
                   fontColor=#1a1a1a;fontStyle=1;fontSize=11;">
          <mxGeometry x="60" y="${Math.round(actorY)}" width="50" height="90" as="geometry"/>
        </mxCell>`;

    // Use Cases
    usecases.forEach(uc => {
        const x = uc.col === 1 ? C1X : C2X;
        const y = BY + 50 + uc.row * ROW_H;
        const style = uc.col === 2
            ? 'ellipse;whiteSpace=wrap;html=1;fillColor=#fff2cc;strokeColor=#d6b656;strokeWidth=1.5;fontSize=10;align=center;'
            : 'ellipse;whiteSpace=wrap;html=1;fillColor=#fffde7;strokeColor=#fb8c00;strokeWidth=2;fontStyle=1;fontSize=11;align=center;';
        cells += `
        <mxCell id="${uc.id}" value="${escapeXml(uc.name)}" vertex="1" parent="1" style="${style}">
          <mxGeometry x="${x}" y="${Math.round(y)}" width="185" height="60" as="geometry"/>
        </mxCell>`;
    });

    // Edges
    relations.forEach((r, i) => {
        // UML chuẩn:
        //   <<include>>: mũi tên từ Base UC → Included UC  (src=base, tgt=included)  → giữ nguyên
        //   <<extend>> : mũi tên từ Extension UC → Base UC (src=extension, tgt=base) → SWAP src/tgt
        const edgeSrc = r.type === 'extend' ? r.tgt : r.src;
        const edgeTgt = r.type === 'extend' ? r.src : r.tgt;

        let style, label = '';
        if (r.type === 'include' || r.type === 'extend') {
            style = 'endArrow=open;endSize=12;dashed=1;html=1;rounded=0;' +
                    'strokeColor=#555555;strokeWidth=1.5;' +
                    'labelBackgroundColor=#ffffff;fontColor=#333333;fontSize=10;align=center;';
            label = `&amp;lt;&amp;lt;${r.type}&amp;gt;&amp;gt;`;
        } else {
            style = 'endArrow=none;html=1;rounded=0;strokeColor=#4a90d9;strokeWidth=1.5;';
        }
        cells += `
        <mxCell id="e${i}" value="${label}" edge="1" parent="1" source="${edgeSrc}" target="${edgeTgt}" style="${style}">
          <mxGeometry relative="1" as="geometry"/>
        </mxCell>`;
    });

    const xml = `<?xml version="1.0" encoding="utf-8"?>
<mxfile host="Electron" type="device">
  <diagram id="${path.basename(filePath,'.drawio')}-id" name="${escapeXml(title)}">
    <mxGraphModel dx="1422" dy="762" grid="1" gridSize="10" guides="1" tooltips="1"
        connect="1" arrows="1" fold="1" page="1" pageScale="1"
        pageWidth="1169" pageHeight="827" math="0" shadow="0">
      <root>
        <mxCell id="0"/>
        <mxCell id="1" parent="0"/>
        <!-- System Boundary -->
        <mxCell id="sys" value="${escapeXml(systemName)}" vertex="1" parent="1"
            style="swimlane;startSize=35;whiteSpace=wrap;html=1;fontStyle=1;fontSize=13;
                   fillColor=#f5f5f5;strokeColor=#666666;strokeWidth=2;fontColor=#333333;align=center;">
          <mxGeometry x="${BX}" y="${BY}" width="${BW}" height="${Math.round(BH)}" as="geometry"/>
        </mxCell>${cells}
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>`;

    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    fs.writeFileSync(filePath, xml, 'utf8');
    console.log(`✅  ${path.basename(filePath)}`);
}

// ═══════════════════════════════════════════════════════════════════════════════
//  DỮ LIỆU: HÀM TIỆN ÍCH TẠO FILE
// ═══════════════════════════════════════════════════════════════════════════════

const BASE = __dirname;
const out  = (role, name) => path.join(BASE, role, `${name}.drawio`);

// ═══════════════════════════════════════════════════════════════════════════════
//  ██████  STAFF — 8 chức năng
// ═══════════════════════════════════════════════════════════════════════════════

// STAFF-01: Xác thực (Đăng nhập / Đăng xuất)
generate(out('STAFF','01_xac_thuc'),
    'STAFF – Xác thực', 'Xác thực & Phiên làm việc', 'Nhân viên kho (STAFF)',
    [
        { id:'uc1', name:'Đăng nhập hệ thống',               row:0, col:1 },
        { id:'uc2', name:'Đăng xuất',                        row:1, col:1 },
        { id:'s1',  name:'Kiểm tra trạng thái\ntài khoản (ACTIVE/LOCKED)', row:0, col:2 },
        { id:'s2',  name:'Force Logout nếu bị\nkhóa giữa phiên',           row:1, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc2', tgt:'s2',  type:'extend' },
    ]
);

// STAFF-02: Tài khoản cá nhân (đổi mật khẩu)
generate(out('STAFF','02_tai_khoan_ca_nhan'),
    'STAFF – Tài khoản cá nhân', 'Quản lý Tài khoản Cá nhân', 'Nhân viên kho (STAFF)',
    [
        { id:'uc1', name:'Đổi mật khẩu cá nhân', row:0, col:1 },
        { id:'s1',  name:'Mã hóa mật khẩu\nbằng BCrypt',         row:0, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'uc1', tgt:'s1',  type:'include' },
    ]
);

// STAFF-03: Xem tồn kho chi nhánh
generate(out('STAFF','03_xem_ton_kho'),
    'STAFF – Xem tồn kho', 'Quản lý Tồn kho', 'Nhân viên kho (STAFF)',
    [
        { id:'uc1', name:'Xem tồn kho chi nhánh\n(Sắp xếp theo lastUpdated DESC)', row:0, col:1 },
        { id:'uc2', name:'Cấu hình ngưỡng cảnh báo\ntồn kho chi nhánh',           row:1, col:1 },
        { id:'s1',  name:'Lọc lô hàng sắp/đã\nhết hạn sử dụng',                  row:0, col:2 },
        { id:'s2',  name:'Highlight đỏ hàng dưới\nngưỡng cảnh báo',               row:1, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1', type:'extend' },
        { src:'uc1', tgt:'s2', type:'extend' },
    ]
);

// STAFF-04: Lập phiếu kho nháp
generate(out('STAFF','04_lap_phieu_kho'),
    'STAFF – Lập phiếu kho', 'Giao dịch Kho – Lập Phiếu Nháp', 'Nhân viên kho (STAFF)',
    [
        { id:'uc1', name:'Lập phiếu kho nháp\n(IMPORT / EXPORT / TRANSFER)', row:0, col:1 },
        { id:'uc2', name:'Xem phiếu nháp\nđang soạn',                        row:1, col:1 },
        { id:'uc3', name:'Xóa dòng sản phẩm\nkhỏi phiếu nháp',               row:2, col:1 },
        { id:'s1',  name:'Chọn lô hàng\n(NSX/HSD theo FEFO/FIFO)',            row:0, col:2 },
        { id:'s2',  name:'Kiểm tra tồn kho\nkhả dụng khi xuất',               row:1, col:2 },
        { id:'s3',  name:'Tự sinh mã phiếu\n(Prefix + UUID 8 ký tự)',         row:2, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'act', tgt:'uc3' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc1', tgt:'s2',  type:'include' },
        { src:'uc1', tgt:'s3',  type:'include' },
    ]
);

// STAFF-05: Xem lịch sử giao dịch
generate(out('STAFF','05_xem_lich_su_giao_dich'),
    'STAFF – Xem lịch sử giao dịch', 'Lịch sử Giao dịch Kho', 'Nhân viên kho (STAFF)',
    [
        { id:'uc1', name:'Xem danh sách phiếu kho\nchi nhánh (lọc ngày/loại)', row:0, col:1 },
        { id:'uc2', name:'Xem chi tiết phiếu kho\n(SP, giá, lô hàng)',         row:1, col:1 },
        { id:'s1',  name:'Xuất báo cáo Excel\nhoặc In PDF hóa đơn',            row:0, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc2', tgt:'s1',  type:'extend' },
    ]
);

// STAFF-06: Kiểm kê kho (tạo nháp + nhập số liệu)
generate(out('STAFF','06_kiem_ke_kho'),
    'STAFF – Kiểm kê kho', 'Kiểm kê Kho (Giai đoạn Nhập liệu)', 'Nhân viên kho (STAFF)',
    [
        { id:'uc1', name:'Tạo phiên kiểm kê nháp\n(trạng thái DRAFT)',       row:0, col:1 },
        { id:'uc2', name:'Nhập số liệu kiểm đếm\nthực tế (actual_quantity)', row:1, col:1 },
        { id:'s1',  name:'Tự sinh mã kiểm kê\n(ST + UUID 8 ký tự)',          row:0, col:2 },
        { id:'s2',  name:'Hiển thị số lượng sổ sách\n(expected_quantity) để đối chiếu', row:1, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc2', tgt:'s2',  type:'include' },
    ]
);

// STAFF-07: Điều chuyển nhân sự (gửi yêu cầu + xác nhận)
generate(out('STAFF','07_dieu_chuyen_nhan_su'),
    'STAFF – Điều chuyển nhân sự', 'Quy trình Điều chuyển Nhân sự', 'Nhân viên kho (STAFF)',
    [
        { id:'uc1', name:'Gửi yêu cầu chuyển\nchi nhánh (tự tạo đơn)',       row:0, col:1 },
        { id:'uc2', name:'Xác nhận đồng ý điều chuyển\n(khi Manager đề xuất)',row:1, col:1 },
        { id:'s1',  name:'Đơn chuyển sang\ntrạng thái STAFF_CONFIRMED',       row:0, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc2', tgt:'s1',  type:'include' },
    ]
);

// STAFF-08: Xuất Excel / In PDF
generate(out('STAFF','08_xuat_excel_in_pdf'),
    'STAFF – Xuất Excel & In PDF', 'Xuất dữ liệu & Báo cáo', 'Nhân viên kho (STAFF)',
    [
        { id:'uc1', name:'Xuất báo cáo tồn kho\nra file Excel (.xlsx)',       row:0, col:1 },
        { id:'uc2', name:'In hóa đơn phiếu kho\nra file PDF',                 row:1, col:1 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
    ]
);

// ═══════════════════════════════════════════════════════════════════════════════
//  ███    ███  MANAGER — 14 chức năng
// ═══════════════════════════════════════════════════════════════════════════════

// MGR-01: Xác thực
generate(out('MANAGER','01_xac_thuc'),
    'MANAGER – Xác thực', 'Xác thực & Phiên làm việc', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Đăng nhập hệ thống',               row:0, col:1 },
        { id:'uc2', name:'Đăng xuất',                        row:1, col:1 },
        { id:'s1',  name:'Kiểm tra trạng thái\ntài khoản (ACTIVE/LOCKED)', row:0, col:2 },
        { id:'s2',  name:'Force Logout nếu bị\nkhóa giữa phiên',           row:1, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1', type:'include' },
        { src:'uc2', tgt:'s2', type:'extend' },
    ]
);

// MGR-02: Tài khoản cá nhân
generate(out('MANAGER','02_tai_khoan_ca_nhan'),
    'MANAGER – Tài khoản cá nhân', 'Quản lý Tài khoản Cá nhân', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Đổi mật khẩu cá nhân', row:0, col:1 },
        { id:'s1',  name:'Mã hóa mật khẩu\nbằng BCrypt', row:0, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'uc1', tgt:'s1', type:'include' },
    ]
);

// MGR-03: Quản lý nhân viên STAFF
generate(out('MANAGER','03_quan_ly_nhan_vien'),
    'MANAGER – Quản lý nhân viên', 'Quản lý Nhân viên STAFF Chi nhánh', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Xem danh sách STAFF\ntrực thuộc chi nhánh',         row:0, col:1 },
        { id:'uc2', name:'Thêm tài khoản STAFF\nmới cho chi nhánh',           row:1, col:1 },
        { id:'uc3', name:'Cập nhật thông tin\ntài khoản STAFF',               row:2, col:1 },
        { id:'uc4', name:'Khóa / Mở khóa\ntài khoản STAFF',                   row:3, col:1 },
        { id:'uc5', name:'Xóa tài khoản STAFF\n(nếu chưa phát sinh GD)',      row:4, col:1 },
        { id:'s1',  name:'Kiểm tra tự khóa\nchính mình (bị cấm)',             row:3, col:2 },
        { id:'s2',  name:'Kiểm tra ràng buộc\nkhóa ngoại (receipts/stocktakes)',row:4, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'act', tgt:'uc3' },
        { src:'act', tgt:'uc4' },
        { src:'act', tgt:'uc5' },
        { src:'uc4', tgt:'s1',  type:'include' },
        { src:'uc5', tgt:'s2',  type:'include' },
    ]
);

// MGR-04: Quản lý sản phẩm
generate(out('MANAGER','04_quan_ly_san_pham'),
    'MANAGER – Quản lý sản phẩm', 'Quản lý Sản phẩm', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Xem & Tìm kiếm\ndanh sách sản phẩm',               row:0, col:1 },
        { id:'uc2', name:'Thêm sản phẩm mới\n(code duy nhất, viết hoa)',      row:1, col:1 },
        { id:'uc3', name:'Cập nhật thông tin\nsản phẩm',                      row:2, col:1 },
        { id:'uc4', name:'Xóa sản phẩm\n(nếu chưa có GD)',                   row:3, col:1 },
        { id:'uc5', name:'Nhập hàng loạt SP\ntừ file Excel mẫu',             row:4, col:1 },
        { id:'s1',  name:'Kiểm tra unique code\n& validate has_expiry',       row:1, col:2 },
        { id:'s2',  name:'Không đổi has_expiry\nnếu đã có tồn kho/GD',       row:2, col:2 },
        { id:'s3',  name:'Kiểm tra ràng buộc\nkhóa ngoại receipt_details',   row:3, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'act', tgt:'uc3' },
        { src:'act', tgt:'uc4' },
        { src:'act', tgt:'uc5' },
        { src:'uc2', tgt:'s1', type:'include' },
        { src:'uc3', tgt:'s2', type:'include' },
        { src:'uc4', tgt:'s3', type:'include' },
    ]
);

// MGR-05: Xem tồn kho
generate(out('MANAGER','05_xem_ton_kho'),
    'MANAGER – Xem tồn kho', 'Quản lý Tồn kho Chi nhánh', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Xem tồn kho chi nhánh\n(Sắp xếp lastUpdated DESC)', row:0, col:1 },
        { id:'uc2', name:'Cấu hình ngưỡng cảnh báo\ntồn kho chi nhánh',       row:1, col:1 },
        { id:'s1',  name:'Lọc lô hàng sắp/đã\nhết hạn sử dụng',              row:0, col:2 },
        { id:'s2',  name:'Highlight đỏ hàng dưới\nngưỡng cảnh báo',           row:1, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1', type:'extend' },
        { src:'uc1', tgt:'s2', type:'extend' },
    ]
);

// MGR-06: Lập phiếu kho nháp
generate(out('MANAGER','06_lap_phieu_kho'),
    'MANAGER – Lập phiếu kho', 'Giao dịch Kho – Lập Phiếu Nháp', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Lập phiếu kho nháp\n(IMPORT / EXPORT / TRANSFER)', row:0, col:1 },
        { id:'uc2', name:'Sửa / Xóa phiếu nháp\n(chỉ khi còn DRAFT)',        row:1, col:1 },
        { id:'s1',  name:'Chọn lô hàng\n(NSX/HSD theo FEFO/FIFO)',           row:0, col:2 },
        { id:'s2',  name:'Kiểm tra tồn kho\nkhả dụng khi xuất',              row:1, col:2 },
        { id:'s3',  name:'Tự sinh mã phiếu\n(Prefix + UUID 8 ký tự)',        row:2, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc1', tgt:'s2',  type:'include' },
        { src:'uc1', tgt:'s3',  type:'include' },
    ]
);

// MGR-07: Phê duyệt phiếu kho
generate(out('MANAGER','07_phe_duyet_phieu_kho'),
    'MANAGER – Phê duyệt phiếu kho', 'Phê duyệt Phiếu Kho', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Phê duyệt phiếu kho\n(DRAFT → COMPLETED)',          row:0, col:1 },
        { id:'uc2', name:'Từ chối/Hủy phiếu kho\n(DRAFT → CANCELLED)',        row:1, col:1 },
        { id:'s1',  name:'Cập nhật số lượng\ntồn kho thực tế (@Transactional)',row:0, col:2 },
        { id:'s2',  name:'Tự động tăng công nợ\nNCC hoặc KH (nếu UNPAID)',    row:1, col:2 },
        { id:'s3',  name:'Khóa phiếu vĩnh viễn\n(không sửa/xóa được)',        row:2, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc1', tgt:'s2',  type:'extend' },
        { src:'uc1', tgt:'s3',  type:'include' },
        { src:'uc2', tgt:'s3',  type:'include' },
    ]
);

// MGR-08: Xem lịch sử giao dịch
generate(out('MANAGER','08_xem_lich_su_giao_dich'),
    'MANAGER – Xem lịch sử giao dịch', 'Lịch sử Giao dịch Kho', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Xem danh sách phiếu kho\nchi nhánh (lọc ngày/loại)', row:0, col:1 },
        { id:'uc2', name:'Xem chi tiết phiếu kho\n(SP, giá, lô hàng)',          row:1, col:1 },
        { id:'s1',  name:'Xuất báo cáo Excel\nhoặc In PDF hóa đơn',             row:0, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc2', tgt:'s1',  type:'extend' },
    ]
);

// MGR-09: Kiểm kê kho (tạo + nhập + duyệt)
generate(out('MANAGER','09_kiem_ke_kho'),
    'MANAGER – Kiểm kê kho', 'Kiểm kê Kho', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Tạo phiên kiểm kê nháp',                            row:0, col:1 },
        { id:'uc2', name:'Nhập số liệu kiểm đếm\nthực tế',                    row:1, col:1 },
        { id:'uc3', name:'Hoàn tất kiểm kê\n(DRAFT → COMPLETED)',             row:2, col:1 },
        { id:'uc4', name:'Hủy phiên kiểm kê\n(DRAFT → CANCELLED)',            row:3, col:1 },
        { id:'s1',  name:'Tự động sinh phiếu\nADJUST_IN / ADJUST_OUT',        row:2, col:2 },
        { id:'s2',  name:'Cập nhật tồn kho\nthực tế theo chênh lệch',         row:3, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'act', tgt:'uc3' },
        { src:'act', tgt:'uc4' },
        { src:'uc3', tgt:'s1',  type:'include' },
        { src:'uc3', tgt:'s2',  type:'include' },
    ]
);

// MGR-10: Quản lý đối tác (NCC & KH)
generate(out('MANAGER','10_quan_ly_doi_tac'),
    'MANAGER – Quản lý đối tác', 'Quản lý Đối tác (NCC & Khách hàng)', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Xem danh sách\nNhà cung cấp / Khách hàng',         row:0, col:1 },
        { id:'uc2', name:'Thêm đối tác mới\n(NCC hoặc KH)',                  row:1, col:1 },
        { id:'uc3', name:'Cập nhật thông tin\nđối tác',                      row:2, col:1 },
        { id:'uc4', name:'Vô hiệu hóa đối tác\n(chuyển sang INACTIVE)',      row:3, col:1 },
        { id:'uc5', name:'Nhập hàng loạt NCC\ntừ file Excel mẫu',           row:4, col:1 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'act', tgt:'uc3' },
        { src:'act', tgt:'uc4' },
        { src:'act', tgt:'uc5' },
    ]
);

// MGR-11: Cập nhật công nợ
generate(out('MANAGER','11_cap_nhat_cong_no'),
    'MANAGER – Cập nhật công nợ', 'Quản lý Thanh toán & Công nợ', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Xem danh sách phiếu\nchưa thanh toán (UNPAID)',     row:0, col:1 },
        { id:'uc2', name:'Thực hiện thanh toán\n(UNPAID → PAID)',              row:1, col:1 },
        { id:'s1',  name:'Giảm trừ công nợ\nNCC hoặc KH tương ứng',          row:1, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc2', tgt:'s1',  type:'include' },
    ]
);

// MGR-12: Điều chuyển hàng hóa (2 bước)
generate(out('MANAGER','12_dieu_chuyen_hang_hoa'),
    'MANAGER – Điều chuyển hàng hóa', 'Điều chuyển Hàng hóa 2 bước', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Phê duyệt phiếu TRANSFER\n(Xuất → trạng thái IN_TRANSIT)', row:0, col:1 },
        { id:'uc2', name:'Xác nhận nhận hàng\ntại chi nhánh đích',                   row:1, col:1 },
        { id:'s1',  name:'Trừ tồn kho\nchi nhánh nguồn',                             row:0, col:2 },
        { id:'s2',  name:'Cộng tồn kho\nchi nhánh đích (RECEIVED)',                  row:1, col:2 },
        { id:'s3',  name:'Tự động sinh ADJUST_OUT\nnếu có hao hụt vận chuyển',       row:2, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc2', tgt:'s2',  type:'include' },
        { src:'uc2', tgt:'s3',  type:'extend' },
    ]
);

// MGR-13: Điều chuyển nhân sự
generate(out('MANAGER','13_dieu_chuyen_nhan_su'),
    'MANAGER – Điều chuyển nhân sự', 'Quy trình Điều chuyển Nhân sự', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Đề xuất điều chuyển\nSTAFF sang chi nhánh khác',     row:0, col:1 },
        { id:'uc2', name:'Phê duyệt bước 2\n(Manager thông qua – MANAGER_APPROVED)', row:1, col:1 },
        { id:'s1',  name:'Hệ thống yêu cầu STAFF\nxác nhận đồng ý (BĐ1)',      row:0, col:2 },
        { id:'s2',  name:'Ghi nhận vào\naudit_logs',                            row:1, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc2', tgt:'s2',  type:'include' },
    ]
);

// MGR-14: Xuất Excel / In PDF
generate(out('MANAGER','14_xuat_excel_in_pdf'),
    'MANAGER – Xuất Excel & In PDF', 'Xuất dữ liệu & Báo cáo', 'Quản lý chi nhánh (MANAGER)',
    [
        { id:'uc1', name:'Xuất báo cáo tồn kho\nchi nhánh ra Excel',         row:0, col:1 },
        { id:'uc2', name:'In hóa đơn phiếu kho\nra file PDF',                row:1, col:1 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
    ]
);

// ═══════════════════════════════════════════════════════════════════════════════
//  █████   ADMIN — 19 chức năng
// ═══════════════════════════════════════════════════════════════════════════════

// ADMIN-01: Xác thực
generate(out('ADMIN','01_xac_thuc'),
    'ADMIN – Xác thực', 'Xác thực & Phiên làm việc', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Đăng nhập hệ thống',               row:0, col:1 },
        { id:'uc2', name:'Đăng xuất',                        row:1, col:1 },
        { id:'s1',  name:'Kiểm tra trạng thái\ntài khoản (ACTIVE/LOCKED)', row:0, col:2 },
        { id:'s2',  name:'Force Logout nếu bị\nkhóa giữa phiên',           row:1, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1', type:'include' },
        { src:'uc2', tgt:'s2', type:'extend' },
    ]
);

// ADMIN-02: Tài khoản cá nhân
generate(out('ADMIN','02_tai_khoan_ca_nhan'),
    'ADMIN – Tài khoản cá nhân', 'Quản lý Tài khoản Cá nhân', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Đổi mật khẩu cá nhân', row:0, col:1 },
        { id:'s1',  name:'Mã hóa mật khẩu\nbằng BCrypt', row:0, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'uc1', tgt:'s1', type:'include' },
    ]
);

// ADMIN-03: Quản lý toàn bộ người dùng
generate(out('ADMIN','03_quan_ly_nguoi_dung'),
    'ADMIN – Quản lý người dùng', 'Quản lý Tất cả Tài khoản Người dùng', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Xem toàn bộ tài khoản\n(ADMIN / MANAGER / STAFF)',  row:0, col:1 },
        { id:'uc2', name:'Thêm tài khoản mới\n(bất kỳ vai trò)',              row:1, col:1 },
        { id:'uc3', name:'Cập nhật thông tin\nbất kỳ tài khoản',              row:2, col:1 },
        { id:'uc4', name:'Khóa / Mở khóa\nbất kỳ tài khoản',                 row:3, col:1 },
        { id:'uc5', name:'Xóa tài khoản\n(nếu chưa phát sinh GD)',           row:4, col:1 },
        { id:'s1',  name:'Kiểm tra: không tự hạ\nquyền ADMIN đang đăng nhập',row:2, col:2 },
        { id:'s2',  name:'Không tự khóa\nchính mình',                        row:3, col:2 },
        { id:'s3',  name:'Kiểm tra ràng buộc\nkhóa ngoại (receipts)',        row:4, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'act', tgt:'uc3' },
        { src:'act', tgt:'uc4' },
        { src:'act', tgt:'uc5' },
        { src:'uc3', tgt:'s1',  type:'include' },
        { src:'uc4', tgt:'s2',  type:'include' },
        { src:'uc5', tgt:'s3',  type:'include' },
    ]
);

// ADMIN-04: Quản lý chi nhánh
generate(out('ADMIN','04_quan_ly_chi_nhanh'),
    'ADMIN – Quản lý chi nhánh', 'Quản lý Chi nhánh (Toàn cục)', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Xem danh sách chi nhánh',                           row:0, col:1 },
        { id:'uc2', name:'Thêm chi nhánh mới\n(tên duy nhất)',                row:1, col:1 },
        { id:'uc3', name:'Cập nhật thông tin\nchi nhánh',                     row:2, col:1 },
        { id:'uc4', name:'Xóa chi nhánh\n(nếu không có ràng buộc)',           row:3, col:1 },
        { id:'s1',  name:'Kiểm tra ràng buộc:\nkhông có user/inventories/receipts', row:3, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'act', tgt:'uc3' },
        { src:'act', tgt:'uc4' },
        { src:'uc4', tgt:'s1',  type:'include' },
    ]
);

// ADMIN-05: Quản lý danh mục
generate(out('ADMIN','05_quan_ly_danh_muc'),
    'ADMIN – Quản lý danh mục', 'Quản lý Danh mục Sản phẩm', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Xem danh sách danh mục',                           row:0, col:1 },
        { id:'uc2', name:'Thêm danh mục mới\n(tên duy nhất)',                row:1, col:1 },
        { id:'uc3', name:'Cập nhật tên danh mục',                            row:2, col:1 },
        { id:'uc4', name:'Xóa danh mục\n(nếu không có sản phẩm)',           row:3, col:1 },
        { id:'s1',  name:'Kiểm tra ràng buộc\nFK tới products (RESTRICT)',   row:3, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'act', tgt:'uc3' },
        { src:'act', tgt:'uc4' },
        { src:'uc4', tgt:'s1',  type:'include' },
    ]
);

// ADMIN-06: Quản lý sản phẩm
generate(out('ADMIN','06_quan_ly_san_pham'),
    'ADMIN – Quản lý sản phẩm', 'Quản lý Sản phẩm', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Xem & Tìm kiếm\ndanh sách sản phẩm',               row:0, col:1 },
        { id:'uc2', name:'Thêm sản phẩm mới\n(code duy nhất, viết hoa)',      row:1, col:1 },
        { id:'uc3', name:'Cập nhật thông tin\nsản phẩm',                      row:2, col:1 },
        { id:'uc4', name:'Xóa sản phẩm\n(nếu chưa có GD)',                   row:3, col:1 },
        { id:'uc5', name:'Nhập hàng loạt SP\ntừ file Excel mẫu',             row:4, col:1 },
        { id:'s1',  name:'Kiểm tra unique code\n& validate has_expiry',       row:1, col:2 },
        { id:'s2',  name:'Không đổi has_expiry\nnếu đã có tồn kho/GD',       row:2, col:2 },
        { id:'s3',  name:'Kiểm tra ràng buộc\nFK receipt_details',           row:3, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'act', tgt:'uc3' },
        { src:'act', tgt:'uc4' },
        { src:'act', tgt:'uc5' },
        { src:'uc2', tgt:'s1',  type:'include' },
        { src:'uc3', tgt:'s2',  type:'include' },
        { src:'uc4', tgt:'s3',  type:'include' },
    ]
);

// ADMIN-07: Xem tồn kho toàn cục
generate(out('ADMIN','07_xem_ton_kho_toan_cuc'),
    'ADMIN – Xem tồn kho toàn cục', 'Quản lý Tồn kho Toàn cục', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Xem tồn kho tất cả\nchi nhánh (Toàn cục)',          row:0, col:1 },
        { id:'uc2', name:'Cấu hình ngưỡng cảnh báo\ntoàn cục (Preferences)',  row:1, col:1 },
        { id:'uc3', name:'Cấu hình ngưỡng cảnh báo\ntheo từng chi nhánh',     row:2, col:1 },
        { id:'s1',  name:'Lọc lô hàng sắp/đã\nhết hạn sử dụng',              row:0, col:2 },
        { id:'s2',  name:'Highlight đỏ hàng dưới\nngưỡng cảnh báo',           row:1, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'act', tgt:'uc3' },
        { src:'uc1', tgt:'s1',  type:'extend' },
        { src:'uc1', tgt:'s2',  type:'extend' },
    ]
);

// ADMIN-08: Lập phiếu kho
generate(out('ADMIN','08_lap_phieu_kho'),
    'ADMIN – Lập phiếu kho', 'Giao dịch Kho – Lập Phiếu Nháp', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Lập phiếu kho nháp\n(bất kỳ chi nhánh)',           row:0, col:1 },
        { id:'uc2', name:'Sửa / Xóa phiếu nháp\n(chỉ khi còn DRAFT)',       row:1, col:1 },
        { id:'s1',  name:'Chọn lô hàng\n(NSX/HSD theo FEFO/FIFO)',          row:0, col:2 },
        { id:'s2',  name:'Kiểm tra tồn kho\nkhả dụng khi xuất',             row:1, col:2 },
        { id:'s3',  name:'Tự sinh mã phiếu\n(Prefix + UUID 8 ký tự)',       row:2, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc1', tgt:'s2',  type:'include' },
        { src:'uc1', tgt:'s3',  type:'include' },
    ]
);

// ADMIN-09: Phê duyệt phiếu kho
generate(out('ADMIN','09_phe_duyet_phieu_kho'),
    'ADMIN – Phê duyệt phiếu kho', 'Phê duyệt Phiếu Kho (Toàn cục)', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Phê duyệt phiếu kho\n(DRAFT → COMPLETED)',          row:0, col:1 },
        { id:'uc2', name:'Từ chối/Hủy phiếu kho\n(DRAFT → CANCELLED)',        row:1, col:1 },
        { id:'s1',  name:'Cập nhật tồn kho thực tế\n(@Transactional)',        row:0, col:2 },
        { id:'s2',  name:'Tự động tăng công nợ\nNCC hoặc KH (nếu UNPAID)',    row:1, col:2 },
        { id:'s3',  name:'Khóa phiếu vĩnh viễn',                              row:2, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc1', tgt:'s2',  type:'extend' },
        { src:'uc1', tgt:'s3',  type:'include' },
        { src:'uc2', tgt:'s3',  type:'include' },
    ]
);

// ADMIN-10: Xem lịch sử giao dịch
generate(out('ADMIN','10_xem_lich_su_giao_dich'),
    'ADMIN – Xem lịch sử giao dịch', 'Lịch sử Giao dịch Kho (Toàn cục)', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Xem tất cả phiếu kho\nmọi chi nhánh',               row:0, col:1 },
        { id:'uc2', name:'Xem chi tiết phiếu kho',                            row:1, col:1 },
        { id:'s1',  name:'Xuất báo cáo Excel\nhoặc In PDF hóa đơn',          row:0, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc2', tgt:'s1',  type:'extend' },
    ]
);

// ADMIN-11: Kiểm kê kho
generate(out('ADMIN','11_kiem_ke_kho'),
    'ADMIN – Kiểm kê kho', 'Kiểm kê Kho (Toàn cục)', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Tạo phiên kiểm kê nháp',                           row:0, col:1 },
        { id:'uc2', name:'Nhập số liệu kiểm đếm\nthực tế',                   row:1, col:1 },
        { id:'uc3', name:'Hoàn tất kiểm kê\n(DRAFT → COMPLETED)',            row:2, col:1 },
        { id:'uc4', name:'Hủy phiên kiểm kê',                                row:3, col:1 },
        { id:'s1',  name:'Tự động sinh phiếu\nADJUST_IN / ADJUST_OUT',       row:2, col:2 },
        { id:'s2',  name:'Cập nhật tồn kho\nthực tế theo chênh lệch',        row:3, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'act', tgt:'uc3' },
        { src:'act', tgt:'uc4' },
        { src:'uc3', tgt:'s1',  type:'include' },
        { src:'uc3', tgt:'s2',  type:'include' },
    ]
);

// ADMIN-12: Điều chuyển hàng hóa
generate(out('ADMIN','12_dieu_chuyen_hang_hoa'),
    'ADMIN – Điều chuyển hàng hóa', 'Điều chuyển Hàng hóa 2 bước', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Phê duyệt phiếu TRANSFER\n(Xuất → trạng thái IN_TRANSIT)', row:0, col:1 },
        { id:'uc2', name:'Xác nhận nhận hàng\ntại chi nhánh đích (RECEIVED)',        row:1, col:1 },
        { id:'s1',  name:'Trừ tồn kho\nchi nhánh nguồn',                             row:0, col:2 },
        { id:'s2',  name:'Cộng tồn kho\nchi nhánh đích',                             row:1, col:2 },
        { id:'s3',  name:'Tự động sinh ADJUST_OUT\nnếu có hao hụt vận chuyển',       row:2, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc2', tgt:'s2',  type:'include' },
        { src:'uc2', tgt:'s3',  type:'extend' },
    ]
);

// ADMIN-13: Quản lý đối tác toàn cục
generate(out('ADMIN','13_quan_ly_doi_tac'),
    'ADMIN – Quản lý đối tác', 'Quản lý Đối tác Toàn cục (NCC & KH)', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Xem danh sách\nNhà cung cấp / Khách hàng',         row:0, col:1 },
        { id:'uc2', name:'Thêm đối tác mới',                                  row:1, col:1 },
        { id:'uc3', name:'Cập nhật thông tin\nđối tác',                      row:2, col:1 },
        { id:'uc4', name:'Vô hiệu hóa đối tác\n(→ INACTIVE)',                row:3, col:1 },
        { id:'uc5', name:'Nhập hàng loạt NCC\ntừ file Excel mẫu',           row:4, col:1 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'act', tgt:'uc3' },
        { src:'act', tgt:'uc4' },
        { src:'act', tgt:'uc5' },
    ]
);

// ADMIN-14: Cập nhật công nợ
generate(out('ADMIN','14_cap_nhat_cong_no'),
    'ADMIN – Cập nhật công nợ', 'Quản lý Thanh toán & Công nợ', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Xem danh sách phiếu\nchưa thanh toán (UNPAID)',     row:0, col:1 },
        { id:'uc2', name:'Thực hiện thanh toán\n(UNPAID → PAID)',              row:1, col:1 },
        { id:'s1',  name:'Giảm trừ công nợ\nNCC hoặc KH tương ứng',          row:1, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'uc2', tgt:'s1',  type:'include' },
    ]
);

// ADMIN-15: Phê duyệt điều chuyển nhân sự (BĐ3)
generate(out('ADMIN','15_phe_duyet_dieu_chuyen_nhan_su'),
    'ADMIN – Phê duyệt điều chuyển nhân sự', 'Quy trình Điều chuyển Nhân sự – Bước 3', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Xem danh sách đơn\nđiều chuyển MANAGER_APPROVED',   row:0, col:1 },
        { id:'uc2', name:'Phê duyệt cuối cùng\n(→ trạng thái APPROVED)',      row:1, col:1 },
        { id:'uc3', name:'Từ chối đơn\n(→ trạng thái REJECTED)',              row:2, col:1 },
        { id:'s1',  name:'Tự động cập nhật\nbranch_id của nhân viên',         row:1, col:2 },
        { id:'s2',  name:'Ghi nhận approved_by\nvà approved_at',              row:2, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'act', tgt:'uc2' },
        { src:'act', tgt:'uc3' },
        { src:'uc2', tgt:'s1',  type:'include' },
        { src:'uc2', tgt:'s2',  type:'include' },
    ]
);

// ADMIN-16: Sao lưu dữ liệu
generate(out('ADMIN','16_sao_luu_du_lieu'),
    'ADMIN – Sao lưu dữ liệu', 'Quản trị Hệ thống – Sao lưu', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Khởi động sao lưu\ntrực tiếp trên Web',            row:0, col:1 },
        { id:'s1',  name:'Backend truy vấn DB\nvà đóng gói dữ liệu',         row:0, col:2 },
        { id:'s2',  name:'Trình duyệt tải xuống\nfile JSON hoặc SQL',         row:1, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc1', tgt:'s2',  type:'include' },
    ]
);

// ADMIN-17: Phục hồi dữ liệu
generate(out('ADMIN','17_phuc_hoi_du_lieu'),
    'ADMIN – Phục hồi dữ liệu', 'Quản trị Hệ thống – Phục hồi', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Tải file sao lưu lên\n(.json / .sql) qua Web UI',  row:0, col:1 },
        { id:'s1',  name:'Backend phân tích file\nvà nạp đè vào PostgreSQL', row:0, col:2 },
        { id:'s2',  name:'Thực hiện trong\ntransaction bảo mật',             row:1, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc1', tgt:'s2',  type:'include' },
    ]
);

// ADMIN-18: Nhập hàng loạt Excel
generate(out('ADMIN','18_nhap_excel_hang_loat'),
    'ADMIN – Nhập Excel hàng loạt', 'Nhập dữ liệu hàng loạt từ Excel', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Tải file Excel mẫu\nlên hệ thống',                  row:0, col:1 },
        { id:'s1',  name:'Phân tích file\nbằng Apache POI',                   row:0, col:2 },
        { id:'s2',  name:'Thêm hàng loạt bản ghi\nvào DB trong 1 transaction',row:1, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'uc1', tgt:'s1',  type:'include' },
        { src:'uc1', tgt:'s2',  type:'include' },
    ]
);

// ADMIN-19: Tra cứu Audit Log
generate(out('ADMIN','19_tra_cuu_audit_log'),
    'ADMIN – Tra cứu Audit Log', 'Nhật ký Hoạt động Hệ thống', 'Quản trị viên (ADMIN)',
    [
        { id:'uc1', name:'Tra cứu nhật ký\nAudit Log theo bộ lọc',            row:0, col:1 },
        { id:'s1',  name:'Chỉ đọc – Không cho phép\nsửa hoặc xóa Audit Log', row:0, col:2 },
    ],
    [
        { src:'act', tgt:'uc1' },
        { src:'uc1', tgt:'s1',  type:'include' },
    ]
);

console.log('\n🎉 Hoàn thành! Tổng cộng:');
console.log('   STAFF:   8 file  → ./STAFF/');
console.log('   MANAGER: 14 file → ./MANAGER/');
console.log('   ADMIN:   19 file → ./ADMIN/');
console.log('   Tổng:    41 file .drawio\n');
