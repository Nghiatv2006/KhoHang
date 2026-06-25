import xml.etree.ElementTree as ET
import xml.dom.minidom
import os

def build_activity_diagram(filename, title, nodes, edges, height=700, user_role_name="1. Admin"):
    mxfile = ET.Element("mxfile", host="Electron", type="device")
    diagram = ET.SubElement(mxfile, "diagram", id=f"{filename}-diagram-id", name=title)
    model = ET.SubElement(diagram, "mxGraphModel", dx="2036", dy="1058", grid="1", gridSize="10", connect="1", arrows="1", fold="1", page="1", pageScale="1", pageWidth="827", pageHeight="1169", math="0", shadow="0")
    root = ET.SubElement(model, "root")
    
    # Default Layers
    ET.SubElement(root, "mxCell", id="0")
    ET.SubElement(root, "mxCell", id="1", parent="0")
    
    cols = [
        {"id": "bg_user", "name": user_role_name, "x": 40, "color": "#f2f7fc"},
        {"id": "bg_ui", "name": "2. Giao diện Frontend", "x": 310, "color": "#fffdf0"},
        {"id": "bg_backend", "name": "3. Hệ thống Backend", "x": 580, "color": "#faf5fc"},
        {"id": "bg_db", "name": "4. Cơ sở dữ liệu PostgreSQL", "x": 850, "color": "#f5faf5"}
    ]
    
    for col in cols:
        bg_cell = ET.SubElement(root, "mxCell", id=col["id"], value="", vertex="1", parent="1")
        bg_cell.set("style", f"fillColor={col['color']};strokeColor=none;")
        ET.SubElement(bg_cell, "mxGeometry", x=str(col["x"]), y="50", width="260", height=str(height)).set("as", "geometry")
        
        header_cell = ET.SubElement(root, "mxCell", id=f"{col['id']}_header", value=col["name"], vertex="1", parent="1")
        header_cell.set("style", "fillColor=#f5f5f5;strokeColor=#cccccc;fontStyle=1;align=center;whiteSpace=wrap;html=1;")
        ET.SubElement(header_cell, "mxGeometry", x=str(col["x"]), y="10", width="260", height="40").set("as", "geometry")

    # Separators
    for idx, sep_x in enumerate([300, 570, 840], start=1):
        sep_cell = ET.SubElement(root, "mxCell", id=f"sep{idx}", value="", vertex="1", parent="1")
        sep_cell.set("style", "endArrow=none;dashed=1;html=1;strokeColor=#dddddd;strokeWidth=1.5;")
        ET.SubElement(sep_cell, "mxGeometry", x=str(sep_x), y="10", width="1", height=str(height + 40)).set("as", "geometry")
        
    # Map old x (for width 220 swimlanes) to new x (for width 260 swimlanes)
    x_map = {
        70: 90,
        135: 155,
        310: 360,
        375: 425,
        550: 630,
        610: 690,
        810: 920
    }

    # Draw nodes
    for node in nodes:
        node_id = node["id"]
        val = node.get("value", "")
        ntype = node.get("type", "action") # action, db, start, end, rhombus, success, error
        nx = node["x"]
        ny = node["y"]
        nw = node.get("w", 160)
        nh = node.get("h", 50)
        
        # Apply coordinate mapping
        nx = x_map.get(nx, nx)
        
        cell = ET.SubElement(root, "mxCell", id=node_id, value=val, vertex="1", parent="1")
        
        if ntype == "start":
            style = "ellipse;whiteSpace=wrap;html=1;aspect=fixed;fillColor=#000000;strokeColor=none;"
        elif ntype == "end":
            style = "ellipse;whiteSpace=wrap;html=1;aspect=fixed;fillColor=#ffffff;strokeColor=#000000;strokeWidth=2;"
            # Inner end node
            inner_id = f"{node_id}_inner"
            inner_cell = ET.SubElement(root, "mxCell", id=inner_id, value="", vertex="1", parent="1")
            inner_cell.set("style", "ellipse;whiteSpace=wrap;html=1;aspect=fixed;fillColor=#000000;strokeColor=none;")
            ET.SubElement(inner_cell, "mxGeometry", x=str(nx + (nw-14)/2), y=str(ny + (nh-14)/2), width="14", height="14").set("as", "geometry")
        elif ntype == "rhombus":
            style = "rhombus;whiteSpace=wrap;html=1;fillColor=#fff3e0;strokeColor=#fb8c00;strokeWidth=2;fontColor=#000000;align=center;"
        elif ntype == "db":
            style = "strokeWidth=2;html=1;shape=mxgraph.flowchart.database;whiteSpace=wrap;fillColor=#e8f5e9;strokeColor=#43a047;fontColor=#000000;align=center;"
        elif ntype == "success":
            style = "rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#d4edda;strokeColor=#c3e6cb;strokeWidth=2;fontColor=#155724;align=center;"
        elif ntype == "error":
            style = "rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#f8d7da;strokeColor=#f5c6cb;strokeWidth=2;fontColor=#721c24;align=center;"
        else: # action
            style = "rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#e3f2fd;strokeColor=#1e88e5;strokeWidth=2;fontColor=#000000;align=center;"
            
        cell.set("style", style)
        ET.SubElement(cell, "mxGeometry", x=str(nx), y=str(ny), width=str(nw), height=str(nh)).set("as", "geometry")

    # Draw edges
    for idx, edge in enumerate(edges):
        edge_id = f"edge_{idx}"
        src = edge["src"]
        tgt = edge["tgt"]
        lbl = edge.get("label", "")
        exit_p = edge.get("exit_p")
        entry_p = edge.get("entry_p")
        
        cell = ET.SubElement(root, "mxCell", id=edge_id, value=lbl, edge="1", parent="1", source=src, target=tgt)
        style = "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;"
        if exit_p:
            style += f"exitX={exit_p[0]};exitY={exit_p[1]};"
        if entry_p:
            style += f"entryX={entry_p[0]};entryY={entry_p[1]};"
        cell.set("style", style)
        geom = ET.SubElement(cell, "mxGeometry")
        geom.set("relative", "1")
        geom.set("as", "geometry")

    xml_str = ET.tostring(mxfile, encoding="utf-8")
    dom = xml.dom.minidom.parseString(xml_str)
    pretty_xml = dom.toprettyxml(indent="  ")
    
    os.makedirs("d:\\IT\\Hehe\\diagrams\\admin", exist_ok=True)
    output_path = f"d:\\IT\\Hehe\\diagrams\\admin\\{filename}.drawio"
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(pretty_xml)
    print(f"[+] Diagram saved at: {output_path}")

def generate_all_diagrams():
    # 1. chuc_nang_quan_ly_nguoi_dung_admin (Height: 800)
    nodes_1 = [
        {"id": "StartNode", "x": 135, "y": 70, "w": 30, "h": 30, "type": "start"},
        {"id": "U1", "value": "Mở module Quản lý người dùng", "x": 70, "y": 130},
        {"id": "B1", "value": "Lấy danh sách người dùng:<br/>- Bản thân<br/>- Nhân viên cùng chi nhánh tổng<br/>- Manager chi nhánh con", "x": 550, "y": 130, "h": 65},
        {"id": "D1", "value": "Bảng Users", "x": 810, "y": 120, "type": "db"},
        {"id": "F1", "value": "Hiển thị danh sách nhân viên<br/>lọc & các nút Thêm/Sửa/Khóa", "x": 310, "y": 220, "h": 60},
        {"id": "U2", "value": "Thực hiện CRUD/Khóa nhân viên", "x": 70, "y": 300, "h": 60},
        {"id": "B2", "value": "Kiểm tra quyền & ràng buộc:<br/>- Không tự khóa/hạ quyền mình<br/>- CN tổng không có Manager<br/>- Không xóa user đã giao dịch", "x": 550, "y": 300, "h": 70},
        {"id": "B2_rhombus", "value": "", "x": 610, "y": 390, "w": 40, "h": 40, "type": "rhombus"},
        {"id": "F_error", "value": "Hiển thị thông báo lỗi", "x": 310, "y": 385, "type": "error"},
        {"id": "B3", "value": "Lưu DB (Mật khẩu băm BCrypt)<br/>& ghi log Audit Log", "x": 550, "y": 460, "h": 60},
        {"id": "D2", "value": "Bảng Users & Audit Logs", "x": 810, "y": 450, "type": "db"},
        {"id": "F_success", "value": "Thông báo thành công", "x": 310, "y": 550, "type": "success"},
        {"id": "EndNode", "x": 375, "y": 630, "w": 30, "h": 30, "type": "end"}
    ]
    edges_1 = [
        {"src": "StartNode", "tgt": "U1"},
        {"src": "U1", "tgt": "B1", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "B1", "tgt": "D1", "exit_p": (1, 0.5), "entry_p": (0.5, 0)},
        {"src": "B1", "tgt": "F1", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "F1", "tgt": "U2", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "U2", "tgt": "B2", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "B2", "tgt": "B2_rhombus", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "B2_rhombus", "tgt": "F_error", "exit_p": (0, 0.5), "entry_p": (1, 0.5), "label": "Lỗi ràng buộc"},
        {"src": "F_error", "tgt": "U2", "exit_p": (0.5, 0), "entry_p": (0.5, 1)},
        {"src": "B2_rhombus", "tgt": "B3", "exit_p": (0.5, 1), "entry_p": (0.5, 0), "label": "Hợp lệ"},
        {"src": "B3", "tgt": "D2", "exit_p": (1, 0.5), "entry_p": (0.5, 0)},
        {"src": "B3", "tgt": "F_success", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "F_success", "tgt": "EndNode", "exit_p": (0.5, 1), "entry_p": (0.5, 0)}
    ]
    build_activity_diagram("chuc_nang_quan_ly_nguoi_dung_admin", "Quản lý Người dùng (Admin)", nodes_1, edges_1, height=800)

    # 2. chuc_nang_quan_ly_ton_kho_admin (Height: 650)
    nodes_2 = [
        {"id": "StartNode", "x": 135, "y": 70, "w": 30, "h": 30, "type": "start"},
        {"id": "U1", "value": "Mở xem tồn kho", "x": 70, "y": 130},
        {"id": "B1", "value": "Lấy dữ liệu tồn kho:<br/>- ADMIN xem toàn bộ chi nhánh<br/>- MANAGER/STAFF xem CN mình", "x": 550, "y": 130, "h": 65},
        {"id": "D1", "value": "Bảng Inventories", "x": 810, "y": 120, "type": "db"},
        {"id": "F1", "value": "Hiển thị danh sách tồn kho<br/>& nút Nhập lô / Cộng kho", "x": 310, "y": 220, "h": 60},
        {"id": "U2", "value": "Chọn Cộng thêm (addStock) hoặc<br/>Nhập lô mới (createInventory)", "x": 70, "y": 300, "h": 60},
        {"id": "B2", "value": "Kiểm tra quyền (Staff bị chặn)<br/>& thông tin lô hàng (NSX/HSD)", "x": 550, "y": 300, "h": 60},
        {"id": "B2_rhombus", "value": "", "x": 610, "y": 380, "w": 40, "h": 40, "type": "rhombus"},
        {"id": "F_error", "value": "Báo lỗi trên giao diện", "x": 310, "y": 375, "type": "error"},
        {"id": "B3", "value": "Lưu/Cộng dồn vào lô hàng DB<br/>& ghi log ADD_INVENTORY_STOCK", "x": 550, "y": 450, "h": 60},
        {"id": "D2", "value": "Cập nhật Inventories & Logs", "x": 810, "y": 440, "type": "db"},
        {"id": "F_success", "value": "Thông báo thành công<br/>& cập nhật tồn kho mới", "x": 310, "y": 530, "h": 60, "type": "success"},
        {"id": "EndNode", "x": 375, "y": 610, "w": 30, "h": 30, "type": "end"}
    ]
    edges_2 = [
        {"src": "StartNode", "tgt": "U1"},
        {"src": "U1", "tgt": "B1", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "B1", "tgt": "D1", "exit_p": (1, 0.5), "entry_p": (0.5, 0)},
        {"src": "B1", "tgt": "F1", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "F1", "tgt": "U2", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "U2", "tgt": "B2", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "B2", "tgt": "B2_rhombus", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "B2_rhombus", "tgt": "F_error", "exit_p": (0, 0.5), "entry_p": (1, 0.5), "label": "Staff / Lỗi NSX"},
        {"src": "F_error", "tgt": "U2", "exit_p": (0.5, 0), "entry_p": (0.5, 1)},
        {"src": "B2_rhombus", "tgt": "B3", "exit_p": (0.5, 1), "entry_p": (0.5, 0), "label": "Hợp lệ"},
        {"src": "B3", "tgt": "D2", "exit_p": (1, 0.5), "entry_p": (0.5, 0)},
        {"src": "B3", "tgt": "F_success", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "F_success", "tgt": "EndNode", "exit_p": (0.5, 1), "entry_p": (0.5, 0)}
    ]
    build_activity_diagram("chuc_nang_quan_ly_ton_kho_admin", "Quản lý Tồn kho (Admin)", nodes_2, edges_2, height=650)

    # 3. chuc_nang_phe_duyet_phieu_kho_admin (Height: 650)
    nodes_3 = [
        {"id": "StartNode", "x": 135, "y": 70, "w": 30, "h": 30, "type": "start"},
        {"id": "U1", "value": "Xem danh sách duyệt phiếu kho", "x": 70, "y": 130},
        {"id": "B1", "value": "Lọc phiếu IMPORT/TRANSFER đang ở<br/>PENDING_ADMIN của chi nhánh con", "x": 550, "y": 130, "h": 60},
        {"id": "D1", "value": "Bảng Receipts", "x": 810, "y": 120, "type": "db"},
        {"id": "F1", "value": "Hiển thị danh sách phiếu chờ duyệt", "x": 310, "y": 210, "h": 50},
        {"id": "U2", "value": "Chọn duyệt phiếu PENDING_ADMIN", "x": 70, "y": 290},
        {"id": "B2", "value": "Kiểm tra quyền ADMIN hệ thống", "x": 550, "y": 290, "h": 60},
        {"id": "B2_rhombus", "value": "", "x": 610, "y": 370, "w": 40, "h": 40, "type": "rhombus"},
        {"id": "F_error", "value": "Thông báo lỗi quyền duyệt", "x": 310, "y": 365, "type": "error"},
        {"id": "B3", "value": "Duyệt: Chuyển sang PENDING_STOCKTAKE<br/>& Trừ tồn kho nguồn (nếu IMPORT chéo)", "x": 550, "y": 450, "h": 65},
        {"id": "D2", "value": "Cập nhật Inventories & Receipts", "x": 810, "y": 440, "type": "db"},
        {"id": "F_success", "value": "Thông báo duyệt thành công,<br/>cập nhật trạng thái PENDING_STOCKTAKE", "x": 310, "y": 530, "h": 60, "type": "success"},
        {"id": "EndNode", "x": 375, "y": 610, "w": 30, "h": 30, "type": "end"}
    ]
    edges_3 = [
        {"src": "StartNode", "tgt": "U1"},
        {"src": "U1", "tgt": "B1", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "B1", "tgt": "D1", "exit_p": (1, 0.5), "entry_p": (0.5, 0)},
        {"src": "B1", "tgt": "F1", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "F1", "tgt": "U2", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "U2", "tgt": "B2", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "B2", "tgt": "B2_rhombus", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "B2_rhombus", "tgt": "F_error", "exit_p": (0, 0.5), "entry_p": (1, 0.5), "label": "Sai quyền"},
        {"src": "F_error", "tgt": "U2", "exit_p": (0.5, 0), "entry_p": (0.5, 1)},
        {"src": "B2_rhombus", "tgt": "B3", "exit_p": (0.5, 1), "entry_p": (0.5, 0), "label": "Hợp lệ"},
        {"src": "B3", "tgt": "D2", "exit_p": (1, 0.5), "entry_p": (0.5, 0)},
        {"src": "B3", "tgt": "F_success", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "F_success", "tgt": "EndNode", "exit_p": (0.5, 1), "entry_p": (0.5, 0)}
    ]
    build_activity_diagram("chuc_nang_phe_duyet_phieu_kho_admin", "Phê duyệt Phiếu kho (Admin)", nodes_3, edges_3, height=650)

    # 4. chuc_nang_kiem_ke_kho_admin (Height: 650)
    nodes_4 = [
        {"id": "StartNode", "x": 135, "y": 70, "w": 30, "h": 30, "type": "start"},
        {"id": "U1", "value": "[Nhân viên] Tạo phiếu ST nháp<br/>& nhập số liệu thực tế", "x": 70, "y": 130, "h": 60},
        {"id": "F1", "value": "Hiển thị phiếu DRAFT kèm cột<br/>expected và actual quantity", "x": 310, "y": 130, "h": 60},
        {"id": "U2", "value": "[Admin] Xem phiếu kiểm kê,<br/>đối chiếu chênh lệch thực tế", "x": 70, "y": 220, "h": 60},
        {"id": "U3", "value": "[Admin] Click hoàn tất (Chốt kiểm kê)", "x": 70, "y": 310},
        {"id": "B1", "value": "1. Cập nhật expectedQty thời gian thực<br/>2. Đóng phiếu ST sang COMPLETED", "x": 550, "y": 310, "h": 60},
        {"id": "B2", "value": "So sánh actual vs expected:<br/>tự động sinh ADJUST_IN / ADJUST_OUT", "x": 550, "y": 400, "h": 60},
        {"id": "D1", "value": "Cập nhật Inventories, stocktakes<br/>& tự động lưu Receipts COMPLETED", "x": 810, "y": 375, "w": 130, "type": "db"},
        {"id": "F2", "value": "Khóa phiếu & hiển thị kết quả chênh lệch<br/>và các phiếu điều chỉnh tự sinh", "x": 310, "y": 490, "h": 60, "type": "success"},
        {"id": "EndNode", "x": 375, "y": 570, "w": 30, "h": 30, "type": "end"}
    ]
    edges_4 = [
        {"src": "StartNode", "tgt": "U1"},
        {"src": "U1", "tgt": "F1", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "F1", "tgt": "U2", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "U2", "tgt": "U3", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "U3", "tgt": "B1", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "B1", "tgt": "B2", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "B2", "tgt": "D1", "exit_p": (1, 0.5), "entry_p": (0.5, 0)},
        {"src": "B2", "tgt": "F2", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "F2", "tgt": "EndNode", "exit_p": (0.5, 1), "entry_p": (0.5, 0)}
    ]
    build_activity_diagram("chuc_nang_kiem_ke_kho_admin", "Kiểm kê kho (Admin)", nodes_4, edges_4, height=650, user_role_name="1. Nhân viên & Admin")

    # 5. chuc_nang_sao_luu_du_lieu_admin (Height: 650)
    nodes_5 = [
        {"id": "StartNode", "x": 135, "y": 70, "w": 30, "h": 30, "type": "start"},
        {"id": "U1", "value": "Mở xem Lịch sử sao lưu", "x": 70, "y": 130},
        {"id": "B1", "value": "Truy vấn danh sách bản ghi sao lưu<br/>của chi nhánh (Admin thuộc CN Tổng)", "x": 550, "y": 130, "h": 60},
        {"id": "D1", "value": "Bảng Backups", "x": 810, "y": 120, "type": "db"},
        {"id": "F1", "value": "Hiển thị danh sách sao lưu,<br/>nút Sao lưu & nút Trigger Tự động", "x": 310, "y": 210, "h": 60},
        {"id": "U2", "value": "Click nút 'Sao lưu thủ công' (Export)<br/>hoặc 'Trigger sao lưu tự động'", "x": 70, "y": 300, "h": 60},
        {"id": "B2", "value": "Sao lưu thủ công: Gom dữ liệu,<br/>ký HMAC-SHA256, mã hóa AES-256-GCM", "x": 550, "y": 300, "h": 60},
        {"id": "B3", "value": "Trigger tự động (ADMIN only):<br/>chạy task sao lưu cho tất cả CN", "x": 550, "y": 390, "h": 60},
        {"id": "D2", "value": "Ghi tệp tin vào thư mục server,<br/>lưu bảng Backups & Ghi Audit Log", "x": 810, "y": 330, "w": 130, "type": "db"},
        {"id": "F2", "value": "Tải file .wbk về trình duyệt<br/>hoặc báo trigger thành công", "x": 310, "y": 480, "h": 60, "type": "success"},
        {"id": "EndNode", "x": 375, "y": 570, "w": 30, "h": 30, "type": "end"}
    ]
    edges_5 = [
        {"src": "StartNode", "tgt": "U1"},
        {"src": "U1", "tgt": "B1", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "B1", "tgt": "D1", "exit_p": (1, 0.5), "entry_p": (0.5, 0)},
        {"src": "B1", "tgt": "F1", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "F1", "tgt": "U2", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "U2", "tgt": "B2", "exit_p": (0.5, 1), "entry_p": (0, 0.5), "label": "Thủ công"},
        {"src": "U2", "tgt": "B3", "exit_p": (0.5, 1), "entry_p": (0, 0.5), "label": "Trigger tự động"},
        {"src": "B2", "tgt": "D2", "exit_p": (1, 0.5), "entry_p": (0.5, 0)},
        {"src": "B3", "tgt": "D2", "exit_p": (1, 0.5), "entry_p": (0.5, 1)},
        {"src": "B2", "tgt": "F2", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "B3", "tgt": "F2", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "F2", "tgt": "EndNode", "exit_p": (0.5, 1), "entry_p": (0.5, 0)}
    ]
    build_activity_diagram("chuc_nang_sao_luu_du_lieu_admin", "Sao lưu Dữ liệu (Admin)", nodes_5, edges_5, height=650)

    # 6. chuc_nang_phuc_hoi_du_lieu_admin (Height: 680)
    nodes_6 = [
        {"id": "StartNode", "x": 135, "y": 70, "w": 30, "h": 30, "type": "start"},
        {"id": "U1", "value": "Mở menu Phục hồi dữ liệu", "x": 70, "y": 130},
        {"id": "F1", "value": "Hiển thị vùng tải file .wbk", "x": 310, "y": 130},
        {"id": "U2", "value": "Tải file backup .wbk lên", "x": 70, "y": 210},
        {"id": "B1", "value": "1. Đọc Magic Header 'WHBK' & ENC_VERSION<br/>2. Kiểm tra HMAC, giải mã AES-256-GCM<br/>3. Tạm khóa chi nhánh (is_locked = true)", "x": 550, "y": 210, "h": 70},
        {"id": "D1", "value": "Cập nhật status is_locked = True", "x": 810, "y": 205, "type": "db"},
        {"id": "B2_rhombus", "value": "", "x": 610, "y": 310, "w": 40, "h": 40, "type": "rhombus"},
        {"id": "B3_fail", "value": "Rollback transaction,<br/>mở khóa giao dịch chi nhánh", "x": 550, "y": 380, "h": 60},
        {"id": "F_fail", "value": "Báo lỗi file hỏng/sai chữ ký/sai CN", "x": 310, "y": 380, "h": 60, "type": "error"},
        {"id": "B3_success", "value": "Nạp đè dữ liệu vào các bảng DB,<br/>mở khóa chi nhánh & ghi Audit Log", "x": 550, "y": 470, "h": 60},
        {"id": "D2", "value": "Ghi CSDL PostgreSQL", "x": 810, "y": 460, "type": "db"},
        {"id": "F_success", "value": "Thông báo phục hồi dữ liệu thành công", "x": 310, "y": 560, "h": 60, "type": "success"},
        {"id": "EndNode", "x": 375, "y": 630, "w": 30, "h": 30, "type": "end"}
    ]
    edges_6 = [
        {"src": "StartNode", "tgt": "U1"},
        {"src": "U1", "tgt": "F1", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "F1", "tgt": "U2", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "U2", "tgt": "B1", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "B1", "tgt": "D1", "exit_p": (1, 0.5), "entry_p": (0.5, 0)},
        {"src": "B1", "tgt": "B2", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "B2", "tgt": "B2_rhombus", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "B2_rhombus", "tgt": "B3_fail", "exit_p": (0.5, 1), "entry_p": (0.5, 0), "label": "Sai chữ ký / lỗi"},
        {"src": "B3_fail", "tgt": "F_fail", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "B2_rhombus", "tgt": "B3_success", "exit_p": (1, 0.5), "entry_p": (0.5, 0), "label": "Hợp lệ"},
        {"src": "B3_success", "tgt": "D2", "exit_p": (1, 0.5), "entry_p": (0.5, 0)},
        {"src": "B3_success", "tgt": "F_success", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "F_success", "tgt": "EndNode", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "F_fail", "tgt": "EndNode", "exit_p": (0.5, 1), "entry_p": (0.5, 0)}
    ]
    build_activity_diagram("chuc_nang_phuc_hoi_du_lieu_admin", "Phục hồi Dữ liệu (Admin)", nodes_6, edges_6, height=680)

    # 7. chuc_nang_nhap_excel_san_pham_admin (Height: 650)
    nodes_7 = [
        {"id": "StartNode", "x": 135, "y": 70, "w": 30, "h": 30, "type": "start"},
        {"id": "U1", "value": "Mở menu Sản phẩm", "x": 70, "y": 130},
        {"id": "F1", "value": "Hiển thị danh sách sản phẩm<br/>& nút 'Nhập từ Excel'", "x": 310, "y": 130, "h": 50},
        {"id": "U2", "value": "Chọn tải file Excel lên", "x": 70, "y": 210},
        {"id": "B1", "value": "Kiểm tra quyền ADMIN.<br/>Đọc file Excel, tìm dòng tiêu đề", "x": 550, "y": 210, "h": 60},
        {"id": "B2", "value": "Validate: Tên SP, tên DM bắt buộc,<br/>Mã SKU duy nhất (nếu có, tự sinh nếu trống),<br/>Giá bán >= 0, Giá nhập >= 0", "x": 550, "y": 300, "h": 70},
        {"id": "B2_rhombus", "value": "", "x": 610, "y": 400, "w": 40, "h": 40, "type": "rhombus"},
        {"id": "B3_fail", "value": "Gom danh sách lỗi các dòng chi tiết", "x": 550, "y": 470, "h": 50},
        {"id": "F_fail", "value": "Hiển thị chi tiết lỗi từng dòng", "x": 310, "y": 470, "h": 50, "type": "error"},
        {"id": "B3_success", "value": "Lưu danh sách sản phẩm mới vào DB<br/>(Lưu ý: KHÔNG khởi tạo tồn kho)", "x": 550, "y": 550, "h": 60},
        {"id": "D1", "value": "Bảng Products", "x": 810, "y": 540, "type": "db"},
        {"id": "F_success", "value": "Thông báo số lượng nhập thành công", "x": 310, "y": 550, "h": 60, "type": "success"},
        {"id": "EndNode", "x": 375, "y": 620, "w": 30, "h": 30, "type": "end"}
    ]
    edges_7 = [
        {"src": "StartNode", "tgt": "U1"},
        {"src": "U1", "tgt": "F1", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "F1", "tgt": "U2", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "U2", "tgt": "B1", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "B1", "tgt": "B2", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "B2", "tgt": "B2_rhombus", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "B2_rhombus", "tgt": "B3_fail", "exit_p": (0.5, 1), "entry_p": (0.5, 0), "label": "Có dòng lỗi"},
        {"src": "B3_fail", "tgt": "F_fail", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "B2_rhombus", "tgt": "B3_success", "exit_p": (1, 0.5), "entry_p": (0.5, 0), "label": "Tất cả hợp lệ"},
        {"src": "B3_success", "tgt": "D1", "exit_p": (1, 0.5), "entry_p": (0.5, 0)},
        {"src": "B3_success", "tgt": "F_success", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "F_success", "tgt": "EndNode", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "F_fail", "tgt": "EndNode", "exit_p": (0.5, 1), "entry_p": (0.5, 0)}
    ]
    build_activity_diagram("chuc_nang_nhap_excel_san_pham_admin", "Nhập excel hàng loạt (Admin)", nodes_7, edges_7, height=680)

    # 8. chuc_nang_tra_cuu_audit_log_admin (Height: 520)
    nodes_8 = [
        {"id": "StartNode", "x": 135, "y": 70, "w": 30, "h": 30, "type": "start"},
        {"id": "U1", "value": "Mở module Nhật ký hoạt động", "x": 70, "y": 130},
        {"id": "F1", "value": "Gửi yêu cầu xem nhật ký", "x": 310, "y": 130},
        {"id": "B1", "value": "Kiểm tra quyền ADMIN.<br/>Ghi đè branchId = 1 (Chi nhánh Tổng)", "x": 550, "y": 130, "h": 60},
        {"id": "B2", "value": "Truy vấn logs thuộc chi nhánh 1", "x": 550, "y": 210, "h": 50},
        {"id": "D1", "value": "Bảng Audit Logs", "x": 810, "y": 200, "type": "db"},
        {"id": "F2", "value": "Hiển thị logs chi tiết (JSON)", "x": 310, "y": 290, "h": 60, "type": "success"},
        {"id": "EndNode", "x": 375, "y": 380, "w": 30, "h": 30, "type": "end"}
    ]
    edges_8 = [
        {"src": "StartNode", "tgt": "U1"},
        {"src": "U1", "tgt": "F1", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "F1", "tgt": "B1", "exit_p": (1, 0.5), "entry_p": (0, 0.5)},
        {"src": "B1", "tgt": "B2", "exit_p": (0.5, 1), "entry_p": (0.5, 0)},
        {"src": "B2", "tgt": "D1", "exit_p": (1, 0.5), "entry_p": (0.5, 0)},
        {"src": "B2", "tgt": "F2", "exit_p": (0, 0.5), "entry_p": (1, 0.5)},
        {"src": "F2", "tgt": "EndNode", "exit_p": (0.5, 1), "entry_p": (0.5, 0)}
    ]
    build_activity_diagram("chuc_nang_tra_cuu_audit_log_admin", "Tra cứu Audit Log (Admin)", nodes_8, edges_8, height=520)

if __name__ == "__main__":
    generate_all_diagrams()
