package com.example.Hehe;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@SpringBootTest
class HeheApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
		System.out.println("=== DIAGNOSTIC START ===");
		try {
			Long productsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Long.class);
			System.out.println("Products count: " + productsCount);

			Long receiptsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM receipts", Long.class);
			System.out.println("Receipts count: " + receiptsCount);

			Long detailsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM receipt_details", Long.class);
			System.out.println("Receipt details count: " + detailsCount);

			List<Map<String, Object>> detailsByReceiptType = jdbcTemplate.queryForList(
				"SELECT r.type, COUNT(d.id) as cnt, SUM(d.quantity * d.price) as total_val " +
				"FROM receipts r JOIN receipt_details d ON r.id = d.receipt_id " +
				"GROUP BY r.type"
			);
			for (Map<String, Object> m : detailsByReceiptType) {
				System.out.println("Type: " + m.get("type") + ", details count: " + m.get("cnt") + ", total value: " + m.get("total_val"));
			}

			List<Map<String, Object>> activeUser = jdbcTemplate.queryForList("SELECT id, username, role, branch_id FROM users");
			for (Map<String, Object> u : activeUser) {
				System.out.println("Active User: ID=" + u.get("id") + ", Username=" + u.get("username") + ", Role=" + u.get("role") + ", BranchID=" + u.get("branch_id"));
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
		System.out.println("=== DIAGNOSTIC END ===");
	}

}



