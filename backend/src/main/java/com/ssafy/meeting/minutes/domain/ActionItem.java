package com.ssafy.meeting.minutes.domain;

import com.ssafy.meeting.member.domain.Member;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "action_items")
public class ActionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "minutes_id", nullable = false)
    private MeetingMinutes minutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Member assignee;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActionItemStatus status;

    protected ActionItem() {
    }

    public ActionItem(MeetingMinutes minutes, Member assignee, String content, LocalDate dueDate) {
        this.minutes = minutes;
        this.assignee = assignee;
        this.content = content;
        this.dueDate = dueDate;
        this.status = ActionItemStatus.TODO;
    }

    public void update(Member assignee, String content, LocalDate dueDate) {
        this.assignee = assignee;
        this.content = content;
        this.dueDate = dueDate;
    }

    public void changeStatus(ActionItemStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public MeetingMinutes getMinutes() { return minutes; }
    public Member getAssignee() { return assignee; }
    public String getContent() { return content; }
    public LocalDate getDueDate() { return dueDate; }
    public ActionItemStatus getStatus() { return status; }
}
