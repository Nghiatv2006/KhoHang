const fs = require('fs');

const actors = {
    ADMIN: { id: 'act_admin', name: 'Quản trị viên\n(Admin)', x: 50, y: 350 },
    MANAGER: { id: 'act_manager', name: 'Quản lý chi nhánh\n(Manager)', x: 850, y: 150 },
    STAFF: { id: 'act_staff', name: 'Nhân viên kho\n(Staff)', x: 850, y: 550 }
};

const useCases = [
    { name: 'Đăng nhập và Xác thực', roles: ['ADMIN', 'MANAGER', 'STAFF'] },
    { name: 'Tài khoản cá nhân', roles: ['ADMIN', 'MANAGER', 'STAFF'] },
    { name: 'Tổng quan (Dashboard)', roles: ['ADMIN', 'MANAGER', 'STAFF'] },
    { name: 'Quản lý Nhân viên', roles: ['ADMIN', 'MANAGER'] },
    { name: 'Quản lý Chi nhánh', roles: ['ADMIN'] },
    { name: 'Quản lý Danh mục', roles: ['ADMIN'] },
    { name: 'Quản lý Sản phẩm', roles: ['ADMIN'] },
    { name: 'Quản lý Đối tác', roles: ['ADMIN', 'MANAGER'] },
    { name: 'Cập nhật Công nợ', roles: ['ADMIN', 'MANAGER'] },
    { name: 'Lịch sử giao dịch', roles: ['ADMIN', 'MANAGER', 'STAFF'] },
    { name: 'Quản lý Tồn kho', roles: ['ADMIN', 'MANAGER', 'STAFF'] },
    { name: 'Kiểm kê kho', roles: ['ADMIN', 'MANAGER', 'STAFF'] },
    { name: 'Quản lý Nhập kho', roles: ['ADMIN', 'MANAGER', 'STAFF'] },
    { name: 'Quản lý Hóa đơn (Xuất)', roles: ['ADMIN', 'MANAGER', 'STAFF'] },
    { name: 'Quản lý Điều chuyển', roles: ['ADMIN', 'MANAGER', 'STAFF'] },
    { name: 'Quản lý Tiêu hủy', roles: ['ADMIN', 'MANAGER', 'STAFF'] },
    { name: 'Nhập/Xuất Excel', roles: ['ADMIN', 'MANAGER', 'STAFF'] },
    { name: 'Sao lưu/Phục hồi Dữ liệu', roles: ['ADMIN'] },
    { name: 'Tra cứu Audit Log', roles: ['ADMIN'] }
];

let xml = `<?xml version="1.0" encoding="UTF-8"?>
<mxfile host="Electron" modified="2023-10-01T12:00:00.000Z" agent="Mozilla/5.0" version="21.6.8" type="device">
  <diagram id="diagram_tong_quat" name="Use Case Tổng Quát">
    <mxGraphModel dx="1200" dy="800" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1100" pageHeight="1400" math="0" shadow="0">
      <root>
        <mxCell id="0" />
        <mxCell id="1" parent="0" />
`;

for (const [key, actor] of Object.entries(actors)) {
    xml += `
        <mxCell id="${actor.id}" value="${actor.name}" style="shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;fillColor=#dae8fc;strokeColor=#6c8ebf;" vertex="1" parent="1">
          <mxGeometry x="${actor.x}" y="${actor.y}" width="30" height="60" as="geometry" />
        </mxCell>`;
}

const boxY = 20;
const boxWidth = 460;
const rowCount = Math.ceil(useCases.length / 2);
const boxHeight = rowCount * 60 + 80;

xml += `
        <mxCell id="sys_box" value="WareHub System" style="shape=rect;html=1;verticalAlign=top;fontStyle=1;fillColor=none;" vertex="1" parent="1">
          <mxGeometry x="250" y="${boxY}" width="${boxWidth}" height="${boxHeight}" as="geometry" />
        </mxCell>`;

let ucId = 100;
let connId = 1000;

useCases.forEach((uc, index) => {
    let col = index % 2;
    let row = Math.floor(index / 2);
    
    let relX = col === 0 ? 30 : 250;
    let relY = row * 60 + 60;
    
    xml += `
        <mxCell id="uc_${ucId}" value="${uc.name}" style="ellipse;whiteSpace=wrap;html=1;fillColor=#fff2cc;strokeColor=#d6b656;" vertex="1" parent="sys_box">
          <mxGeometry x="${relX}" y="${relY}" width="180" height="40" as="geometry" />
        </mxCell>`;
    
    uc.roles.forEach(role => {
        let actorId = actors[role].id;
        xml += `
        <mxCell id="conn_${connId++}" value="" style="endArrow=none;html=1;rounded=0;entryX=${col === 0 ? 0 : 1};entryY=0.5;entryDx=0;entryDy=0;" edge="1" parent="1" source="${actorId}" target="uc_${ucId}">
          <mxGeometry width="50" height="50" relative="1" as="geometry" />
        </mxCell>`;
    });
    
    ucId++;
});

xml += `
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>`;

fs.writeFileSync('c:/Users/MKC/Documents/New folder (16)/UseCase/use_case_tong_quat.drawio', xml);
console.log('Done generating use_case_tong_quat.drawio');
