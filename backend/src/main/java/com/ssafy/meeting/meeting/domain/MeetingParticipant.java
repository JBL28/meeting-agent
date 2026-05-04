package com.ssafy.meeting.meeting.domain;

import com.ssafy.meeting.member.domain.Member;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "meeting_participants", uniqueConstraints = @UniqueConstraint(name = "uk_meeting_participants_meeting_member", columnNames = {"meeting_id", "member_id"}))
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    protected MeetingParticipant() {
    }

    public MeetingParticipant(Meeting meeting, Member member) {
        this.meeting = meeting;
        this.member = member;
    }

    public Long getId() { return id; }
    public Meeting getMeeting() { return meeting; }
    public Member getMember() { return member; }
}
