package com.example.Hehe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HeheApplication {

	public static void main(String[] args) {
		SpringApplication.run(HeheApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.boot.CommandLineRunner updateDb(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				int updated = jdbcTemplate.update("UPDATE users SET username = 'manager_dn', full_name = 'Lê Cường (QL Đà Nẵng)', branch_id = 3 WHERE username = 'manager_hn'");
				if (updated > 0) {
					System.out.println("========== DB MIGRATION: Successfully updated manager_hn to manager_dn! ==========");
				}
			} catch (Exception e) {
				System.out.println("DB migration failed or already applied: " + e.getMessage());
			}
		};
	}

}
