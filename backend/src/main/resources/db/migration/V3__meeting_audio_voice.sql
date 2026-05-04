CREATE TABLE voice_samples (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    duration_seconds INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consent_agreed_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_voice_samples_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_voice_samples_team FOREIGN KEY (team_id) REFERENCES teams (id)
);

CREATE TABLE meetings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    title VARCHAR(300) NOT NULL,
    scheduled_at DATETIME NULL,
    status VARCHAR(40) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_meetings_team FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT fk_meetings_created_by FOREIGN KEY (created_by) REFERENCES members (id)
);

CREATE TABLE meeting_participants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_meeting_participants_meeting_member UNIQUE (meeting_id, member_id),
    CONSTRAINT fk_meeting_participants_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_meeting_participants_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE audio_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    duration_seconds INT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_audio_files_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id)
);
