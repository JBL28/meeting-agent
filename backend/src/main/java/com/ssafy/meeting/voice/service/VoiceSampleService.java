package com.ssafy.meeting.voice.service;

import com.ssafy.meeting.common.exception.ForbiddenException;
import com.ssafy.meeting.common.exception.ResourceNotFoundException;
import com.ssafy.meeting.common.exception.ValidationException;
import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.member.repository.MemberRepository;
import com.ssafy.meeting.storage.AudioDurationProbe;
import com.ssafy.meeting.storage.FilePolicyValidator;
import com.ssafy.meeting.storage.StorageService;
import com.ssafy.meeting.team.domain.Team;
import com.ssafy.meeting.team.domain.TeamMember;
import com.ssafy.meeting.team.domain.TeamRole;
import com.ssafy.meeting.team.repository.TeamRepository;
import com.ssafy.meeting.team.service.TeamPermissionEvaluator;
import com.ssafy.meeting.voice.domain.VoiceSample;
import com.ssafy.meeting.voice.dto.VoiceSampleResponse;
import com.ssafy.meeting.voice.repository.VoiceSampleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class VoiceSampleService {

    private final VoiceSampleRepository voiceSampleRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final TeamPermissionEvaluator permissionEvaluator;
    private final StorageService storageService;
    private final FilePolicyValidator filePolicyValidator;
    private final AudioDurationProbe audioDurationProbe;

    public VoiceSampleService(
        VoiceSampleRepository voiceSampleRepository,
        MemberRepository memberRepository,
        TeamRepository teamRepository,
        TeamPermissionEvaluator permissionEvaluator,
        StorageService storageService,
        FilePolicyValidator filePolicyValidator,
        AudioDurationProbe audioDurationProbe
    ) {
        this.voiceSampleRepository = voiceSampleRepository;
        this.memberRepository = memberRepository;
        this.teamRepository = teamRepository;
        this.permissionEvaluator = permissionEvaluator;
        this.storageService = storageService;
        this.filePolicyValidator = filePolicyValidator;
        this.audioDurationProbe = audioDurationProbe;
    }

    @Transactional
    public VoiceSampleResponse upload(Long teamId, Long memberId, Long requesterId, boolean consent, MultipartFile file) {
        if (!consent) {
            log.warn("Voice sample upload rejected: consent missing teamId={} memberId={} requesterId={}", teamId, memberId, requesterId);
            throw new ValidationException("Voice sample consent is required");
        }
        TeamMember requesterMembership = permissionEvaluator.requireRole(teamId, requesterId, TeamRole.MEMBER);
        boolean ownSample = requesterId.equals(memberId);
        if (!ownSample && !requesterMembership.getRole().atLeast(TeamRole.ADMIN)) {
            throw new ForbiddenException("Only self or admin can upload voice samples");
        }
        permissionEvaluator.requireRole(teamId, memberId, TeamRole.VIEWER);
        filePolicyValidator.validateVoiceSample(file);

        String filePath = storageService.save(file, "voice-samples");
        Integer durationSeconds = null;
        try {
            OptionalInt duration = audioDurationProbe.probeSeconds(filePath);
            if (duration.isPresent()) {
                durationSeconds = duration.getAsInt();
                if (durationSeconds < 2 || durationSeconds > 10) {
                    throw new ValidationException("Voice sample duration must be 2 to 10 seconds");
                }
                log.info("Voice sample duration validated teamId={} memberId={} seconds={}", teamId, memberId, durationSeconds);
            } else {
                log.warn("Voice sample duration skipped because ffprobe is unavailable teamId={} memberId={} fileName={}", teamId, memberId, file.getOriginalFilename());
            }
            Member member = findMember(memberId);
            Team team = findTeam(teamId);
            VoiceSample sample = voiceSampleRepository.save(new VoiceSample(member, team, filePath, file.getOriginalFilename(), durationSeconds, LocalDateTime.now()));
            log.info("Voice sample uploaded sampleId={} teamId={} memberId={} requesterId={}", sample.getId(), teamId, memberId, requesterId);
            return VoiceSampleResponse.from(sample);
        } catch (RuntimeException exception) {
            storageService.delete(filePath);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<VoiceSampleResponse> list(Long teamId, Long memberId, Long requesterId) {
        permissionEvaluator.requireRole(teamId, requesterId, TeamRole.MEMBER);
        return voiceSampleRepository.findAllByTeamIdAndMemberIdOrderByCreatedAtDesc(teamId, memberId).stream()
            .map(VoiceSampleResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long sampleId, Long requesterId) {
        VoiceSample sample = voiceSampleRepository.findWithMemberAndTeamById(sampleId)
            .orElseThrow(() -> new ResourceNotFoundException("Voice sample not found"));
        if (!sample.getMember().getId().equals(requesterId)) {
            throw new ForbiddenException("Only the owner can delete voice samples");
        }
        voiceSampleRepository.delete(sample);
        storageService.delete(sample.getFilePath());
        log.info("Voice sample deleted sampleId={} requesterId={}", sampleId, requesterId);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }

    private Team findTeam(Long teamId) {
        return teamRepository.findById(teamId)
            .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
    }
}
