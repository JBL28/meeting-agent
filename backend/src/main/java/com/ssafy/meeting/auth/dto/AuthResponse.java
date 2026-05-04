package com.ssafy.meeting.auth.dto;

import com.ssafy.meeting.member.dto.MemberResponse;

public class AuthResponse {
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresIn;
    private final MemberResponse member;

    public AuthResponse(String accessToken, String refreshToken, long expiresIn, MemberResponse member) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.member = member;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public MemberResponse getMember() { return member; }
}
