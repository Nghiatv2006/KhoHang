const fs = require('fs');
const path = require('path');

function createUseCaseDiagram(filename, diagramName, actors, usecases, relations, outputDir = '.') {
    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
    }

    let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
    xml += `<mxfile host="Electron" modified="2024-01-01T00:00:00.000Z" agent="JS Script" type="device">\n`;
    const escapedDiagramName = diagramName.replace(/&/g, '&amp;');
    xml += `  <diagram id="${escapedDiagramName}" name="${escapedDiagramName}">\n`;
    xml += `    <mxGraphModel dx="1000" dy="1000" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1">\n`;
    xml += `      <root>\n`;
    xml += `        <mxCell id="0"/>\n`;
    xml += `        <mxCell id="1" parent="0"/>\n`;

    // Actors
    actors.forEach(act => {
        const x = act.x || 50;
        const y = act.y || 200;
        const escapedName = act.name.replace(/\\n/g, '&#xa;').replace(/\n/g, '&#xa;').replace(/&/g, '&amp;');
        xml += `        <mxCell id="${act.id}" value="${escapedName}" style="shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;" vertex="1" parent="1">\n`;
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
        const escapedName = uc.name.replace(/\\n/g, '&#xa;').replace(/\n/g, '&#xa;').replace(/&/g, '&amp;');
        xml += `        <mxCell id="${uc.id}" value="${escapedName}" style="ellipse;whiteSpace=wrap;html=1;" vertex="1" parent="${sysId}">\n`;
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


// ================= TỔNG QUÁT =================
const allActors = [
    { id: 'act_admin', name: 'ADMIN\\n(Quản trị viên)', x: 50, y: 250 },
    { id: 'act_manager', name: 'MANAGER\\n(Quản lý chi nhánh)', x: 50, y: 550 },
    { id: 'act_staff', name: 'STAFF\\n(Nhân viên kho)', x: 50, y: 850 }
];

const generalUseCases = [
    { id: 'uc1', name: 'Xác thực & Phiên làm việc', row: 0, col: 1 },
    { id: 'uc2', name: 'Hồ sơ cá nhân', row: 1, col: 1 },
    { id: 'uc3', name: 'Tổng quan', row: 2, col: 1 },
    { id: 'uc4', name: 'Quản lý Phiếu Kho', row: 3, col: 1 },
    { id: 'uc5', name: 'Quản lý Sản phẩm', row: 4, col: 1 },
    { id: 'uc6', name: 'Quản lý Danh mục', row: 5, col: 1 },
    { id: 'uc7', name: 'Quản lý Tồn kho', row: 6, col: 1 },
    { id: 'uc8', name: 'Kiểm kê kho', row: 7, col: 1 },
    { id: 'uc9', name: 'Quản lý Đối tác', row: 8, col: 1 },
    { id: 'uc10', name: 'Quản lý Nhân viên', row: 9, col: 1 },
    { id: 'uc11', name: 'Quản lý Chi nhánh', row: 10, col: 1 },
    { id: 'uc12', name: 'Nhật ký hoạt động', row: 11, col: 1 }
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
