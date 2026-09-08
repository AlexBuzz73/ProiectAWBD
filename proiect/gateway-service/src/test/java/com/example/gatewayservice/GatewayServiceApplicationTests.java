package com.example.gatewayservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
})
@Import(TestJwtConfig.class)
class GatewayServiceApplicationTests {

    @Test
    @DisplayName("Gateway context loads successfully")
    void contextLoads() {
    }
}
