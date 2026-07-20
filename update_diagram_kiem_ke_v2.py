import re

new_content = '''            <root>
                <mxCell id="0"/>
                <mxCell id="1" parent="0"/>
                
                <!-- Backgrounds -->
                <mxCell id="bg_user" value="" style="fillColor=#f2f7fc;strokeColor=none;" parent="1" vertex="1">
                    <mxGeometry x="20" y="50" width="280" height="900" as="geometry"/>
                </mxCell>
                <mxCell id="bg_user_header" value="1. Admin / Quản lý" style="fillColor=#f5f5f5;strokeColor=#cccccc;fontStyle=1;align=center;whiteSpace=wrap;html=1;" parent="1" vertex="1">
                    <mxGeometry x="20" y="10" width="280" height="40" as="geometry"/>
                </mxCell>
                <mxCell id="bg_ui" value="" style="fillColor=#fffdf0;strokeColor=none;" parent="1" vertex="1">
                    <mxGeometry x="300" y="50" width="280" height="900" as="geometry"/>
                </mxCell>
                <mxCell id="bg_ui_header" value="2. Màn hình làm việc" style="fillColor=#f5f5f5;strokeColor=#cccccc;fontStyle=1;align=center;whiteSpace=wrap;html=1;" parent="1" vertex="1">
                    <mxGeometry x="300" y="10" width="280" height="40" as="geometry"/>
                </mxCell>
                <mxCell id="bg_backend" value="" style="fillColor=#faf5fc;strokeColor=none;" parent="1" vertex="1">
                    <mxGeometry x="580" y="50" width="280" height="900" as="geometry"/>
                </mxCell>
                <mxCell id="bg_backend_header" value="3. Hệ thống xử lý" style="fillColor=#f5f5f5;strokeColor=#cccccc;fontStyle=1;align=center;whiteSpace=wrap;html=1;" parent="1" vertex="1">
                    <mxGeometry x="580" y="10" width="280" height="40" as="geometry"/>
                </mxCell>
                <mxCell id="bg_db" value="" style="fillColor=#f5faf5;strokeColor=none;" parent="1" vertex="1">
                    <mxGeometry x="860" y="50" width="260" height="900" as="geometry"/>
                </mxCell>
                <mxCell id="bg_db_header" value="4. Cơ sở dữ liệu" style="fillColor=#f5f5f5;strokeColor=#cccccc;fontStyle=1;align=center;whiteSpace=wrap;html=1;" parent="1" vertex="1">
                    <mxGeometry x="860" y="10" width="260" height="40" as="geometry"/>
                </mxCell>

                <mxCell id="sep1" value="" style="endArrow=none;dashed=1;html=1;strokeColor=#dddddd;strokeWidth=1.5;" parent="1" vertex="1">
                    <mxGeometry x="300" y="10" width="1" height="940" as="geometry"/>
                </mxCell>
                <mxCell id="sep2" value="" style="endArrow=none;dashed=1;html=1;strokeColor=#dddddd;strokeWidth=1.5;" parent="1" vertex="1">
                    <mxGeometry x="580" y="10" width="1" height="940" as="geometry"/>
                </mxCell>
                <mxCell id="sep3" value="" style="endArrow=none;dashed=1;html=1;strokeColor=#dddddd;strokeWidth=1.5;" parent="1" vertex="1">
                    <mxGeometry x="860" y="10" width="1" height="940" as="geometry"/>
                </mxCell>

                <!-- Start -->
                <mxCell id="StartNode" value="" style="ellipse;whiteSpace=wrap;html=1;aspect=fixed;fillColor=#000000;strokeColor=none;" parent="1" vertex="1">
                    <mxGeometry x="145" y="70" width="30" height="30" as="geometry"/>
                </mxCell>
                
                <!-- Row 1: Truy cập -->
                <mxCell id="U1" value="Truy cập chức năng&lt;br/&gt;Kiểm kê kho" style="rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#e3f2fd;strokeColor=#1e88e5;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="80" y="130" width="160" height="50" as="geometry"/>
                </mxCell>
                
                <mxCell id="F1" value="Gửi yêu cầu lấy danh sách&lt;br/&gt;phiếu kiểm kê" style="rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#e3f2fd;strokeColor=#1e88e5;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="360" y="130" width="160" height="50" as="geometry"/>
                </mxCell>
                
                <mxCell id="B1" value="Tìm kiếm danh sách phiếu&lt;br/&gt;từ cơ sở dữ liệu" style="rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#e3f2fd;strokeColor=#1e88e5;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="640" y="130" width="160" height="50" as="geometry"/>
                </mxCell>

                <mxCell id="D1" value="Truy xuất dữ liệu&lt;br/&gt;Phiếu kiểm kê" style="shape=mxgraph.flowchart.database;whiteSpace=wrap;fillColor=#e8f5e9;strokeColor=#43a047;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="910" y="130" width="160" height="50" as="geometry"/>
                </mxCell>

                <!-- Row 2: Hiển thị -->
                <mxCell id="F2" value="Hiển thị danh sách phiếu&lt;br/&gt;đang Chờ duyệt" style="rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#e3f2fd;strokeColor=#1e88e5;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="350" y="220" width="180" height="50" as="geometry"/>
                </mxCell>

                <!-- Row 3: Xem chi tiết -->
                <mxCell id="U2" value="Chọn phiếu, xem chi tiết&lt;br/&gt;số liệu chênh lệch&lt;br/&gt;(Chỉ đọc, không sửa được)" style="rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#fff9c4;strokeColor=#fbc02d;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="60" y="310" width="200" height="60" as="geometry"/>
                </mxCell>
                
                <!-- Row 4: Quyết định -->
                <mxCell id="U3" value="Quyết định xử lý?" style="rhombus;whiteSpace=wrap;html=1;fillColor=#fff3e0;strokeColor=#fb8c00;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="80" y="420" width="160" height="80" as="geometry"/>
                </mxCell>

                <!-- Nhánh trái: Yêu cầu đếm lại -->
                <mxCell id="U4a" value="Click &quot;Yêu cầu đếm lại&quot;&lt;br/&gt;Nhập lý do" style="rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#ffccbc;strokeColor=#d84315;strokeWidth=2;fontStyle=1;" parent="1" vertex="1">
                    <mxGeometry x="60" y="550" width="200" height="50" as="geometry"/>
                </mxCell>
                
                <mxCell id="F3a" value="Truyền lệnh Từ chối&lt;br/&gt;về Hệ thống" style="rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#e3f2fd;strokeColor=#1e88e5;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="360" y="550" width="160" height="50" as="geometry"/>
                </mxCell>

                <mxCell id="B3a" value="Chuyển phiếu về Nháp.&lt;br/&gt;Lưu lý do từ chối.&lt;br/&gt;Chờ nhân viên tự sửa số &amp;amp; nộp lại" style="rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#e3f2fd;strokeColor=#1e88e5;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="620" y="540" width="220" height="70" as="geometry"/>
                </mxCell>

                <mxCell id="D3a" value="Cập nhật trạng thái&lt;br/&gt;phiếu kiểm kê" style="shape=mxgraph.flowchart.database;whiteSpace=wrap;fillColor=#e8f5e9;strokeColor=#43a047;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="910" y="545" width="160" height="60" as="geometry"/>
                </mxCell>

                <!-- Nhánh phải: Duyệt chênh lệch -->
                <mxCell id="U4b" value="Click &quot;Duyệt chênh lệch&quot;&lt;br/&gt;Nhập lý do &amp;amp; Người chịu trách nhiệm" style="rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#e3f2fd;strokeColor=#1e88e5;strokeWidth=2;fontStyle=1;" parent="1" vertex="1">
                    <mxGeometry x="60" y="670" width="200" height="60" as="geometry"/>
                </mxCell>
                
                <mxCell id="F3b" value="Truyền lệnh Duyệt&lt;br/&gt;về Hệ thống" style="rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#e3f2fd;strokeColor=#1e88e5;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="360" y="675" width="160" height="50" as="geometry"/>
                </mxCell>

                <mxCell id="B3b" value="Tự động sinh phiếu Điều chỉnh&lt;br/&gt;(Thêm/Bớt tồn kho cho khớp).&lt;br/&gt;Lưu lý do &amp;amp; người chịu trách nhiệm.&lt;br/&gt;Hoàn tất phiếu kiểm kê" style="rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#e3f2fd;strokeColor=#1e88e5;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="610" y="660" width="230" height="80" as="geometry"/>
                </mxCell>

                <mxCell id="D3b" value="Lưu Phiếu mới &amp;amp;&lt;br/&gt;Cập nhật Tồn kho" style="shape=mxgraph.flowchart.database;whiteSpace=wrap;fillColor=#e8f5e9;strokeColor=#43a047;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="910" y="675" width="160" height="50" as="geometry"/>
                </mxCell>

                <!-- Kết quả chung -->
                <mxCell id="F4" value="Hiển thị thông báo&lt;br/&gt;Thao tác thành công" style="rounded=1;whiteSpace=wrap;html=1;arcSize=20;fillColor=#d4edda;strokeColor=#c3e6cb;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="360" y="800" width="160" height="50" as="geometry"/>
                </mxCell>

                <mxCell id="EndNode" value="" style="ellipse;whiteSpace=wrap;html=1;aspect=fixed;fillColor=#ffffff;strokeColor=#000000;strokeWidth=2;" parent="1" vertex="1">
                    <mxGeometry x="425" y="880" width="30" height="30" as="geometry"/>
                </mxCell>
                <mxCell id="EndNode_inner" value="" style="ellipse;whiteSpace=wrap;html=1;aspect=fixed;fillColor=#000000;strokeColor=none;" parent="1" vertex="1">
                    <mxGeometry x="433" y="888" width="14" height="14" as="geometry"/>
                </mxCell>

                <!-- Edges -->
                <mxCell id="e0" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;" parent="1" source="StartNode" target="U1" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>
                <mxCell id="e1" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;" parent="1" source="U1" target="F1" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>
                <mxCell id="e2" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;" parent="1" source="F1" target="B1" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>
                <mxCell id="e3" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;" parent="1" source="B1" target="D1" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>
                <mxCell id="e4" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;exitX=0;exitY=0.5;entryX=1;entryY=0.5;" parent="1" source="B1" target="F2" edge="1">
                    <mxGeometry relative="1" as="geometry"><Array as="points"><mxPoint x="600" y="155"/><mxPoint x="600" y="245"/></Array></mxGeometry>
                </mxCell>
                <mxCell id="e5" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;exitX=0;exitY=0.5;entryX=1;entryY=0.5;" parent="1" source="F2" target="U2" edge="1">
                    <mxGeometry relative="1" as="geometry"><Array as="points"><mxPoint x="320" y="245"/><mxPoint x="320" y="340"/></Array></mxGeometry>
                </mxCell>
                <mxCell id="e6" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;" parent="1" source="U2" target="U3" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>

                <!-- Nhánh trái -->
                <mxCell id="e7a" value="Số liệu sai / Cần đếm lại" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#d84315;strokeWidth=1.5;endArrow=classic;fontStyle=1;fontColor=#d84315;exitX=0.5;exitY=1;entryX=0.5;entryY=0;" parent="1" source="U3" target="U4a" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>
                <mxCell id="e8a" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;" parent="1" source="U4a" target="F3a" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>
                <mxCell id="e9a" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;" parent="1" source="F3a" target="B3a" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>
                <mxCell id="e10a" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;" parent="1" source="B3a" target="D3a" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>

                <!-- Nhánh phải -->
                <mxCell id="e7b" value="Mất hàng thật sự" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#1e88e5;strokeWidth=1.5;endArrow=classic;fontStyle=1;fontColor=#1e88e5;exitX=0.5;exitY=1;entryX=0.5;entryY=0;" parent="1" source="U4a" target="U4b" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>
                <mxCell id="e8b" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;" parent="1" source="U4b" target="F3b" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>
                <mxCell id="e9b" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;" parent="1" source="F3b" target="B3b" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>
                <mxCell id="e10b" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;" parent="1" source="B3b" target="D3b" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>

                <!-- Cả 2 nhánh đều đổ về thông báo thành công -->
                <mxCell id="e11a" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;exitX=0;exitY=0.5;entryX=1;entryY=0.25;" parent="1" source="B3a" target="F4" edge="1">
                    <mxGeometry relative="1" as="geometry"><Array as="points"><mxPoint x="600" y="575"/><mxPoint x="600" y="812"/></Array></mxGeometry>
                </mxCell>
                <mxCell id="e11b" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;exitX=0;exitY=0.5;entryX=1;entryY=0.75;" parent="1" source="B3b" target="F4" edge="1">
                    <mxGeometry relative="1" as="geometry"><Array as="points"><mxPoint x="600" y="700"/><mxPoint x="600" y="837"/></Array></mxGeometry>
                </mxCell>

                <mxCell id="e12" value="" style="edgeStyle=orthogonalEdgeStyle;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=classic;" parent="1" source="F4" target="EndNode" edge="1">
                    <mxGeometry relative="1" as="geometry"/>
                </mxCell>

            </root>'''

with open('d:/IT/Hehe/diagrams/admin/chuc_nang_kiem_ke_kho_admin.drawio', 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'<root>.*?</root>', new_content, content, flags=re.DOTALL)

with open('d:/IT/Hehe/diagrams/admin/chuc_nang_kiem_ke_kho_admin.drawio', 'w', encoding='utf-8') as f:
    f.write(content)
