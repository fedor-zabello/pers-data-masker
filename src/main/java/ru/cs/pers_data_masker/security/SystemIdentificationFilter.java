package ru.cs.pers_data_masker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.cs.pers_data_masker.config.SystemConfigResolver;

import java.io.IOException;

/**
 * Идентифицирует систему-потребителя по заголовку {@code X-System-Id}.
 *
 * <p>Если заголовок отсутствует — применяется {@code default}-профиль (для
 * нагрузочного теста). Если система отключена — возвращается 403.
 *
 * <p>Идентифицированная система кладётся в request attribute {@code systemId}.
 */
@Component
public class SystemIdentificationFilter extends OncePerRequestFilter {

    public static final String SYSTEM_ID_ATTRIBUTE = "systemId";
    public static final String SYSTEM_ID_HEADER = "X-System-Id";

    private final SystemConfigResolver resolver;

    public SystemIdentificationFilter(SystemConfigResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String systemId = request.getHeader(SYSTEM_ID_HEADER);
        if (systemId == null || systemId.isBlank()) {
            systemId = SystemConfigResolver.DEFAULT_SYSTEM;
        }

        if (!resolver.isEnabled(systemId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "System is disabled: " + systemId);
            return;
        }

        request.setAttribute(SYSTEM_ID_ATTRIBUTE, systemId);
        filterChain.doFilter(request, response);
    }
}