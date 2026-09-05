package com.iam.server.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class IamHealthIndicatorTest {

    private DataSource dataSource;
    private Connection connection;
    private IamHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);

        healthIndicator = new IamHealthIndicator(dataSource);
    }

    @Test
    void testHealth_returnsUpWhenDatabaseIsValid() {
        Health health = healthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertTrue(health.getDetails().containsKey("database"));
    }

    @Test
    void testHealth_returnsDownWhenDatabaseThrowsException() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB Connection Refused"));

        Health health = healthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
    }
}
