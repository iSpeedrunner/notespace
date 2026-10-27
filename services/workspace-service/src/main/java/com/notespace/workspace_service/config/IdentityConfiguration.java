package com.notespace.workspace_service.config;

import com.notespace.workspace_service.exception.CurrentUserRequiredException;
import com.notespace.workspace_service.service.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
public class IdentityConfiguration {
    @Bean
    @Profile("(local | test) & !prod & !production")
    CurrentUserProvider localCurrentUserProvider(ObjectProvider<HttpServletRequest> requests) {
        return new HeaderCurrentUserProvider(requests);
    }

    @Bean
    @Profile("(!local & !test) | prod | production")
    @ConditionalOnMissingBean(CurrentUserProvider.class)
    CurrentUserProvider unavailableCurrentUserProvider() {
        return () -> { throw new CurrentUserRequiredException(); };
    }
}
