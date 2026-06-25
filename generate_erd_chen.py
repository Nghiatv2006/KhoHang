import xml.etree.ElementTree as ET
import xml.dom.minidom
import os

def create_erd_diagram():
    SCALE_X = 1.8
    SCALE_Y = 1.8

    # 1. Initialize XML Structure
    mxfile = ET.Element("mxfile", host="Electron", type="device")
    diagram = ET.SubElement(mxfile, "diagram", id="erd-chen-id", name="Chen ERD Model")
    model = ET.SubElement(diagram, "mxGraphModel", dx="3000", dy="2000", grid="1", gridSize="10", connect="1", arrows="1")
    root = ET.SubElement(model, "root")
    
    # Default Layers
    ET.SubElement(root, "mxCell", id="0")
    ET.SubElement(root, "mxCell", id="1", parent="0")
    
    # 2. Functional Panels (Background Containers)
    panels = [
        {
            "id": "panel_blue",
            "x": 30, "y": 40, "w": 490, "h": 500,
            "style": "rounded=1;whiteSpace=wrap;html=1;fillColor=#E8F0FE;strokeColor=none;opacity=50;collapsible=0;"
        },
        {
            "id": "panel_green",
            "x": 30, "y": 570, "w": 490, "h": 490,
            "style": "rounded=1;whiteSpace=wrap;html=1;fillColor=#E6F4EA;strokeColor=none;opacity=50;collapsible=0;"
        },
        {
            "id": "panel_yellow",
            "x": 550, "y": 40, "w": 740, "h": 700,
            "style": "rounded=1;whiteSpace=wrap;html=1;fillColor=#FEF7E0;strokeColor=none;opacity=50;collapsible=0;"
        },
        {
            "id": "panel_orange",
            "x": 1320, "y": 40, "w": 490, "h": 700,
            "style": "rounded=1;whiteSpace=wrap;html=1;fillColor=#FCE8E6;strokeColor=none;opacity=50;collapsible=0;"
        },
        {
            "id": "panel_purple",
            "x": 550, "y": 770, "w": 740, "h": 290,
            "style": "rounded=1;whiteSpace=wrap;html=1;fillColor=#F3E5F5;strokeColor=none;opacity=50;collapsible=0;"
        }
    ]
    
    for p in panels:
        px = int(p["x"] * SCALE_X)
        py = int(p["y"] * SCALE_Y)
        pw = int(p["w"] * SCALE_X)
        ph = int(p["h"] * SCALE_Y)
        p_cell = ET.SubElement(root, "mxCell", id=p["id"], value="", vertex="1", parent="1")
        p_cell.set("style", p["style"])
        ET.SubElement(p_cell, "mxGeometry", x=str(px), y=str(py), width=str(pw), height=str(ph)).set("as", "geometry")
        
    # 3. Entities Setup
    entities = {
        "Branches": {
            "name": "Branches", "table": "branches", "x": 230, "y": 120, "w": 120, "h": 50,
            "color": "fillColor=#D2E3FC;strokeColor=#1A73E8;fontColor=#1A73E8;",
            "attrs": [
                ("id", True, -110, -50),
                ("name", False, -110, 0),
                ("address", False, -110, 50),
                ("low_stock_threshold", False, 150, -50),
                ("is_head", False, 150, 0),
                ("tax_code", False, 150, 50),
                ("is_locked", False, 50, -60)
            ]
        },
        "Categories": {
            "name": "Categories", "table": "categories", "x": 230, "y": 290, "w": 120, "h": 50,
            "color": "fillColor=#D2E3FC;strokeColor=#1A73E8;fontColor=#1A73E8;",
            "attrs": [
                ("id", True, -110, -30),
                ("name", False, -110, 30)
            ]
        },
        "Backups": {
            "name": "Backups", "table": "backups", "x": 230, "y": 420, "w": 120, "h": 50,
            "color": "fillColor=#D2E3FC;strokeColor=#1A73E8;fontColor=#1A73E8;",
            "attrs": [
                ("id", True, -110, -30),
                ("filename", False, -110, 30),
                ("filepath", False, 150, -30),
                ("file_size", False, 150, 30),
                ("backup_type", False, 20, -50),
                ("created_at", False, 20, 60)
            ]
        },
        "Customers": {
            "name": "Customers", "table": "customers", "x": 230, "y": 640, "w": 120, "h": 50,
            "color": "fillColor=#E6F4EA;strokeColor=#137333;fontColor=#137333;",
            "attrs": [
                ("id", True, -110, -50),
                ("name", False, -110, 0),
                ("contact_info", False, -110, 50),
                ("address", False, 150, -50),
                ("debt", False, 150, 0),
                ("email", False, 150, 50),
                ("tax_code", False, 20, -60),
                ("status", False, 20, 60)
            ]
        },
        "Users": {
            "name": "Users", "table": "users", "x": 230, "y": 810, "w": 120, "h": 50,
            "color": "fillColor=#E6F4EA;strokeColor=#137333;fontColor=#137333;",
            "attrs": [
                ("id", True, -110, -50),
                ("username", False, -110, 0),
                ("password", False, -110, 50),
                ("full_name", False, 150, -50),
                ("email", False, 150, 0),
                ("phone", False, 150, 50),
                ("role", False, -40, -60),
                ("status", False, 60, -60),
                ("created_at", False, -40, 60),
                ("updated_at", False, 60, 60),
                ("ban_until", False, 150, 100)
            ]
        },
        "PasswordResetOTPs": {
            "name": "Password Reset OTPs", "table": "password_reset_otps", "x": 230, "y": 950, "w": 150, "h": 50,
            "color": "fillColor=#E6F4EA;strokeColor=#137333;fontColor=#137333;",
            "attrs": [
                ("id", True, -110, -30),
                ("username", False, -110, 30),
                ("email", False, 160, -30),
                ("otp_code", False, 160, 30),
                ("expiry_time", False, 20, -55),
                ("used", False, 20, 55)
            ]
        },
        "Products": {
            "name": "Products", "table": "products", "x": 650, "y": 120, "w": 120, "h": 50,
            "color": "fillColor=#FEF7E0;strokeColor=#B06000;fontColor=#B06000;",
            "attrs": [
                ("id", True, -110, -50),
                ("code", False, -110, 0),
                ("name", False, -110, 50),
                ("description", False, -40, -60),
                ("unit", False, 60, -60),
                ("import_price", False, 150, -50),
                ("price", False, 150, 0),
                ("has_expiry", False, 150, 50),
                ("image_url", False, -40, 60),
                ("mfg_date", False, 60, 60),
                ("exp_date", False, 150, 100),
                ("is_deleted", False, -110, 100)
            ]
        },
        "Inventories": {
            "name": "Inventories", "table": "inventories", "x": 1050, "y": 120, "w": 120, "h": 50,
            "color": "fillColor=#FEF7E0;strokeColor=#B06000;fontColor=#B06000;",
            "attrs": [
                ("id", True, -110, -50),
                ("quantity", False, -110, 0),
                ("mfg_date", False, -110, 50),
                ("exp_date", False, 50, -60),
                ("batch_code", False, 150, -50),
                ("has_expiry", False, 150, 0),
                ("expiry_warning_days", False, 150, 50),
                ("last_updated", False, 50, 60)
            ]
        },
        "Receipts": {
            "name": "Receipts", "table": "receipts", "x": 650, "y": 420, "w": 120, "h": 50,
            "color": "fillColor=#FEF7E0;strokeColor=#B06000;fontColor=#B06000;",
            "attrs": [
                ("id", True, -110, -50),
                ("code", False, -110, 0),
                ("type", False, -110, 50),
                ("status", False, -40, -60),
                ("payment_status", False, 60, -60),
                ("customer_name", False, 150, -50),
                ("customer_phone", False, 150, 0),
                ("description", False, 150, 50),
                ("created_at", False, 50, 60)
            ]
        },
        "ReceiptDetails": {
            "name": "Receipt Details", "table": "receipt_details", "x": 1050, "y": 420, "w": 120, "h": 50,
            "color": "fillColor=#FEF7E0;strokeColor=#B06000;fontColor=#B06000;",
            "attrs": [
                ("id", True, -110, -50),
                ("quantity", False, -110, 0),
                ("price", False, -110, 50),
                ("mfg_date", False, 50, -60),
                ("exp_date", False, 150, -50),
                ("batch_code", False, 150, 0),
                ("received_quantity", False, 150, 50),
                ("shortfall_reason", False, 50, 60)
            ]
        },
        "Stocktakes": {
            "name": "Stocktakes", "table": "stocktakes", "x": 1450, "y": 150, "w": 120, "h": 50,
            "color": "fillColor=#FCE8E6;strokeColor=#C5221F;fontColor=#C5221F;",
            "attrs": [
                ("id", True, -110, -30),
                ("code", False, -110, 30),
                ("status", False, 150, -30),
                ("notes", False, 150, 30),
                ("created_at", False, 20, -55)
            ]
        },
        "StocktakeDetails": {
            "name": "Stocktake Details", "table": "stocktake_details", "x": 1450, "y": 380, "w": 150, "h": 50,
            "color": "fillColor=#FCE8E6;strokeColor=#C5221F;fontColor=#C5221F;",
            "attrs": [
                ("id", True, -110, -50),
                ("mfg_date", False, -110, 0),
                ("exp_date", False, -110, 50),
                ("batch_code", False, 160, -50),
                ("expected_quantity", False, 160, 0),
                ("actual_quantity", False, 160, 50)
            ]
        },
        "AuditLogs": {
            "name": "Audit Logs", "table": "audit_logs", "x": 800, "y": 850, "w": 120, "h": 50,
            "color": "fillColor=#F3E5F5;strokeColor=#673AB7;fontColor=#673AB7;",
            "attrs": [
                ("id", True, -110, -30),
                ("action", False, -110, 30),
                ("entity_name", False, 150, -30),
                ("entity_id", False, 150, 30),
                ("details", False, -30, -60),
                ("is_warning", False, 50, -60),
                ("created_at", False, 10, 60)
            ]
        }
    }
    
    # Write entities and their attributes
    node_id_counter = 100
    entity_node_ids = {}
    
    for name, data in entities.items():
        node_id_counter += 1
        ent_id = f"ent_{name.lower()}"
        entity_node_ids[name] = ent_id
        
        # Scale coordinates
        data["x"] = int(data["x"] * SCALE_X)
        data["y"] = int(data["y"] * SCALE_Y)
        
        # Draw Entity Rectangle
        val = f"<b>{data['name']}</b><br/><span style='font-size: 9px;'>{data['table']}</span>"
        style = f"shape=rectangle;whiteSpace=wrap;html=1;{data['color']}strokeWidth=1.5;fontStyle=1;align=center;rounded=0;"
        ent_cell = ET.SubElement(root, "mxCell", id=ent_id, value=val, vertex="1", parent="1")
        ent_cell.set("style", style)
        ET.SubElement(ent_cell, "mxGeometry", x=str(data["x"]), y=str(data["y"]), width=str(data["w"]), height=str(data["h"])).set("as", "geometry")
        
        # Draw attributes
        for attr_idx, (attr_name, is_pk, dx, dy) in enumerate(data["attrs"]):
            node_id_counter += 1
            attr_id = f"attr_{name.lower()}_{attr_idx}"
            
            # Format text: Underline if PK
            attr_val = f"<u>{attr_name}</u>" if is_pk else attr_name
            attr_style = "ellipse;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#666666;strokeWidth=1;fontColor=#333333;align=center;fontSize=10;"
            
            attr_cell = ET.SubElement(root, "mxCell", id=attr_id, value=attr_val, vertex="1", parent="1")
            attr_cell.set("style", attr_style)
            
            # Draw ellipse
            ew = 110 if len(attr_name) > 12 else 80
            eh = 35
            ET.SubElement(attr_cell, "mxGeometry", x=str(data["x"] + dx), y=str(data["y"] + dy), width=str(ew), height=str(eh)).set("as", "geometry")
            
            # Draw connection line
            line_id = f"line_{attr_id}"
            line_cell = ET.SubElement(root, "mxCell", id=line_id, value="", edge="1", parent="1", source=attr_id, target=ent_id)
            line_cell.set("style", "endArrow=none;html=1;rounded=0;strokeColor=#888888;strokeWidth=1;")
            line_geom = ET.SubElement(line_cell, "mxGeometry")
            line_geom.set("relative", "1")
            line_geom.set("as", "geometry")
            pt_s = ET.SubElement(line_geom, "mxPoint", x=str(data["x"] + dx + ew/2), y=str(data["y"] + dy + eh/2))
            pt_s.set("as", "sourcePoint")
            pt_t = ET.SubElement(line_geom, "mxPoint", x=str(data["x"] + data["w"]/2), y=str(data["y"] + data["h"]/2))
            pt_t.set("as", "targetPoint")
            
    # 4. Relationships (Diamonds) Setup
    # (name, display_name, src_entity_id, dest_entity_id, x, y, src_card, dest_card)
    relationships = [
        ("categorizes", "phân loại", "Categories", "Products", 460, 220, "1", "N"),
        ("has_staff", "có nhân viên", "Branches", "Users", 245, 520, "1", "N"),
        ("manages_customers", "quản lý", "Branches", "Customers", 380, 390, "1", "N"),
        ("stores", "lưu trữ", "Branches", "Inventories", 500, 80, "1", "N"),
        ("source_branch", "kho nguồn", "Branches", "Receipts", 430, 270, "1", "N"),
        ("dest_branch", "kho đích", "Branches", "Receipts", 480, 330, "1", "N"),
        ("performs_at", "thực hiện tại", "Branches", "Stocktakes", 840, 40, "1", "N"),
        ("has_backup", "có sao lưu", "Branches", "Backups", 180, 270, "1", "N"),
        ("created_by_user", "tạo bởi", "Users", "Backups", 120, 610, "1", "N"),
        ("tracks", "theo dõi", "Products", "Inventories", 880, 115, "1", "N"),
        ("contains_product", "nằm trong", "Products", "ReceiptDetails", 850, 270, "1", "N"),
        ("inspects_product", "kiểm đếm", "Products", "StocktakeDetails", 1050, 250, "1", "N"),
        ("has_otp", "có OTP", "Users", "PasswordResetOTPs", 320, 890, "1", "N"),
        ("created_by", "lập phiếu", "Users", "Receipts", 440, 620, "1", "N"),
        ("stocktake_by", "ghi nhận", "Users", "Receipts", 495, 620, "1", "N"),
        ("creates_stocktake", "lập phiên", "Users", "Stocktakes", 940, 480, "1", "N"),
        ("performs_action", "thực hiện", "Users", "AuditLogs", 515, 830, "1", "N"),
        ("receives", "nhận hàng", "Customers", "Receipts", 440, 535, "1", "N"),
        ("has_details", "chứa chi tiết", "Receipts", "ReceiptDetails", 850, 415, "1", "N"),
        ("has_stocktake_details", "chứa chi tiết", "Stocktakes", "StocktakeDetails", 1460, 265, "1", "N"),
        ("generates", "tự sinh", "StocktakeDetails", "Receipts", 1050, 480, "N", "1"),
        ("logs_branch_action", "ghi nhật ký", "Branches", "AuditLogs", 515, 480, "1", "N")
    ]
    
    for idx, rel in enumerate(relationships):
        rel_name, display_name, src, dest, rx, ry, src_card, dest_card = rel
        rel_id = f"rel_{rel_name}_{idx}"
        
        # Scale relationship coordinates
        rx_scaled = int(rx * SCALE_X)
        ry_scaled = int(ry * SCALE_Y)
        
        # Draw Relationship Rhombus
        style = "rhombus;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#333333;strokeWidth=1.2;fontColor=#333333;align=center;fontSize=10;"
        rel_cell = ET.SubElement(root, "mxCell", id=rel_id, value=display_name, vertex="1", parent="1")
        rel_cell.set("style", style)
        ET.SubElement(rel_cell, "mxGeometry", x=str(rx_scaled), y=str(ry_scaled), width="100", height="60").set("as", "geometry")
        
        # Get coordinates of source and destination entities
        src_data = entities[src]
        dest_data = entities[dest]
        src_x = src_data["x"] + src_data["w"]/2
        src_y = src_data["y"] + src_data["h"]/2
        dest_x = dest_data["x"] + dest_data["w"]/2
        dest_y = dest_data["y"] + dest_data["h"]/2

        # Connect Source (Entity) to Relationship (Diamond)
        src_arrow = "ERmandOne" if src_card == "1" else "ERmandMany"
        src_edge_id = f"edge_src_{idx}"
        src_edge = ET.SubElement(root, "mxCell", id=src_edge_id, value="", edge="1", parent="1", source=entity_node_ids[src], target=rel_id)
        src_edge.set("style", f"endArrow=none;startArrow={src_arrow};startSize=8;html=1;rounded=0;strokeColor=#555555;strokeWidth=1.2;")
        src_geom = ET.SubElement(src_edge, "mxGeometry")
        src_geom.set("relative", "1")
        src_geom.set("as", "geometry")
        pt_s = ET.SubElement(src_geom, "mxPoint", x=str(src_x), y=str(src_y))
        pt_s.set("as", "sourcePoint")
        pt_t = ET.SubElement(src_geom, "mxPoint", x=str(rx_scaled + 50), y=str(ry_scaled + 30))
        pt_t.set("as", "targetPoint")
        
        # Connect Relationship (Diamond) to Destination (Entity)
        dest_arrow = "ERmandOne" if dest_card == "1" else "ERmandMany"
        dest_edge_id = f"edge_dest_{idx}"
        dest_edge = ET.SubElement(root, "mxCell", id=dest_edge_id, value="", edge="1", parent="1", source=rel_id, target=entity_node_ids[dest])
        dest_edge.set("style", f"endArrow={dest_arrow};endSize=8;startArrow=none;html=1;rounded=0;strokeColor=#555555;strokeWidth=1.2;")
        dest_geom = ET.SubElement(dest_edge, "mxGeometry")
        dest_geom.set("relative", "1")
        dest_geom.set("as", "geometry")
        pt_s2 = ET.SubElement(dest_geom, "mxPoint", x=str(rx_scaled + 50), y=str(ry_scaled + 30))
        pt_s2.set("as", "sourcePoint")
        pt_t2 = ET.SubElement(dest_geom, "mxPoint", x=str(dest_x), y=str(dest_y))
        pt_t2.set("as", "targetPoint")
        
    # 5. Title Text
    title_cell = ET.SubElement(root, "mxCell", id="diagram_title", value="ERD Diagram - Chen Notation (WareHub System)", vertex="1", parent="1")
    title_cell.set("style", "text;html=1;align=center;verticalAlign=middle;resizable=0;points=[];autosize=1;strokeColor=none;fillColor=none;fontSize=20;fontStyle=1;fontColor=#000000;")
    ET.SubElement(title_cell, "mxGeometry", x=str(int(650 * SCALE_X)), y="5", width="500", height="30").set("as", "geometry")

    # 6. Pretty Print and Export File
    xml_str = ET.tostring(mxfile, encoding="utf-8")
    dom = xml.dom.minidom.parseString(xml_str)
    pretty_xml = dom.toprettyxml(indent="  ")
    
    os.makedirs("d:\\IT\\Hehe\\diagrams", exist_ok=True)
    output_path = "d:\\IT\\Hehe\\diagrams\\erd-chen-notation.drawio"
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(pretty_xml)
    print(f"[+] Draw.io ERD file generated at: {output_path}")

if __name__ == "__main__":
    print("Executing generate_erd_chen.py...")
    create_erd_diagram()
