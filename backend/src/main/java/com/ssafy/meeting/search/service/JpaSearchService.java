package com.ssafy.meeting.search.service;

import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.repository.MeetingRepository;
import com.ssafy.meeting.minutes.domain.ActionItem;
import com.ssafy.meeting.minutes.domain.MeetingMinutes;
import com.ssafy.meeting.minutes.repository.ActionItemRepository;
import com.ssafy.meeting.minutes.repository.MeetingMinutesRepository;
import com.ssafy.meeting.search.dto.MeetingSearchResult;
import com.ssafy.meeting.team.domain.TeamRole;
import com.ssafy.meeting.team.service.TeamPermissionEvaluator;
import com.ssafy.meeting.transcription.domain.TranscriptSegment;
import com.ssafy.meeting.transcription.repository.TranscriptSegmentRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class JpaSearchService implements SearchService {

    private final TeamPermissionEvaluator permissionEvaluator;
    private final MeetingRepository meetingRepository;
    private final MeetingMinutesRepository minutesRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final ActionItemRepository actionItemRepository;

    public JpaSearchService(
        TeamPermissionEvaluator permissionEvaluator,
        MeetingRepository meetingRepository,
        MeetingMinutesRepository minutesRepository,
        TranscriptSegmentRepository segmentRepository,
        ActionItemRepository actionItemRepository
    ) {
        this.permissionEvaluator = permissionEvaluator;
        this.meetingRepository = meetingRepository;
        this.minutesRepository = minutesRepository;
        this.segmentRepository = segmentRepository;
        this.actionItemRepository = actionItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MeetingSearchResult> search(Long teamId, Long requesterId, String keyword, Pageable pageable) {
        permissionEvaluator.requireRole(teamId, requesterId, TeamRole.VIEWER);
        if (!StringUtils.hasText(keyword)) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
        Map<Long, MeetingSearchResult> results = new LinkedHashMap<>();
        for (Meeting meeting : meetingRepository.findByTeamIdAndTitleContainingIgnoreCase(teamId, keyword)) {
            results.put(meeting.getId(), result(meeting, snippet(meeting.getTitle(), keyword)));
        }
        for (MeetingMinutes minutes : minutesRepository.findByMeetingTeamIdAndFullSummaryContainingIgnoreCase(teamId, keyword)) {
            results.putIfAbsent(minutes.getMeeting().getId(), result(minutes.getMeeting(), snippet(minutes.getFullSummary(), keyword)));
        }
        for (TranscriptSegment segment : segmentRepository.findByTranscriptionJobMeetingTeamIdAndTextContainingIgnoreCase(teamId, keyword)) {
            Meeting meeting = segment.getTranscriptionJob().getMeeting();
            results.putIfAbsent(meeting.getId(), result(meeting, snippet(segment.getText(), keyword)));
        }
        for (ActionItem item : actionItemRepository.findByMinutesMeetingTeamIdAndContentContainingIgnoreCase(teamId, keyword)) {
            Meeting meeting = item.getMinutes().getMeeting();
            results.putIfAbsent(meeting.getId(), result(meeting, snippet(item.getContent(), keyword)));
        }
        List<MeetingSearchResult> all = new ArrayList<>(results.values());
        int start = (int) Math.min(pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }

    private MeetingSearchResult result(Meeting meeting, String snippet) {
        return new MeetingSearchResult(meeting.getId(), meeting.getTitle(), meeting.getStatus(), snippet, meeting.getCreatedAt());
    }

    private String snippet(String text, String keyword) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase();
        String key = keyword.toLowerCase();
        int index = lower.indexOf(key);
        if (index < 0) {
            return text.length() <= 100 ? text : text.substring(0, 100);
        }
        int start = Math.max(0, index - 50);
        int end = Math.min(text.length(), index + keyword.length() + 50);
        return text.substring(start, end);
    }
}
