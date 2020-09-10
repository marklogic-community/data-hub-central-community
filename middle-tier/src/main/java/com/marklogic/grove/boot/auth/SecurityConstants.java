package com.marklogic.grove.boot.auth;

public final class SecurityConstants {

    public static final String JWT_SECRET = "C31FB258ACC98BAC66AE97C59E86B2F798F6A0387855B30C09DFB692007001D5588C2A7CBBFEBDC44E6ED3685269893B5BEEDA468FA1B5E6E970DCB5A14E138B";

    // JWT token defaults
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE = "JWT";

    private SecurityConstants() {
        throw new IllegalStateException("Cannot create instance of static util class");
    }
}
