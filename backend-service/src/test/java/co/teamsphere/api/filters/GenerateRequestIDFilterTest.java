package co.teamsphere.api.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateRequestIDFilterTest {

    private GenerateRequestIDFilter generateRequestIDFilter;

    @Mock
    private HttpServletRequest mockHttpServletRequest;

    @Mock
    private HttpServletResponse mockHttpServletResponse;

    @Mock
    private FilterChain mockFilterChain;

    @BeforeEach
    void setUp() {
        generateRequestIDFilter = new GenerateRequestIDFilter();
        try {
            java.lang.reflect.Field field = GenerateRequestIDFilter.class.getDeclaredField("requestIdGenerator");
            field.setAccessible(true);
            // We can set a predictable UUID for testing purposes
            field.set(generateRequestIDFilter, (Supplier<String>) () -> "test-generated-uuid");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void doFilter_HttpServletRequest_NoRequestIdHeader_GeneratesNewId() throws IOException, ServletException {
        when(mockHttpServletRequest.getHeader(RequestHeaders.X_REQUEST_ID.getHeader())).thenReturn(null); // No existing header
        generateRequestIDFilter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);
        verify(mockHttpServletRequest).setAttribute(eq(RequestHeaders.X_REQUEST_ID.getHeader()), eq("test-generated-uuid"));
        verify(mockFilterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    }

    @Test
    void doFilter_HttpServletRequest_BlankRequestIdHeader_GeneratesNewId() throws IOException, ServletException {
        when(mockHttpServletRequest.getHeader(RequestHeaders.X_REQUEST_ID.getHeader())).thenReturn(""); // Blank header
        generateRequestIDFilter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);
        verify(mockHttpServletRequest).setAttribute(eq(RequestHeaders.X_REQUEST_ID.getHeader()), eq("test-generated-uuid"));
        verify(mockFilterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    }

    @Test
    void doFilter_HttpServletRequest_ExistingRequestIdHeader_DoesNotGenerateNewId() throws IOException, ServletException {
        String existingRequestId = "existing-request-id-123";
        when(mockHttpServletRequest.getHeader(RequestHeaders.X_REQUEST_ID.getHeader())).thenReturn(existingRequestId);
        generateRequestIDFilter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);
        verify(mockHttpServletRequest, never()).setAttribute(eq(RequestHeaders.X_REQUEST_ID.getHeader()), anyString());
        verify(mockFilterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    }

    @Test
    void doFilter_NonHttpServletRequest_DoesNothingAndContinuesChain() throws IOException, ServletException {
        jakarta.servlet.ServletRequest mockServletRequest = mock(jakarta.servlet.ServletRequest.class);
        generateRequestIDFilter.doFilter(mockServletRequest, mockHttpServletResponse, mockFilterChain);
        verifyNoInteractions(mockServletRequest);
        verify(mockFilterChain).doFilter(mockServletRequest, mockHttpServletResponse);
    }
}

