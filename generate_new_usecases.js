const fs = require('fs');
const path = require('path');

function createUsecaseDiagram(filename, title, actors, usecases, relations) {
    let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
    xml += `<mxfile host="Electron" type="device">\n`;
    const escapedTitle = title.replace(/&/g, '&amp;');
    xml += `  <diagram id="${filename}-id" name="${escapedTitle}">\n`;
    xml += `    <mxGraphModel dx="1000" dy="1000" grid="1" gridSize="10" connect="1" arrows="1">\n`;
    xml += `      <root>\n`;
    xml += `        <mxCell id="0"/>\n`;
    xml += `        <mxCell id="1" parent="0"/>\n`;

    const boundaryYStart = 50;
    const boundaryHeight = usecases.length * 100 + 50;
    
    // System Boundary
    xml += `        <mxCell id="system_boundary" value="${escapedTitle}" style="swimlane;whiteSpace=wrap;html=1;startSize=30;fillColor=#f8f9fa;strokeColor=#cccccc;fontStyle=1;align=center;" vertex="1" parent="1">\n`;
    xml += `          <mxGeometry x="260" y="${boundaryYStart}" width="400" height="${boundaryHeight}" as="geometry"/>\n`;
    xml += `        </mxCell>\n`;

    // Actors
    const actorStyle = "shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;fillColor=#e3f2fd;strokeColor=#1e88e5;strokeWidth=2;";
    actors.forEach((act, i) => {
        const yPos = boundaryYStart + 50 + (i * 150);
        xml += `        <mxCell id="${act.id}" value="${act.name.replace(/&/g, '&amp;')}" style="${actorStyle}" vertex="1" parent="1">\n`;
        xml += `          <mxGeometry x="80" y="${yPos}" width="40" height="80" as="geometry"/>\n`;
        xml += `        </mxCell>\n`;
    });

    // Use cases
    const ucStyle = "ellipse;whiteSpace=wrap;html=1;fillColor=#fffdf0;strokeColor=#fb8c00;strokeWidth=2;fontColor=#000000;align=center;";
    usecases.forEach((uc, i) => {
        const yPos = boundaryYStart + 40 + (i * 100); // absolute position on parent="1"
        xml += `        <mxCell id="${uc.id}" value="${uc.name.replace(/&/g, '&amp;')}" style="${ucStyle}" vertex="1" parent="1">\n`;
        xml += `          <mxGeometry x="360" y="${yPos}" width="200" height="60" as="geometry"/>\n`;
        xml += `        </mxCell>\n`;
    });

    // Relations
    relations.forEach((rel, idx) => {
        const edgeId = `edge_${idx}`;
        let style = "endArrow=none;html=1;rounded=0;strokeColor=#000000;strokeWidth=1.5;";
        let label = "";
        
        if (rel.type === "include" || rel.type === "extend") {
            style = "endArrow=open;endSize=12;dashed=1;html=1;rounded=0;strokeColor=#000000;strokeWidth=1.5;";
            label = `&lt;&lt;${rel.type}&gt;&gt;`;
        }
        
        xml += `        <mxCell id="${edgeId}" value="${label}" style="${style}" edge="1" parent="1" source="${rel.src}" target="${rel.tgt}">\n`;
        xml += `          <mxGeometry relative="1" as="geometry"/>\n`;
        xml += `        </mxCell>\n`;
    });

    xml += `      </root>\n`;
    xml += `    </mxGraphModel>\n`;
    xml += `  </diagram>\n`;
    xml += `</mxfile>`;

    const outputDir = path.join(__dirname, 'UseCase', 'Usecase_PhanRa');
    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
    }
    
    const outputPath = path.join(outputDir, `${filename}.drawio`);
    fs.writeFileSync(outputPath, xml, 'utf8');
    console.log(`Đã tạo UseCase: ${outputPath} thành công!`);
}

const actorsList = [
    {id: "act_admin", name: "Quản trị viên (Admin)"},
    {id: "act_manager", name: "Quản lý (Manager)"},
    {id: "act_staff", name: "Nhân viên (Staff)"}
];

// 1. Nhập Kho
const ucNhapKho = [
    {id: "uc_view_nk", name: "Xem danh sách phiếu Nhập kho"},
    {id: "uc_create_nk", name: "Lập phiếu Nhập kho (Draft)"},
    {id: "uc_approve_nk", name: "Phê duyệt phiếu Nhập kho"}
];
const relNhapKho = [
    {src: "act_admin", tgt: "uc_view_nk"},
    {src: "act_manager", tgt: "uc_view_nk"},
    {src: "act_staff", tgt: "uc_view_nk"},
    {src: "act_staff", tgt: "uc_create_nk"},
    {src: "act_manager", tgt: "uc_approve_nk"}
];
createUsecaseDiagram("usecase_nhap_kho", "Use Case: Nghiệp vụ Nhập Kho", actorsList, ucNhapKho, relNhapKho);

// 2. Hóa đơn (Xuất kho)
const ucHoaDon = [
    {id: "uc_view_hd", name: "Xem danh sách Hóa đơn"},
    {id: "uc_create_hd", name: "Lập Hóa đơn xuất hàng (Draft)"},
    {id: "uc_approve_hd", name: "Phê duyệt Hóa đơn"}
];
const relHoaDon = [
    {src: "act_admin", tgt: "uc_view_hd"},
    {src: "act_manager", tgt: "uc_view_hd"},
    {src: "act_staff", tgt: "uc_view_hd"},
    {src: "act_staff", tgt: "uc_create_hd"},
    {src: "act_manager", tgt: "uc_approve_hd"}
];
createUsecaseDiagram("usecase_hoa_don", "Use Case: Nghiệp vụ Hóa Đơn (Xuất kho)", actorsList, ucHoaDon, relHoaDon);

// 3. Điều chuyển
const ucDieuChuyen = [
    {id: "uc_view_dc", name: "Xem danh sách Điều chuyển"},
    {id: "uc_create_dc", name: "Lập phiếu Điều chuyển đi"},
    {id: "uc_approve_mgr", name: "Phê duyệt Điều chuyển đi (Manager)"},
    {id: "uc_approve_admin", name: "Duyệt Điều chuyển liên chi nhánh (Admin)"},
    {id: "uc_confirm", name: "Xác nhận nhận hàng tại kho đích"}
];
const relDieuChuyen = [
    {src: "act_admin", tgt: "uc_view_dc"},
    {src: "act_manager", tgt: "uc_view_dc"},
    {src: "act_staff", tgt: "uc_view_dc"},
    {src: "act_staff", tgt: "uc_create_dc"},
    {src: "act_manager", tgt: "uc_approve_mgr"},
    {src: "act_admin", "tgt": "uc_approve_admin"},
    {src: "act_manager", tgt: "uc_confirm"},
    {src: "act_staff", "tgt": "uc_confirm"}
];
createUsecaseDiagram("usecase_dieu_chuyen", "Use Case: Nghiệp vụ Điều chuyển", actorsList, ucDieuChuyen, relDieuChuyen);

// 4. Tiêu hủy
const ucTieuHuy = [
    {id: "uc_view_th", name: "Xem danh sách phiếu Tiêu hủy"},
    {id: "uc_create_th", name: "Đề xuất Tiêu hủy (Lập phiếu nháp)"},
    {id: "uc_approve_mgr_th", "name": "Xác nhận Tiêu hủy (Manager)"},
    {id: "uc_approve_admin_th", "name": "Phê duyệt Tiêu hủy cuối cùng (Admin)"}
];
const relTieuHuy = [
    {src: "act_admin", tgt: "uc_view_th"},
    {src: "act_manager", tgt: "uc_view_th"},
    {src: "act_staff", tgt: "uc_view_th"},
    {src: "act_staff", tgt: "uc_create_th"},
    {src: "act_manager", tgt: "uc_approve_mgr_th"},
    {src: "act_admin", "tgt": "uc_approve_admin_th"}
];
createUsecaseDiagram("usecase_tieu_huy", "Use Case: Nghiệp vụ Tiêu hủy", actorsList, ucTieuHuy, relTieuHuy);
