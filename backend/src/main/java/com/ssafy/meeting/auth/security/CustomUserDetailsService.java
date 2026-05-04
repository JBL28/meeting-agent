package com.ssafy.meeting.auth.security;

import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.member.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    public CustomUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("Member not found"));
        return UserPrincipal.from(member);
    }

    @Transactional(readOnly = true)
    public UserPrincipal loadUserById(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new UsernameNotFoundException("Member not found"));
        return UserPrincipal.from(member);
    }
}
