package com.ssafy.meeting.auth.service;

import com.ssafy.meeting.auth.domain.RefreshToken;
import com.ssafy.meeting.auth.dto.AuthResponse;
import com.ssafy.meeting.auth.dto.LoginRequest;
import com.ssafy.meeting.auth.dto.RegisterRequest;
import com.ssafy.meeting.auth.repository.RefreshTokenRepository;
import com.ssafy.meeting.auth.security.JwtTokenProvider;
import com.ssafy.meeting.common.exception.UnauthorizedException;
import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.member.dto.MemberResponse;
import com.ssafy.meeting.member.repository.MemberRepository;
import com.ssafy.meeting.member.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final long accessTokenSeconds;

    public AuthService(
        MemberRepository memberRepository,
        MemberService memberService,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider,
        @Value("${jwt.access-token-seconds:3600}") long accessTokenSeconds
    ) {
        this.memberRepository = memberRepository;
        this.memberService = memberService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.accessTokenSeconds = accessTokenSeconds;
    }

    @Transactional
    public MemberResponse register(RegisterRequest request) {
        Member member = memberService.register(request.getEmail(), request.getPassword(), request.getName());
        return MemberResponse.from(member);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("Login failed: member not found email={}", email);
                return new UnauthorizedException("Invalid email or password");
            });

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            log.warn("Login failed: bad credentials memberId={}", member.getId());
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getEmail());
        refreshTokenRepository.save(new RefreshToken(member, refreshToken, jwtTokenProvider.refreshTokenExpiresAt()));
        log.info("Login succeeded memberId={}", member.getId());
        return new AuthResponse(accessToken, refreshToken, accessTokenSeconds, MemberResponse.from(member));
    }

    @Transactional(readOnly = true)
    public MemberResponse me(Long memberId) {
        return memberService.getMemberResponse(memberId);
    }
}
