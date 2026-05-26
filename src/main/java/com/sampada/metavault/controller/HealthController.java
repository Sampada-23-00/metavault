package com.sampada.metavault.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * @RestController = @Controller + @ResponseBody
 *   - @Controller  : marks this class as a Spring MVC controller (a bean that handles HTTP)
 *   - @ResponseBody: automatically serialize return values to JSON (via Jackson)
 *
 * @RequestMapping sets the base URL path for all methods in this class.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Public health check endpoint")
public class HealthController {

    /**
     * @GetMapping maps HTTP GET /api/health to this method.
     * ResponseEntity lets us control the HTTP status code + body.
     * Map.of() creates a simple immutable map — Jackson turns it into JSON.
     */
    @GetMapping
    @Operation(summary = "Health check", description = "Returns service status and current timestamp")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "MetaVault",
                "timestamp", Instant.now().toString()
        ));
    }
}
