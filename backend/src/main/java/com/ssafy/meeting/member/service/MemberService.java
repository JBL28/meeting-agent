package com.ssafy.meeting.member.service;

import com.ssafy.meeting.common.exception.ResourceNotFoundException;
import com.ssafy.meeting.common.exception.ValidationException;
import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.member.dto.MemberResponse;
import com.ssafy.meeting.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Member register(String email, String rawPassword, String name) {
        String normalizedEmail = email.toLowerCase().trim();
        if (memberRepository.existsByEmail(normalizedEmail)) {
            log.warn("Register failed: duplicate email={}", normalizedEmail);
            throw new ValidationException("Email is already registered");
        }
        Member member = memberRepository.save(new Member(normalizedEmail, passwordEncoder.encode(rawPassword), name.trim()));
        log.info("Member registered memberId={} email={}", member.getId(), member.getEmail());
        return member;
    }

    @Transactional(readOnly = true)
    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }

    @Transactional(readOnly = true)
    public MemberResponse getMemberResponse(Long memberId) {
        return MemberResponse.from(findById(memberId));
    }
}
