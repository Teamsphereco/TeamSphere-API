package co.teamsphere.api.filters;

import org.springframework.core.Ordered;

public class FilterOrders {
    public static final int GENERATE_REQUEST_ID_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE;
    public static final int REQUEST_LOGGING_FILTER_ORDER = GENERATE_REQUEST_ID_FILTER_ORDER + 10;
}
