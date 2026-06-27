const fs = require('fs');
const path = require('path');

function createUseCaseDiagram(filename, diagramName, actors, usecases, relations, outputDir = '.') {
    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
    }

    let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
    xml += `<mxfile host="Electron" modified="2024-01-01T00:00:00.000Z" agent="JS Script" type="device">\n`;
    xml += `  <diagram id="${diagramName}" name="${diagramName}">\n`;
    xml += `    <mxGraphModel dx="1000" dy="1000" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1">\n`;
    xml += `      <root>\n`;
    xml += `        <mxCell id="0"/>\n`;
    xml += `        <mxCell id="1" parent="0"/>\n`;

    // Actors
    actors.forEach(act => {
        const x = act.x || 50;
        const y = act.y || 200;
        xml += `        <mxCell id="${act.id}" value="${act.name.replace(/\n/g, '&#xa;')}" style="shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;" vertex="1" parent="1">\n`;
        xml += `          <mxGeometry x="${x}" y="${y}" width="30" height="60" as="geometry"/>\n`;
        xml += `        </mxCell>\n`;
    });

    const maxRow = usecases.length ? Math.max(...usecases.map(uc => uc.row)) : 0;
    const sysHeight = Math.max(400, 150 + maxRow * 80);
    const maxCol = usecases.length ? Math.max(...usecases.map(uc => uc.col)) : 1;
    const sysWidth = Math.max(350, 200 + maxCol * 200);

    const sysId = 'sys';
    xml += `        <mxCell id="${sysId}" value="WareHub System" style="shape=rect;html=1;verticalAlign=top;fontStyle=1;fillColor=none;" vertex="1" parent="1">\n`;
    xml += `          <mxGeometry x="150" y="50" width="${sysWidth}" height="${sysHeight}" as="geometry"/>\n`;
    xml += `        </mxCell>\n`;

    usecases.forEach(uc => {
        const x = 50 + (uc.col - 1) * 200;
        const y = 50 + uc.row * 80;
        xml += `        <mxCell id="${uc.id}" value="${uc.name.replace(/\n/g, '&#xa;')}" style="ellipse;whiteSpace=wrap;html=1;" vertex="1" parent="${sysId}">\n`;
        xml += `          <mxGeometry x="${x}" y="${y}" width="140" height="60" as="geometry"/>\n`;
        xml += `        </mxCell>\n`;
    });

    let edgeCount = 0;
    relations.forEach(rel => {
        const edgeId = `edge_${edgeCount++}`;
        const rtype = rel.type || 'normal';
        let style = "endArrow=none;html=1;rounded=0;";
        let value = "";

        if (rtype === 'include') {
            style = "endArrow=open;endSize=12;dashed=1;html=1;rounded=0;";
            value = "&lt;&lt;include&gt;&gt;";
        } else if (rtype === 'extend') {
            style = "endArrow=open;endSize=12;dashed=1;html=1;rounded=0;";
            value = "&lt;&lt;extend&gt;&gt;";
        }

        xml += `        <mxCell id="${edgeId}" value="${value}" style="${style}" edge="1" parent="1" source="${rel.src}" target="${rel.tgt}">\n`;
        xml += `          <mxGeometry relative="1" as="geometry"/>\n`;
        xml += `        </mxCell>\n`;
    });

    xml += `      </root>\n`;
    xml += `    </mxGraphModel>\n`;
    xml += `  </diagram>\n`;
    xml += `</mxfile>`;

    fs.writeFileSync(path.join(outputDir, `${filename}.drawio`), xml, 'utf8');
    console.log(`✅ ${filename}.drawio`);
}

// ---------------------------------------------------------
// DỮ LIỆU ĐƯỢC MAPPING CHUẨN TỪ CODE (CONTROLLERS)
// ---------------------------------------------------------

// ================= ADMIN =================
const adminActor = [{ id: 'act', name: 'ADMIN\n(Quản trị viên)' }];
createUseCaseDiagram("01_xac_thuc", "Xác thực & Phiên làm việc", adminActor,
    [{ id: 'uc1', name: 'Đăng nhập', row: 0, col: 1 }, { id: 'uc2', name: 'Đăng xuất', row: 1, col: 1 }],
    [{ src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }], 'ADMIN');

createUseCaseDiagram("02_ho_so_ca_nhan", "Hồ sơ cá nhân", adminActor,
    [{ id: 'uc1', name: 'Xem thông tin cá nhân', row: 0, col: 1 }, { id: 'uc2', name: 'Đổi mật khẩu', row: 1, col: 1 }],
    [{ src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }], 'ADMIN');

createUseCaseDiagram("03_tong_quan", "Tổng quan", adminActor,
    [{ id: 'uc1', name: 'Xem thống kê tổng quan', row: 0, col: 1 }], [{ src: 'act', tgt: 'uc1' }], 'ADMIN');

createUseCaseDiagram("04_phieu_kho", "Quản lý Phiếu Kho", adminActor, [
    { id: 'uc1', name: 'Xem danh sách phiếu kho', row: 0, col: 1 },
    { id: 'uc2', name: 'Lập phiếu kho (Nhập/Xuất/Điều chuyển)', row: 1, col: 1 },
    { id: 'uc3', name: 'Phê duyệt phiếu', row: 2, col: 1 },
    { id: 'uc4', name: 'Hủy phiếu', row: 3, col: 1 },
    { id: 'uc5', name: 'In phiếu (PDF)', row: 4, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }, { src: 'act', tgt: 'uc3' },
    { src: 'act', tgt: 'uc4' }, { src: 'act', tgt: 'uc5' }
], 'ADMIN');

createUseCaseDiagram("05_san_pham", "Quản lý Sản phẩm", adminActor, [
    { id: 'uc1', name: 'Xem danh sách sản phẩm', row: 0, col: 1 },
    { id: 'uc2', name: 'Thêm sản phẩm mới', row: 1, col: 1 },
    { id: 'uc3', name: 'Cập nhật sản phẩm', row: 2, col: 1 },
    { id: 'uc4', name: 'Xóa sản phẩm', row: 3, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' },
    { src: 'act', tgt: 'uc3' }, { src: 'act', tgt: 'uc4' }
], 'ADMIN');

createUseCaseDiagram("06_danh_muc", "Quản lý Danh mục", adminActor, [
    { id: 'uc1', name: 'Xem danh sách danh mục', row: 0, col: 1 },
    { id: 'uc2', name: 'Thêm danh mục mới', row: 1, col: 1 },
    { id: 'uc3', name: 'Cập nhật danh mục', row: 2, col: 1 },
    { id: 'uc4', name: 'Xóa danh mục', row: 3, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' },
    { src: 'act', tgt: 'uc3' }, { src: 'act', tgt: 'uc4' }
], 'ADMIN');

createUseCaseDiagram("07_ton_kho", "Quản lý Tồn kho", adminActor, [
    { id: 'uc1', name: 'Xem danh sách tồn kho', row: 0, col: 1 },
    { id: 'uc2', name: 'Lọc tồn kho theo hạn sử dụng', row: 1, col: 1 },
    { id: 'uc3', name: 'Cập nhật định mức tồn', row: 2, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }, { src: 'act', tgt: 'uc3' }
], 'ADMIN');

createUseCaseDiagram("08_kiem_ke_kho", "Kiểm kê kho", adminActor, [
    { id: 'uc1', name: 'Xem danh sách phiên kiểm kê', row: 0, col: 1 },
    { id: 'uc2', name: 'Khởi tạo kiểm kê', row: 1, col: 1 },
    { id: 'uc3', name: 'Nhập số lượng kiểm đếm', row: 2, col: 1 },
    { id: 'uc4', name: 'Xác nhận hoàn tất', row: 3, col: 1 },
    { id: 'uc5', name: 'Tự động sinh phiếu điều chỉnh', row: 3, col: 2 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }, { src: 'act', tgt: 'uc3' },
    { src: 'act', tgt: 'uc4' }, { src: 'uc4', tgt: 'uc5', type: 'include' }
], 'ADMIN');

createUseCaseDiagram("09_doi_tac", "Quản lý Đối tác", adminActor, [
    { id: 'uc1', name: 'Xem danh sách đối tác', row: 0, col: 1 },
    { id: 'uc2', name: 'Thêm đối tác mới', row: 1, col: 1 },
    { id: 'uc3', name: 'Cập nhật đối tác', row: 2, col: 1 },
    { id: 'uc4', name: 'Vô hiệu hóa đối tác', row: 3, col: 1 },
    { id: 'uc5', name: 'Chặn thao tác nếu còn nợ', row: 3, col: 2 },
    { id: 'uc6', name: 'Xóa đối tác', row: 4, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }, { src: 'act', tgt: 'uc3' },
    { src: 'act', tgt: 'uc4' }, { src: 'uc4', tgt: 'uc5', type: 'include' }, { src: 'act', tgt: 'uc6' }
], 'ADMIN');

createUseCaseDiagram("10_nhan_vien", "Quản lý Nhân viên", adminActor, [
    { id: 'uc1', name: 'Xem danh sách người dùng', row: 0, col: 1 },
    { id: 'uc2', name: 'Thêm người dùng mới', row: 1, col: 1 },
    { id: 'uc3', name: 'Cập nhật người dùng', row: 2, col: 1 },
    { id: 'uc4', name: 'Khóa/Mở khóa tài khoản', row: 3, col: 1 },
    { id: 'uc5', name: 'Xóa người dùng', row: 4, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }, { src: 'act', tgt: 'uc3' },
    { src: 'act', tgt: 'uc4' }, { src: 'act', tgt: 'uc5' }
], 'ADMIN');

createUseCaseDiagram("11_chi_nhanh", "Quản lý Chi nhánh", adminActor, [
    { id: 'uc1', name: 'Xem danh sách chi nhánh', row: 0, col: 1 },
    { id: 'uc2', name: 'Thêm chi nhánh mới', row: 1, col: 1 },
    { id: 'uc3', name: 'Cập nhật chi nhánh', row: 2, col: 1 },
    { id: 'uc4', name: 'Xóa chi nhánh', row: 3, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' },
    { src: 'act', tgt: 'uc3' }, { src: 'act', tgt: 'uc4' }
], 'ADMIN');

createUseCaseDiagram("12_nhat_ky", "Nhật ký hoạt động", adminActor, [
    { id: 'uc1', name: 'Tra cứu Audit Log', row: 0, col: 1 },
], [{ src: 'act', tgt: 'uc1' }], 'ADMIN');

// ================= MANAGER =================
const managerActor = [{ id: 'act', name: 'MANAGER\n(Quản lý chi nhánh)' }];

createUseCaseDiagram("01_xac_thuc", "Xác thực & Phiên làm việc", managerActor,
    [{ id: 'uc1', name: 'Đăng nhập', row: 0, col: 1 }, { id: 'uc2', name: 'Đăng xuất', row: 1, col: 1 }],
    [{ src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }], 'MANAGER');

createUseCaseDiagram("02_ho_so_ca_nhan", "Hồ sơ cá nhân", managerActor,
    [{ id: 'uc1', name: 'Xem thông tin cá nhân', row: 0, col: 1 }, { id: 'uc2', name: 'Đổi mật khẩu', row: 1, col: 1 }],
    [{ src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }], 'MANAGER');

createUseCaseDiagram("03_tong_quan", "Tổng quan", managerActor,
    [{ id: 'uc1', name: 'Xem thống kê tổng quan', row: 0, col: 1 }], [{ src: 'act', tgt: 'uc1' }], 'MANAGER');

createUseCaseDiagram("04_phieu_kho", "Quản lý Phiếu Kho", managerActor, [
    { id: 'uc1', name: 'Xem danh sách phiếu kho', row: 0, col: 1 },
    { id: 'uc3', name: 'Phê duyệt phiếu', row: 1, col: 1 },
    { id: 'uc4', name: 'Hủy phiếu', row: 2, col: 1 },
    { id: 'uc5', name: 'In phiếu (PDF)', row: 3, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc3' },
    { src: 'act', tgt: 'uc4' }, { src: 'act', tgt: 'uc5' }
], 'MANAGER');

createUseCaseDiagram("05_san_pham", "Quản lý Sản phẩm", managerActor, [
    { id: 'uc1', name: 'Xem danh sách sản phẩm', row: 0, col: 1 },
    { id: 'uc2', name: 'Thêm sản phẩm mới', row: 1, col: 1 },
    { id: 'uc3', name: 'Cập nhật sản phẩm', row: 2, col: 1 },
    { id: 'uc4', name: 'Xóa sản phẩm', row: 3, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' },
    { src: 'act', tgt: 'uc3' }, { src: 'act', tgt: 'uc4' }
], 'MANAGER');

createUseCaseDiagram("06_danh_muc", "Quản lý Danh mục", managerActor, [
    { id: 'uc1', name: 'Xem danh sách danh mục', row: 0, col: 1 },
    { id: 'uc2', name: 'Thêm danh mục mới', row: 1, col: 1 },
    { id: 'uc3', name: 'Cập nhật danh mục', row: 2, col: 1 },
    { id: 'uc4', name: 'Xóa danh mục', row: 3, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' },
    { src: 'act', tgt: 'uc3' }, { src: 'act', tgt: 'uc4' }
], 'MANAGER');

createUseCaseDiagram("07_ton_kho", "Quản lý Tồn kho", managerActor, [
    { id: 'uc1', name: 'Xem danh sách tồn kho', row: 0, col: 1 },
    { id: 'uc2', name: 'Lọc tồn kho theo hạn sử dụng', row: 1, col: 1 },
    { id: 'uc3', name: 'Cập nhật định mức tồn', row: 2, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }, { src: 'act', tgt: 'uc3' }
], 'MANAGER');

createUseCaseDiagram("08_kiem_ke_kho", "Kiểm kê kho", managerActor, [
    { id: 'uc1', name: 'Xem danh sách phiên kiểm kê', row: 0, col: 1 },
    { id: 'uc2', name: 'Khởi tạo kiểm kê', row: 1, col: 1 },
    { id: 'uc3', name: 'Nhập số lượng kiểm đếm', row: 2, col: 1 },
    { id: 'uc4', name: 'Xác nhận hoàn tất', row: 3, col: 1 },
    { id: 'uc5', name: 'Tự động sinh phiếu điều chỉnh', row: 3, col: 2 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }, { src: 'act', tgt: 'uc3' },
    { src: 'act', tgt: 'uc4' }, { src: 'uc4', tgt: 'uc5', type: 'include' }
], 'MANAGER');

createUseCaseDiagram("09_doi_tac", "Quản lý Đối tác", managerActor, [
    { id: 'uc1', name: 'Xem danh sách đối tác', row: 0, col: 1 },
    { id: 'uc2', name: 'Thêm đối tác mới', row: 1, col: 1 },
    { id: 'uc3', name: 'Cập nhật đối tác', row: 2, col: 1 },
    { id: 'uc4', name: 'Vô hiệu hóa đối tác', row: 3, col: 1 },
    { id: 'uc5', name: 'Chặn thao tác nếu còn nợ', row: 3, col: 2 },
    { id: 'uc6', name: 'Xóa đối tác', row: 4, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }, { src: 'act', tgt: 'uc3' },
    { src: 'act', tgt: 'uc4' }, { src: 'uc4', tgt: 'uc5', type: 'include' }, { src: 'act', tgt: 'uc6' }
], 'MANAGER');

createUseCaseDiagram("10_nhan_vien", "Quản lý Nhân viên", managerActor, [
    { id: 'uc1', name: 'Xem danh sách nhân viên', row: 0, col: 1 },
    { id: 'uc2', name: 'Thêm nhân viên mới', row: 1, col: 1 },
    { id: 'uc3', name: 'Cập nhật nhân viên', row: 2, col: 1 },
    { id: 'uc4', name: 'Khóa/Mở khóa tài khoản', row: 3, col: 1 },
    { id: 'uc5', name: 'Xóa nhân viên', row: 4, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }, { src: 'act', tgt: 'uc3' },
    { src: 'act', tgt: 'uc4' }, { src: 'act', tgt: 'uc5' }
], 'MANAGER');

createUseCaseDiagram("11_nhat_ky", "Nhật ký hoạt động", managerActor, [
    { id: 'uc1', name: 'Tra cứu Audit Log', row: 0, col: 1 },
], [{ src: 'act', tgt: 'uc1' }], 'MANAGER');

// ================= STAFF =================
const staffActor = [{ id: 'act', name: 'STAFF\n(Nhân viên kho)' }];

createUseCaseDiagram("01_xac_thuc", "Xác thực & Phiên làm việc", staffActor,
    [{ id: 'uc1', name: 'Đăng nhập', row: 0, col: 1 }, { id: 'uc2', name: 'Đăng xuất', row: 1, col: 1 }],
    [{ src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }], 'STAFF');

createUseCaseDiagram("02_ho_so_ca_nhan", "Hồ sơ cá nhân", staffActor,
    [{ id: 'uc1', name: 'Xem thông tin cá nhân', row: 0, col: 1 }, { id: 'uc2', name: 'Đổi mật khẩu', row: 1, col: 1 }],
    [{ src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }], 'STAFF');

createUseCaseDiagram("03_tong_quan", "Tổng quan", staffActor,
    [{ id: 'uc1', name: 'Xem thống kê tổng quan', row: 0, col: 1 }], [{ src: 'act', tgt: 'uc1' }], 'STAFF');

createUseCaseDiagram("04_phieu_kho", "Quản lý Phiếu Kho", staffActor, [
    { id: 'uc1', name: 'Xem danh sách phiếu kho', row: 0, col: 1 },
    { id: 'uc2', name: 'Lập phiếu kho nháp', row: 1, col: 1 },
    { id: 'uc3', name: 'Xác nhận kiểm kê phiếu', row: 2, col: 1 },
    { id: 'uc4', name: 'In phiếu (PDF)', row: 3, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' },
    { src: 'act', tgt: 'uc3' }, { src: 'act', tgt: 'uc4' }
], 'STAFF');

createUseCaseDiagram("05_ton_kho", "Quản lý Tồn kho", staffActor, [
    { id: 'uc1', name: 'Xem danh sách tồn kho', row: 0, col: 1 },
    { id: 'uc2', name: 'Lọc tồn kho theo hạn sử dụng', row: 1, col: 1 },
    { id: 'uc3', name: 'Cập nhật định mức tồn', row: 2, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }, { src: 'act', tgt: 'uc3' }
], 'STAFF');

createUseCaseDiagram("06_kiem_ke_kho", "Kiểm kê kho", staffActor, [
    { id: 'uc1', name: 'Xem danh sách phiên kiểm kê', row: 0, col: 1 },
    { id: 'uc2', name: 'Khởi tạo kiểm kê', row: 1, col: 1 },
    { id: 'uc3', name: 'Nhập số lượng kiểm đếm', row: 2, col: 1 },
], [
    { src: 'act', tgt: 'uc1' }, { src: 'act', tgt: 'uc2' }, { src: 'act', tgt: 'uc3' }
], 'STAFF');

createUseCaseDiagram("07_doi_tac", "Xem Đối tác", staffActor, [
    { id: 'uc1', name: 'Xem danh sách đối tác', row: 0, col: 1 },
], [{ src: 'act', tgt: 'uc1' }], 'STAFF');

// ================= TỔNG QUÁT =================
const allActors = [
    { id: 'act_admin', name: 'ADMIN\\n(Quản trị viên)', x: 50, y: 150 },
    { id: 'act_manager', name: 'MANAGER\\n(Quản lý chi nhánh)', x: 50, y: 350 },
    { id: 'act_staff', name: 'STAFF\\n(Nhân viên kho)', x: 50, y: 550 }
];

const generalUseCases = [
    { id: 'uc1', name: 'Xác thực & Phiên làm việc', row: 0, col: 1 },
    { id: 'uc2', name: 'Hồ sơ cá nhân', row: 0, col: 2 },
    { id: 'uc3', name: 'Tổng quan', row: 1, col: 1 },
    { id: 'uc4', name: 'Quản lý Phiếu Kho', row: 1, col: 2 },
    { id: 'uc5', name: 'Quản lý Sản phẩm', row: 2, col: 1 },
    { id: 'uc6', name: 'Quản lý Danh mục', row: 2, col: 2 },
    { id: 'uc7', name: 'Quản lý Tồn kho', row: 3, col: 1 },
    { id: 'uc8', name: 'Kiểm kê kho', row: 3, col: 2 },
    { id: 'uc9', name: 'Quản lý Đối tác', row: 4, col: 1 },
    { id: 'uc10', name: 'Quản lý Nhân viên', row: 4, col: 2 },
    { id: 'uc11', name: 'Quản lý Chi nhánh', row: 5, col: 1 },
    { id: 'uc12', name: 'Nhật ký hoạt động', row: 5, col: 2 }
];

const generalRelations = [
    // ADMIN connects to all
    { src: 'act_admin', tgt: 'uc1' }, { src: 'act_admin', tgt: 'uc2' },
    { src: 'act_admin', tgt: 'uc3' }, { src: 'act_admin', tgt: 'uc4' },
    { src: 'act_admin', tgt: 'uc5' }, { src: 'act_admin', tgt: 'uc6' },
    { src: 'act_admin', tgt: 'uc7' }, { src: 'act_admin', tgt: 'uc8' },
    { src: 'act_admin', tgt: 'uc9' }, { src: 'act_admin', tgt: 'uc10' },
    { src: 'act_admin', tgt: 'uc11' }, { src: 'act_admin', tgt: 'uc12' },

    // MANAGER connects to 11 (except Chi nhánh)
    { src: 'act_manager', tgt: 'uc1' }, { src: 'act_manager', tgt: 'uc2' },
    { src: 'act_manager', tgt: 'uc3' }, { src: 'act_manager', tgt: 'uc4' },
    { src: 'act_manager', tgt: 'uc5' }, { src: 'act_manager', tgt: 'uc6' },
    { src: 'act_manager', tgt: 'uc7' }, { src: 'act_manager', tgt: 'uc8' },
    { src: 'act_manager', tgt: 'uc9' }, { src: 'act_manager', tgt: 'uc10' },
    { src: 'act_manager', tgt: 'uc12' },

    // STAFF connects to 7
    { src: 'act_staff', tgt: 'uc1' }, { src: 'act_staff', tgt: 'uc2' },
    { src: 'act_staff', tgt: 'uc3' }, { src: 'act_staff', tgt: 'uc4' },
    { src: 'act_staff', tgt: 'uc7' }, { src: 'act_staff', tgt: 'uc8' },
    { src: 'act_staff', tgt: 'uc9' }
];

createUseCaseDiagram("use_case_tong_quat", "Biểu đồ Use Case Tổng Quát", allActors, generalUseCases, generalRelations, ".");

console.log("✅ Hoàn thành xuất file drawio cho ADMIN, MANAGER, STAFF và Use Case Tổng quát.");
