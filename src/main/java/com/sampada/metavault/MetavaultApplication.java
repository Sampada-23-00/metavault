package com.sampada.metavault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @SpringBootApplication is a convenience annotation that combines:
 *   - @Configuration      : this class is a source of Spring bean definitions
 *   - @EnableAutoConfiguration : Spring Boot auto-configures beans based on classpath
 *   - @ComponentScan      : scan this package (and sub-packages) for @Component, @Service, etc.
 *
 * The main() method simply hands control to Spring Boot, which:
 *   1. Starts an embedded Tomcat server
 *   2. Reads application.yml
 *   3. Connects to the database
 *   4. Wires up all your beans
 *   5. Registers all your REST endpoints
 */
@SpringBootApplication
public class MetavaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetavaultApplication.class, args);
    }
}
