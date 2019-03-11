package com.gederin.util;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class UserContext {
    public static final String CORRELATION_ID = "correlation-id";
    public static final String AUTH_TOKEN     = "auth-token";
    public static final String USER_ID        = "user-id";
    public static final String ORG_ID         = "org-id";

    private String correlationId= "";
    private String authToken= "";
    private String userId = "";
    private String orgId = "";
}