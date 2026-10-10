
package com.example.banking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"jwt.secret=wrQfg1CNHBmzcTHkmatqxn4VuB1Z5R1m3EVrENag9V8=",
		"jwt.expiration-ms=3600000"
})
class BankingSystemApplicationTests {

	@Test
	void contextLoads() {
	}

}
