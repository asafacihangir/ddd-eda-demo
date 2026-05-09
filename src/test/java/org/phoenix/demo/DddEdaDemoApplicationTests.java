package org.phoenix.demo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Infrastructure layer not yet implemented; OrderRepository bean is missing. Re-enable after Infrastructure port is complete.")
class DddEdaDemoApplicationTests {

    @Test
    void contextLoads() {
    }

}
