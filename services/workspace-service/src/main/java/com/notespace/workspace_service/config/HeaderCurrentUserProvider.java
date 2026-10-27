package com.notespace.workspace_service.config;

import com.notespace.workspace_service.exception.CurrentUserRequiredException;
import com.notespace.workspace_service.service.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import java.util.Collections;
import java.util.UUID;

/** Development adapter; registered only by the explicit local/test profile configuration. */
final class HeaderCurrentUserProvider implements CurrentUserProvider {
    private final ObjectProvider<HttpServletRequest> requests;

    HeaderCurrentUserProvider(ObjectProvider<HttpServletRequest> requests) {
        this.requests = requests;
    }

    @Override
    public UUID getCurrentUserId() {
        var request = requests.getIfAvailable();
        if (request == null) throw new CurrentUserRequiredException();
        var headers = Collections.list(request.getHeaders("X-User-Id"));
        if (headers.size() != 1) throw new CurrentUserRequiredException();
        String value = headers.getFirst();
        try {
            UUID id = UUID.fromString(value);
            if (!id.toString().equalsIgnoreCase(value)) throw new CurrentUserRequiredException();
            return id;
        } catch (IllegalArgumentException exception) {
            throw new CurrentUserRequiredException();
        }
    }
}
