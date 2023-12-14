package com.efs.sdk.metadata.helper;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Objects;

public class Utils {

    private Utils() {
        // do nothing
    }

    /**
     * Get Subject out of Auth-Token from Security-Context
     *
     * @return Auth-Token
     */
    public static String getSubject() {
        return Objects.requireNonNull(((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken(), "JWT token of " +
                "SecurityContextHolder should not be null").getSubject();
    }

    /**
     * Get JwtAuthorizationToken from Security-Context
     *
     * @return JwtAuthenticationToken
     */
    public static JwtAuthenticationToken getSubjectAsToken() {
        return Objects.requireNonNull(((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()), "JWT token of " +
                "SecurityContextHolder should not be null");
    }
}
