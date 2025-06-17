package co.teamsphere.api.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import java.io.IOException;

import static co.teamsphere.api.filters.FilterOrders.REQUEST_LOGGING_FILTER_ORDER;

@Order(REQUEST_LOGGING_FILTER_ORDER)
public class LoggingFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingFilter.class);
    private static final TimerThreadLocal TIMER = new TimerThreadLocal();
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            TIMER.start();
            LOG.info("Start: {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            long duration = TIMER.stop();
            LOG.info("Took {}ms to {} {} | method={}, uri={}, duration={}, status={}",
                duration, request.getMethod(), request.getRequestURI(), request.getMethod(),
                request.getRequestURI(), duration,response.getStatus());
        }
    }
    private static final class TimerThreadLocal extends ThreadLocal<Long>{
        void start(){ set(System.currentTimeMillis());}
        long stop(){
            Long startTime = get();
            remove();
            if (startTime == null){
                return -1;
            }
            return System.currentTimeMillis() - startTime;
        }
    }
}
