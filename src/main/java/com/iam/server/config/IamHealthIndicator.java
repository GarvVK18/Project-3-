package com.iam.server.config;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class IamHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    public IamHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        boolean dbHealthy = false;
        boolean redisHealthy = false;

        // Check DB
        try (var conn = dataSource.getConnection()) {
            dbHealthy = conn.isValid(2);
            details.put("database", "UP (PostgreSQL)");
        } catch (Exception e) {
            details.put("database", "DOWN (" + e.getMessage() + ")");
        }

        // Check Redis
        try {
            if (redisConnectionFactory != null) {
                var conn = redisConnectionFactory.getConnection();
                redisHealthy = "PONG".equalsIgnoreCase(conn.ping());
                details.put("redis", "UP");
            } else {
                details.put("redis", "STANDBY (In-Memory Fallback Active)");
                redisHealthy = true; // Fallback allows app to stay functional
            }
        } catch (Exception e) {
            details.put("redis", "DOWN (" + e.getMessage() + ")");
        }

        if (dbHealthy && redisHealthy) {
            return Health.up().withDetails(details).build();
        }

        return Health.down().withDetails(details).build();
    }
}
