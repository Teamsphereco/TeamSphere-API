package co.teamsphere.api.filters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import co.teamsphere.api.helpers.TestAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import ch.qos.logback.classic.Logger;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoggingFilterTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @InjectMocks
    private LoggingFilter loggingFilter;
    private TestAppender testAppender;
    private final Logger logger = (Logger) LoggerFactory.getLogger(LoggingFilter.class);

    @BeforeEach
    void setUp() {
        // Initialize mocks annotated with @Mock and inject them into @InjectMocks fields
        MockitoAnnotations.openMocks(this);

        // Configure and attach the custom Logback appender
        testAppender = new TestAppender();
        testAppender.start(); // Important: Start the appender
        logger.addAppender(testAppender);
        // Set logger level to DEBUG to capture both DEBUG (start) and INFO (end) logs
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(testAppender);
        testAppender.stop();
        testAppender.clearEvents();
    }

    @Test
    void doFilter_logsRequestStartAndEndWithNoRequestId() throws IOException, ServletException {
        // Arrange
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test-uri");
        // Simulate no X-Request-ID header initially, so filter should log "N/A"
        when(response.getStatus()).thenReturn(200);

        // Act
        loggingFilter.doFilter(request, response, filterChain);

        // Assert
        // Verify that the filter chain proceeded
        verify(filterChain).doFilter(request, response);

        // Verify log messages
        List<ILoggingEvent> logEvents = testAppender.getLogEvents();
        assertThat(logEvents).hasSize(2); // Expecting a DEBUG (start) and an INFO (end) log

        // Verify start log (DEBUG) content
        assertThat(logEvents.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvents.get(0).getFormattedMessage())
            .matches("Start: GET /test-uri");

        // Verify end log (INFO) content
        assertThat(logEvents.get(1).getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
        assertThat(logEvents.get(1).getFormattedMessage())
            .matches("Took \\d+ms to GET /test-uri \\| method=GET, uri=/test-uri, duration=\\d+, status=200");
    }


}
