const fs = require('fs');
const path = require('path');

function createUsecaseDiagram(folder, filename, title, actors, usecases, relations) {
    let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
    xml += `<mxfile host="Electron" type="device">\n`;
    const escapedTitle = title.replace(/&/g, '&amp;');
    xml += `  <diagram id="${filename}-id" name="${escapedTitle}">\n`;
    xml += `    <mxGraphModel dx="1200" dy="800" grid="1" gridSize="10" connect="1" arrows="1">\n`;
    xml += `      <root>\n`;
    xml += `        <mxCell id="0"/>\n`;
    xml += `        <mxCell id="1" parent="0"/>\n`;

    const boundaryYStart = 50;
    
    let maxRow = 0;
    usecases.forEach(uc => {
        if (uc.row > maxRow) maxRow = uc.row;
    });
    
    const boundaryHeight = Math.max(300, (maxRow + 1) * 110 + 50);
    
    // System Boundary
    xml += `        <mxCell id="system_boundary" value="${escapedTitle}" style="swimlane;whiteSpace=wrap;html=1;startSize=30;fillColor=#f8f9fa;strokeColor=#cccccc;fontStyle=1;align=center;" vertex="1" parent="1">\n`;
    xml += `          <mxGeometry x="220" y="${boundaryYStart}" width="650" height="${boundaryHeight}" as="geometry"/>\n`;
    xml += `        </mxCell>\n`;

    // Actors
    const actorStyle = "shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;fillColor=#dae8fc;strokeColor=#6c8ebf;strokeWidth=2;fontColor=#1a1a1a;fontStyle=1;fontSize=11;";
    actors.forEach((act, i) => {
        const yPos = boundaryYStart + 50 + (i * 150);
        xml += `        <mxCell id="${act.id}" value="${act.name.replace(/&/g, '&amp;')}" style="${actorStyle}" vertex="1" parent="1">\n`;
        xml += `          <mxGeometry x="80" y="${yPos}" width="50" height="90" as="geometry"/>\n`;
        xml += `        </mxCell>\n`;
    });

    // Use cases
    const ucStyle = "ellipse;whiteSpace=wrap;html=1;fillColor=#fffde7;strokeColor=#fb8c00;strokeWidth=2;fontStyle=1;fontSize=11;align=center;";
    usecases.forEach((uc) => {
        const xPos = 260 + (uc.col * 300); // Tăng khoảng cách cột để mũi tên dài hơn
        const yPos = boundaryYStart + 50 + (uc.row * 100); 
        xml += `        <mxCell id="${uc.id}" value="${uc.name.replace(/&/g, '&amp;')}" style="${ucStyle}" vertex="1" parent="1">\n`;
        xml += `          <mxGeometry x="${xPos}" y="${yPos}" width="185" height="60" as="geometry"/>\n`;
        xml += `        </mxCell>\n`;
    });

    // Relations
    relations.forEach((rel, idx) => {
        const edgeId = `edge_${idx}`;
        let style = "endArrow=none;html=1;rounded=0;strokeColor=#4a90d9;strokeWidth=1.5;";
        let label = "";
        
        if (rel.type === "include" || rel.type === "extend") {
            style = "endArrow=open;endSize=12;dashed=1;html=1;rounded=0;strokeColor=#555555;strokeWidth=1.5;labelBackgroundColor=#ffffff;fontColor=#333333;fontSize=10;align=center;";
            label = `&amp;lt;&amp;lt;${rel.type}&amp;gt;&amp;gt;`;
        }
        
        xml += `        <mxCell id="${edgeId}" value="${label}" style="${style}" edge="1" parent="1" source="${rel.src}" target="${rel.tgt}">\n`;
        xml += `          <mxGeometry relative="1" as="geometry"/>\n`;
        xml += `        </mxCell>\n`;
    });

    xml += `      </root>\n`;
    xml += `    </mxGraphModel>\n`;
    xml += `  </diagram>\n`;
    xml += `</mxfile>`;

    const outputDir = path.join(__dirname, 'UseCase', folder);
    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
    }
    
    const outputPath = path.join(outputDir, `${filename}.drawio`);
    fs.writeFileSync(outputPath, xml, 'utf8');
    console.log(`Đã tạo UseCase: ${outputPath} thành công!`);
}

const diagrams = [
  // ================= ADMIN =================
  {
    role: "ADMIN",
    filename: "20_nhap_kho",
    title: "Use Case: Nhập kho (Admin)",
    actors: [ { id: "act_admin", name: "Admin" } ],
    usecases: [
      { id: "uc_view", name: "Xem danh sách Phiếu Nhập", row: 0, col: 0 },
      { id: "uc_duyet_1", name: "Phê duyệt phiếu Nhập lên Kiểm kê", row: 1, col: 0 },
      { id: "uc_duyet_2", name: "Phê duyệt Hao hụt (Bước cuối)", row: 2, col: 0 },
      { id: "uc_huy", name: "Hủy phiếu (trừ phiếu Nháp)", row: 3, col: 0 },
      { id: "uc_tru_ton", name: "Trừ tồn kho nguồn (kho tổng)", row: 1, col: 1 }
    ],
    relations: [
      { src: "act_admin", tgt: "uc_view" },
      { src: "act_admin", tgt: "uc_duyet_1" },
      { src: "act_admin", tgt: "uc_duyet_2" },
      { src: "act_admin", tgt: "uc_huy" },
      { src: "uc_duyet_1", tgt: "uc_tru_ton", type: "include" }
    ]
  },
  {
    role: "ADMIN",
    filename: "21_hoa_don",
    title: "Use Case: Hóa đơn (Admin)",
    actors: [ { id: "act_admin", name: "Admin" } ],
    usecases: [
      { id: "uc_view", name: "Xem Hóa đơn (Chi nhánh tổng)", row: 0, col: 0 },
      { id: "uc_duyet", name: "Phê duyệt Hóa đơn", row: 1, col: 0 },
      { id: "uc_tru_ton", name: "Hệ thống tự động trừ tồn kho", row: 1, col: 1 },
      { id: "uc_huy", name: "Hủy Hóa đơn (đã xử lý)", row: 2, col: 0 }
    ],
    relations: [
      { src: "act_admin", tgt: "uc_view" },
      { src: "act_admin", tgt: "uc_duyet" },
      { src: "uc_duyet", tgt: "uc_tru_ton", type: "include" },
      { src: "act_admin", tgt: "uc_huy" }
    ]
  },
  {
    role: "ADMIN",
    filename: "22_dieu_chuyen",
    title: "Use Case: Điều chuyển (Admin)",
    actors: [ { id: "act_admin", name: "Admin" } ],
    usecases: [
      { id: "uc_view", name: "Xem toàn bộ Điều chuyển", row: 0, col: 0 },
      { id: "uc_huy", name: "Hủy phiếu Điều chuyển (đã xử lý)", row: 1, col: 0 },
      { id: "uc_bao_cao", name: "Xuất báo cáo Điều chuyển", row: 0, col: 1 }
    ],
    relations: [
      { src: "act_admin", tgt: "uc_view" },
      { src: "act_admin", tgt: "uc_huy" },
      { src: "uc_bao_cao", tgt: "uc_view", type: "extend" }
    ]
  },
  {
    role: "ADMIN",
    filename: "23_tieu_huy",
    title: "Use Case: Tiêu hủy (Admin)",
    actors: [ { id: "act_admin", name: "Admin" } ],
    usecases: [
      { id: "uc_view", name: "Xem phiếu Tiêu hủy", row: 0, col: 0 },
      { id: "uc_duyet_cuoi", name: "Phê duyệt Tiêu hủy cuối cùng", row: 1, col: 0 },
      { id: "uc_tru_ton", name: "Trừ tồn kho vĩnh viễn", row: 1, col: 1 },
      { id: "uc_huy", name: "Hủy phiếu Tiêu hủy (đã xử lý)", row: 2, col: 0 }
    ],
    relations: [
      { src: "act_admin", tgt: "uc_view" },
      { src: "act_admin", tgt: "uc_duyet_cuoi" },
      { src: "uc_duyet_cuoi", tgt: "uc_tru_ton", type: "include" },
      { src: "act_admin", tgt: "uc_huy" }
    ]
  },

  // ================= MANAGER =================
  {
    role: "MANAGER",
    filename: "20_nhap_kho",
    title: "Use Case: Nhập kho (Manager)",
    actors: [ { id: "act_manager", name: "Manager" } ],
    usecases: [
      { id: "uc_view", name: "Xem danh sách Phiếu Nhập", row: 0, col: 0 },
      { id: "uc_lap", name: "Lập phiếu Nhập (Draft)", row: 1, col: 0 },
      { id: "uc_duyet_1", name: "Duyệt yêu cầu Nhập kho", row: 2, col: 0 },
      { id: "uc_nhan_hang", name: "Xác nhận Kiểm kê", row: 3, col: 0 },
      { id: "uc_duyet_hao_hut", name: "Phê duyệt Hao hụt lên Admin", row: 4, col: 0 },
      { id: "uc_huy_nhap", name: "Xóa phiếu Nháp (tự lập)", row: 1, col: 1 },
      { id: "uc_huy", name: "Hủy phiếu đã xử lý", row: 2, col: 1 }
    ],
    relations: [
      { src: "act_manager", tgt: "uc_view" },
      { src: "act_manager", tgt: "uc_lap" },
      { src: "act_manager", tgt: "uc_duyet_1" },
      { src: "act_manager", tgt: "uc_nhan_hang" },
      { src: "act_manager", tgt: "uc_duyet_hao_hut" },
      { src: "uc_huy", tgt: "uc_duyet_1", type: "extend" },
      { src: "act_manager", tgt: "uc_huy_nhap" }
    ]
  },
  {
    role: "MANAGER",
    filename: "21_hoa_don",
    title: "Use Case: Hóa đơn (Manager)",
    actors: [ { id: "act_manager", name: "Manager" } ],
    usecases: [
      { id: "uc_view", name: "Xem danh sách Hóa đơn", row: 0, col: 0 },
      { id: "uc_lap", name: "Lập Hóa đơn", row: 1, col: 0 },
      { id: "uc_duyet", name: "Phê duyệt Hóa đơn", row: 2, col: 0 },
      { id: "uc_huy", name: "Hủy Hóa đơn đã xử lý", row: 3, col: 0 },
      { id: "uc_tru_ton", name: "Hệ thống tự động trừ tồn kho", row: 2, col: 1 },
      { id: "uc_cong_no", name: "Tự động cộng công nợ", row: 3, col: 1 },
      { id: "uc_huy_nhap", name: "Xóa phiếu Nháp (tự lập)", row: 1, col: 1 }
    ],
    relations: [
      { src: "act_manager", tgt: "uc_view" },
      { src: "act_manager", tgt: "uc_lap" },
      { src: "act_manager", tgt: "uc_duyet" },
      { src: "act_manager", tgt: "uc_huy" },
      { src: "uc_duyet", tgt: "uc_tru_ton", type: "include" },
      { src: "uc_duyet", tgt: "uc_cong_no", type: "include" },
      { src: "act_manager", tgt: "uc_huy_nhap" }
    ]
  },
  {
    role: "MANAGER",
    filename: "22_dieu_chuyen",
    title: "Use Case: Điều chuyển (Manager)",
    actors: [ { id: "act_manager", name: "Manager" } ],
    usecases: [
      { id: "uc_view", name: "Xem phiếu Điều chuyển", row: 0, col: 0 },
      { id: "uc_lap_dc", name: "Lập phiếu Yêu cầu xin hàng", row: 1, col: 0 },
      { id: "uc_duyet_xin", name: "Duyệt yêu cầu Xin hàng (Kho Đích)", row: 2, col: 0 },
      { id: "uc_duyet_xuat", name: "Duyệt Xuất kho (Kho Nguồn)", row: 3, col: 0 },
      { id: "uc_nhan_hang", name: "Xác nhận Kiểm kê nhận hàng", row: 4, col: 0 },
      { id: "uc_tru_ton", name: "Trừ tồn kho chi nhánh nguồn", row: 3, col: 1 },
      { id: "uc_huy", name: "Hủy phiếu đã xử lý", row: 2, col: 1 },
      { id: "uc_huy_nhap", name: "Xóa phiếu Nháp (tự lập)", row: 1, col: 1 }
    ],
    relations: [
      { src: "act_manager", tgt: "uc_view" },
      { src: "act_manager", tgt: "uc_lap_dc" },
      { src: "act_manager", tgt: "uc_duyet_xin" },
      { src: "act_manager", tgt: "uc_duyet_xuat" },
      { src: "act_manager", tgt: "uc_nhan_hang" },
      { src: "uc_duyet_xuat", tgt: "uc_tru_ton", type: "include" },
      { src: "uc_huy", tgt: "uc_duyet_xin", type: "extend" },
      { src: "act_manager", tgt: "uc_huy_nhap" }
    ]
  },
  {
    role: "MANAGER",
    filename: "23_tieu_huy",
    title: "Use Case: Tiêu hủy (Manager)",
    actors: [ { id: "act_manager", name: "Manager" } ],
    usecases: [
      { id: "uc_view", name: "Xem danh sách Tiêu hủy", row: 0, col: 0 },
      { id: "uc_de_xuat", name: "Đề xuất Tiêu hủy", row: 1, col: 0 },
      { id: "uc_xac_nhan", name: "Xác nhận Đề xuất Tiêu hủy", row: 2, col: 0 },
      { id: "uc_kiem_tra", name: "Đánh giá tình trạng hỏng hóc", row: 2, col: 1 },
      { id: "uc_huy", name: "Hủy đề xuất (đã xử lý)", row: 3, col: 0 },
      { id: "uc_huy_nhap", name: "Xóa phiếu Nháp (tự lập)", row: 1, col: 1 }
    ],
    relations: [
      { src: "act_manager", tgt: "uc_view" },
      { src: "act_manager", tgt: "uc_de_xuat" },
      { src: "act_manager", tgt: "uc_xac_nhan" },
      { src: "uc_xac_nhan", tgt: "uc_kiem_tra", type: "include" },
      { src: "act_manager", tgt: "uc_huy" },
      { src: "act_manager", tgt: "uc_huy_nhap" }
    ]
  },

  // ================= STAFF =================
  {
    role: "STAFF",
    filename: "20_nhap_kho",
    title: "Use Case: Nhập kho (Staff)",
    actors: [ { id: "act_staff", name: "Staff" } ],
    usecases: [
      { id: "uc_view", name: "Xem phiếu Nhập kho", row: 0, col: 0 },
      { id: "uc_lap_phieu", name: "Lập phiếu Nhập kho", row: 1, col: 0 },
      { id: "uc_kiem_ke", name: "Xác nhận Kiểm kê", row: 2, col: 0 },
      { id: "uc_chon_ncc", name: "Chọn Nhà cung cấp", row: 1, col: 1 },
      { id: "uc_hao_hut", name: "Báo cáo Hao hụt", row: 2, col: 1 },
      { id: "uc_huy_nhap", name: "Xóa/Hủy phiếu Nháp", row: 3, col: 0 }
    ],
    relations: [
      { src: "act_staff", tgt: "uc_view" },
      { src: "act_staff", tgt: "uc_lap_phieu" },
      { src: "uc_lap_phieu", tgt: "uc_chon_ncc", type: "include" },
      { src: "act_staff", tgt: "uc_kiem_ke" },
      { src: "uc_hao_hut", tgt: "uc_kiem_ke", type: "extend" },
      { src: "act_staff", tgt: "uc_huy_nhap" }
    ]
  },
  {
    role: "STAFF",
    filename: "21_hoa_don",
    title: "Use Case: Hóa đơn (Staff)",
    actors: [ { id: "act_staff", name: "Staff" } ],
    usecases: [
      { id: "uc_view", name: "Xem danh sách Hóa đơn", row: 0, col: 0 },
      { id: "uc_lap_hd", name: "Lập Hóa đơn xuất hàng", row: 1, col: 0 },
      { id: "uc_tim_sp", name: "Kiểm tra số lượng tồn kho", row: 1, col: 1 },
      { id: "uc_chon_kh", name: "Điền/Chọn thông tin Khách hàng", row: 2, col: 1 },
      { id: "uc_huy_nhap", name: "Xóa/Hủy Hóa đơn Nháp", row: 2, col: 0 }
    ],
    relations: [
      { src: "act_staff", tgt: "uc_view" },
      { src: "act_staff", tgt: "uc_lap_hd" },
      { src: "uc_lap_hd", tgt: "uc_tim_sp", type: "include" },
      { src: "uc_lap_hd", tgt: "uc_chon_kh", type: "include" },
      { src: "act_staff", tgt: "uc_huy_nhap" }
    ]
  },
  {
    role: "STAFF",
    filename: "22_dieu_chuyen",
    title: "Use Case: Điều chuyển (Staff)",
    actors: [ { id: "act_staff", name: "Staff" } ],
    usecases: [
      { id: "uc_view", name: "Xem phiếu Điều chuyển", row: 0, col: 0 },
      { id: "uc_lap_dc", name: "Lập phiếu Yêu cầu xin hàng", row: 1, col: 0 },
      { id: "uc_chon_kho_nguon", name: "Chọn chi nhánh Nguồn", row: 1, col: 1 },
      { id: "uc_nhan_hang", name: "Xác nhận Kiểm kê nhận hàng", row: 2, col: 0 },
      { id: "uc_bao_loi", name: "Báo cáo chênh lệch/lỗi", row: 2, col: 1 },
      { id: "uc_huy_nhap", name: "Xóa phiếu Yêu cầu Nháp", row: 3, col: 0 }
    ],
    relations: [
      { src: "act_staff", tgt: "uc_view" },
      { src: "act_staff", tgt: "uc_lap_dc" },
      { src: "uc_lap_dc", tgt: "uc_chon_kho_nguon", type: "include" },
      { src: "act_staff", tgt: "uc_nhan_hang" },
      { src: "uc_bao_loi", tgt: "uc_nhan_hang", type: "extend" },
      { src: "act_staff", tgt: "uc_huy_nhap" }
    ]
  },
  {
    role: "STAFF",
    filename: "23_tieu_huy",
    title: "Use Case: Tiêu hủy (Staff)",
    actors: [ { id: "act_staff", name: "Staff" } ],
    usecases: [
      { id: "uc_view", name: "Xem phiếu Tiêu hủy", row: 0, col: 0 },
      { id: "uc_de_xuat", name: "Đề xuất Tiêu hủy", row: 1, col: 0 },
      { id: "uc_chon_sp", name: "Chọn sản phẩm hỏng/cận date", row: 1, col: 1 },
      { id: "uc_ghi_ly_do", name: "Đính kèm hình ảnh và Lý do", row: 2, col: 1 },
      { id: "uc_huy_nhap", name: "Xóa Đề xuất Nháp", row: 2, col: 0 }
    ],
    relations: [
      { src: "act_staff", tgt: "uc_view" },
      { src: "act_staff", tgt: "uc_de_xuat" },
      { src: "uc_de_xuat", tgt: "uc_chon_sp", type: "include" },
      { src: "uc_de_xuat", tgt: "uc_ghi_ly_do", type: "include" },
      { src: "act_staff", tgt: "uc_huy_nhap" }
    ]
  }
];

diagrams.forEach(d => {
    createUsecaseDiagram(d.role, d.filename, d.title, d.actors, d.usecases, d.relations);
});
