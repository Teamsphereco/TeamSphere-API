package co.teamsphere.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class MainController {

    @Value("${spring.profiles.active:default}")
    private String activeProfiles;

    private final ApplicationContext applicationContext;

    public MainController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> status = new LinkedHashMap<>();

        // Basic info
        status.put("application", "TeamSphere API");
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now().toString());
        status.put("activeProfile", activeProfiles);

        // Environment info
        status.put("environment", applicationContext.getEnvironment().getProperty("spring.application.name"));

        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("profile", activeProfiles);

        return ResponseEntity.ok(health);
    }
}
