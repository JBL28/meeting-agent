package com.ssafy.meeting.search.service;

import com.ssafy.meeting.search.dto.MeetingSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchService {
    Page<MeetingSearchResult> search(Long teamId, Long requesterId, String keyword, Pageable pageable);
}
