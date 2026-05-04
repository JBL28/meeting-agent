package com.ssafy.meeting.member.dto;

import com.ssafy.meeting.member.domain.Member;

public class MemberResponse {
    private final Long id;
    private final String email;
    private final String name;

    public MemberResponse(Long id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getName());
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
}
