CREATE TABLE transcription_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    audio_file_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    error_message TEXT NULL,
    raw_response_path VARCHAR(500) NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_transcription_jobs_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_transcription_jobs_audio_file FOREIGN KEY (audio_file_id) REFERENCES audio_files (id)
);

CREATE INDEX idx_transcription_jobs_meeting_status ON transcription_jobs (meeting_id, status);

CREATE TABLE transcript_segments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transcription_job_id BIGINT NOT NULL,
    speaker VARCHAR(100) NOT NULL,
    member_id BIGINT NULL,
    start_time DOUBLE NOT NULL,
    end_time DOUBLE NOT NULL,
    text TEXT NOT NULL,
    sequence INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_transcript_segments_job FOREIGN KEY (transcription_job_id) REFERENCES transcription_jobs (id),
    CONSTRAINT fk_transcript_segments_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_transcript_segments_job_sequence ON transcript_segments (transcription_job_id, sequence);

CREATE TABLE speaker_mappings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transcription_job_id BIGINT NOT NULL,
    speaker VARCHAR(100) NOT NULL,
    member_id BIGINT NOT NULL,
    is_auto_mapped BOOLEAN NOT NULL,
    confirmed_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_speaker_mappings_job_speaker UNIQUE (transcription_job_id, speaker),
    CONSTRAINT fk_speaker_mappings_job FOREIGN KEY (transcription_job_id) REFERENCES transcription_jobs (id),
    CONSTRAINT fk_speaker_mappings_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_speaker_mappings_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES members (id)
);
