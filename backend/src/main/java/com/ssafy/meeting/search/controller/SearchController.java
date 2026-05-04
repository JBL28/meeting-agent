package com.ssafy.meeting.search.controller;

import com.ssafy.meeting.auth.security.UserPrincipal;
import com.ssafy.meeting.common.api.ApiResponse;
import com.ssafy.meeting.search.dto.MeetingSearchResult;
import com.ssafy.meeting.search.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/teams/{teamId}/meetings/search")
    public ApiResponse<Page<MeetingSearchResult>> search(
        @PathVariable Long teamId,
        @RequestParam String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(searchService.search(teamId, principal.getId(), keyword, PageRequest.of(page, size)));
    }
}
