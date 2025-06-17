package co.teamsphere.api.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

import static co.teamsphere.api.filters.FilterOrders.GENERATE_REQUEST_ID_FILTER_ORDER;

@Component
@Order(GENERATE_REQUEST_ID_FILTER_ORDER)
public class GenerateRequestIDFilter implements Filter {
    private final Supplier<String> requestIdGenerator = () -> UUID.randomUUID().toString();
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest request){
            String requestId = request.getHeader(RequestHeaders.X_REQUEST_ID.getHeader());

            if (StringUtils.isBlank(requestId)){
                request.setAttribute(RequestHeaders.X_REQUEST_ID.getHeader(), requestIdGenerator.get());
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
