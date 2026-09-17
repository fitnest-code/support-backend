package az.fitnest.support.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Order(1)
public class GatewayHeaderFilter extends OncePerRequestFilter {
    private static final String GATEWAY_HEADER = "X-From-Gateway";
    private static final String GATEWAY_HEADER_VALUE = "1";
    private static final List<String> IGNORED_PATHS = List.of(
            "/actuator", "/actuator/", "/swagger-ui", "/swagger-ui/", "/swagger-ui.html", "/v3/api-docs", "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (IGNORED_PATHS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (path.startsWith("/api/v1/")) {
            String gatewayHeader = request.getHeader(GATEWAY_HEADER);
            if (!GATEWAY_HEADER_VALUE.equals(gatewayHeader)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                String timestamp = java.time.OffsetDateTime.now().toString();
                String json = String.format("{\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"Requests must come via API Gateway\",\"status\":403,\"path\":\"%s\",\"timestamp\":\"%s\"}}", path, timestamp);
                response.getWriter().write(json);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
