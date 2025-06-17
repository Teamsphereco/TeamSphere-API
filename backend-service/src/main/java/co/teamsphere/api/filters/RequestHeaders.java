package co.teamsphere.api.filters;

public enum RequestHeaders {
    X_REQUEST_ID("X-Request-ID");
    private final String headerName;

    RequestHeaders(String headerName) {
        this.headerName = headerName;
    }

    public String getHeader() {
        return headerName;
    }
}
