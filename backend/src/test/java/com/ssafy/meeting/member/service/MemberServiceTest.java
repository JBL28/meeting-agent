package com.ssafy.meeting.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.meeting.common.exception.ValidationException;
import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        when(memberRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member member = memberService.register(" User@Example.COM ", "password123", " ??? ");

        assertThat(member.getEmail()).isEqualTo("user@example.com");
        assertThat(member.getPassword()).isEqualTo("hashed");
        assertThat(member.getName()).isEqualTo("???");
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(memberRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.register("user@example.com", "password123", "User"))
            .isInstanceOf(ValidationException.class);
    }
}
