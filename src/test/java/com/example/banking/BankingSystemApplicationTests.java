package com.example.banking;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Smoke test: does the Spring context start? Index auto-creation is switched off here
// so this test does not need a running MongoDB server.
@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
class BankingSystemApplicationTests {

	@Test
	void contextLoads() {
	}

}
