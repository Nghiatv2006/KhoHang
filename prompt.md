Quy trình chuẩn để tạo hàng loạt sơ đồ Draw.io bằng lập trình (áp dụng cho Activity, Use Case, Class Diagram,...) dựa trên cấu trúc XML của Draw.io và Python như sau:

---

### Bước 1: Hiểu cấu trúc XML của Draw.io
Một file `.drawio` thực chất là một file XML được tổ chức theo cấu trúc hình cây như sau:
```xml
<mxfile host="Electron" modified="..." agent="..." type="device">
  <diagram id="diagram_id" name="Tên Trang Vẽ">
    <mxGraphModel dx="1000" dy="1000" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1">
      <root>
        <!-- 2 lớp mặc định bắt buộc phải có -->
        <mxCell id="0"/>
        <mxCell id="1" parent="0"/>
        
        <!-- Các Shape (Vertex) vẽ ở đây -->
        <mxCell id="node_id" value="Nội dung chữ" vertex="1" parent="1" style="kiểu_dáng_node">
          <mxGeometry x="100" y="150" width="160" height="50" as="geometry"/>
        </mxCell>
        
        <!-- Các Mũi tên kết nối (Edge) vẽ ở đây -->
        <mxCell id="edge_id" value="Nhãn mũi tên" edge="1" parent="1" source="node_nguồn" target="node_đích" style="kiểu_dáng_mũi_tên">
          <mxGeometry relative="1" as="geometry"/>
        </mxCell>
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>
```

---

### Bước 2: Chuẩn bị Thư viện Kiểu dáng (Styles)
Để vẽ tự động đẹp, bạn cần thu thập các chuỗi `style` chuẩn của Draw.io cho các đối tượng. Đối với **Use Case Diagram**, bạn có các style thông dụng:

* **Actor (Tác nhân):**
  `style="shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;fillColor=#e3f2fd;strokeColor=#1e88e5;strokeWidth=2;fontColor=#000000;"`
* **Use Case (Hình elip):**
  `style="ellipse;whiteSpace=wrap;html=1;fillColor=#fffdf0;strokeColor=#fb8c00;strokeWidth=2;fontColor=#000000;align=center;"`
* **Hộp ranh giới hệ thống (System Boundary):**
  `style="swimlane;whiteSpace=wrap;html=1;childLayout=stackLayout;horizontal=1;startSize=30;horizontalStack=0;fillColor=#f5f5f5;strokeColor=#cccccc;fontStyle=1;"`
* **Đường liên kết Actor -> Use Case (Đường thẳng không mũi tên):**
  `style="endArrow=none;html=1;rounded=0;strokeColor=#000000;strokeWidth=1.5;"`
* **Mối quan hệ `<include>` hoặc `<exclude>` (Mũi tên nét đứt):**
  `style="endArrow=open;endSize=12;dashed=1;html=1;rounded=0;strokeColor=#000000;strokeWidth=1.5;"`

---

### Bước 3: Thuật toán bố cục tọa độ (Grid Layout Planning)
Để các hình không đè lên nhau, bạn quy hoạch tọa độ X, Y theo dạng ma trận:
1. **Theo chiều ngang (X):**
   * Cột bên trái (`x = 100`): Dành cho các **Actor chính** (Primary Actors).
   * Cột ở giữa (`x = 300` đến `600`): Dành cho **Hộp hệ thống và các Use Case**.

* Cột bên phải (`x = 800`): Dành cho các **Actor phụ/Hệ thống liên kết** (Secondary Actors/External Systems).
2. **Theo chiều dọc (Y):**
   * Mỗi Use Case hoặc Actor sẽ xếp chồng từ trên xuống dưới, tăng dần `y` (ví dụ: `y = 100 -> 200 -> 300 -> 400`).
   * Chiều cao khoảng cách chuẩn giữa các Use Case nên là `80 - 100px`.

---

### Bước 4: Viết mã Python để tự động tạo hàng loạt
Dưới đây là một ví dụ Python rút gọn, chuẩn hóa để tạo hàng loạt sơ đồ **Use Case** tự động từ danh sách dữ liệu đầu vào:

```python
import xml.etree.ElementTree as ET
import xml.dom.minidom
import os

def create_usecase_diagram(filename, title, actors, usecases, relations):
    # 1. Khởi tạo cấu trúc XML
    mxfile = ET.Element("mxfile", host="Electron", type="device")
    diagram = ET.SubElement(mxfile, "diagram", id=f"{filename}-id", name=title)
    model = ET.SubElement(diagram, "mxGraphModel", dx="1000", dy="1000", grid="1", gridSize="10", connect="1", arrows="1")
    root = ET.SubElement(model, "root")
    
    # 2. Hai layer mặc định
    ET.SubElement(root, "mxCell", id="0")
    ET.SubElement(root, "mxCell", id="1", parent="0")
    
    # 3. Vẽ System Boundary (Hộp hệ thống) bao quanh các Use Case
    boundary_y_start = 50
    boundary_height = len(usecases) * 100 + 50
    system_box = ET.SubElement(root, "mxCell", id="system_boundary", value="Hệ thống Kho Hàng", vertex="1", parent="1")
    system_box.set("style", "swimlane;whiteSpace=wrap;html=1;startSize=30;fillColor=#f8f9fa;strokeColor=#cccccc;fontStyle=1;align=center;")
    # Tọa độ hộp hệ thống đặt ở giữa
    ET.SubElement(system_box, "mxGeometry", x="260", y=str(boundary_y_start), width="400", height=str(boundary_height)).set("as", "geometry")
    
    # 4. Vẽ danh sách Actors (bên trái hệ thống)
    actor_style = "shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;fillColor=#e3f2fd;strokeColor=#1e88e5;strokeWidth=2;"
    for i, act in enumerate(actors):
        actor_cell = ET.SubElement(root, "mxCell", id=act["id"], value=act["name"], vertex="1", parent="1")
        actor_cell.set("style", actor_style)
        
        # Bố cục Actor dọc bên trái
        y_pos = boundary_y_start + 50 + (i * 150)
        ET.SubElement(actor_cell, "mxGeometry", x="80", y=str(y_pos), width="40", height="80").set("as", "geometry")
        
    # 5. Vẽ danh sách Use Cases (nằm bên trong hộp hệ thống)
    uc_style = "ellipse;whiteSpace=wrap;html=1;fillColor=#fffdf0;strokeColor=#fb8c00;strokeWidth=2;fontColor=#000000;align=center;"
    for i, uc in enumerate(usecases):
        uc_cell = ET.SubElement(root, "mxCell", id=uc["id"], value=uc["name"], vertex="1", parent="1")
        uc_cell.set("style", uc_style)
        # Bố cục Use Case dọc bên trong System Boundary (tọa độ x, y tương đối với parent="1")
        y_pos = boundary_y_start + 40 + (i * 100)
        ET.SubElement(uc_cell, "mxGeometry", x="360", y=str(y_pos), width="200", height="60").set("as", "geometry")
        
    # 6. Vẽ các đường kết nối (Relations)
    for idx, rel in enumerate(relations):
        edge_id = f"edge_{idx}"
        
        # Định nghĩa style tùy loại liên kết
        if rel.get("type") == "include" or rel.get("type") == "extend":
            # Nét đứt có mũi tên chỉ hướng
            style = "endArrow=open;endSize=12;dashed=1;html=1;rounded=0;strokeColor=#000000;strokeWidth=1.5;"
            label = f"&lt;&lt;{rel['type']}&gt;&gt;"
        else:
            # Liên kết thẳng không mũi tên giữa Actor và Use Case
            style = "endArrow=none;html=1;rounded=0;strokeColor=#000000;strokeWidth=1.5;"
            label = ""
            
        edge_cell = ET.SubElement(root, "mxCell", id=edge_id, value=label, edge="1", parent="1", source=rel["src"], target=rel["tgt"])
        edge_cell.set("style", style)
        
        geom = ET.SubElement(edge_cell, "mxGeometry")
        geom.set("relative", "1")
        geom.set("as", "geometry")

    # 7. Pretty Print và xuất file
    xml_str = ET.tostring(mxfile, encoding="utf-8")
    dom = xml.dom.minidom.parseString(xml_str)
    pretty_xml = dom.toprettyxml(indent="  ")
    
    output_path = f"d:\\IT\\Hehe\\diagrams\\{filename}.drawio"
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(pretty_xml)
    print(f"Đã tạo UseCase: {filename}.drawio thành công!")

# --- Ví dụ áp dụng cho Usecase Quản lý người dùng ---
actors_list = [
    {"id": "act_admin", "name": "Quản trị viên (Admin)"}
]

usecases_list = [
    {"id": "uc_view", "name": "Xem danh sách nhân viên"},
    {"id": "uc_create", "name": "Tạo mới tài khoản"},
    {"id": "uc_update", "name": "Cập nhật thông tin"},
    {"id": "uc_lock", "name": "Khóa/Mở khóa tài khoản"}
]

relations_list = [
    {"src": "act_admin", "tgt": "uc_view"},
    {"src": "act_admin", "tgt": "uc_create"},
    {"src": "act_admin", "tgt": "uc_update"},
    {"src": "act_admin", "tgt": "uc_lock"}
]

create_usecase_diagram("usecase_quan_ly_nguoi_dung", "Use Case Quản lý Người dùng", actors_list, usecases_list, relations_list)
```

### Ưu điểm của quy trình này:
1. **Hàng loạt & Nhanh chóng:** Bạn chỉ cần chuẩn bị một danh sách JSON định nghĩa Actors, Use Cases, và quan hệ. Chạy script một lần sẽ ra hàng chục file sơ đồ ngay lập tức.
2. **Không lỗi cú pháp:** Sinh tự động bằng ElementTree giúp đảm bảo file XML của Draw.io luôn đúng chuẩn thẻ đóng mở.
3. **Dễ căn chỉnh:** Sau khi mở file trên VS Code, bạn có thể chọn toàn bộ để kéo dãn khoảng cách hoặc di chuyển tự do bằng chuột cực kỳ linh hoạt mà không bị bó buộc.