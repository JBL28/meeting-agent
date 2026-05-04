CREATE TABLE minutes_generation_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    error_message TEXT NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_minutes_generation_jobs_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id)
);

CREATE INDEX idx_minutes_generation_jobs_meeting_status ON minutes_generation_jobs (meeting_id, status);

CREATE TABLE meeting_minutes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    generation_job_id BIGINT NOT NULL,
    title VARCHAR(300) NOT NULL,
    meeting_date DATE NULL,
    full_summary TEXT NOT NULL,
    raw_content LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_meeting_minutes_meeting UNIQUE (meeting_id),
    CONSTRAINT fk_meeting_minutes_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_meeting_minutes_generation_job FOREIGN KEY (generation_job_id) REFERENCES minutes_generation_jobs (id)
);

CREATE TABLE action_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    minutes_id BIGINT NOT NULL,
    assignee_id BIGINT NULL,
    content TEXT NOT NULL,
    due_date DATE NULL,
    status VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_action_items_minutes FOREIGN KEY (minutes_id) REFERENCES meeting_minutes (id),
    CONSTRAINT fk_action_items_assignee FOREIGN KEY (assignee_id) REFERENCES members (id)
);

CREATE TABLE member_minutes_summaries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    minutes_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    progress TEXT NOT NULL,
    issues TEXT NOT NULL,
    next_tasks TEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_member_minutes_summaries_minutes FOREIGN KEY (minutes_id) REFERENCES meeting_minutes (id),
    CONSTRAINT fk_member_minutes_summaries_member FOREIGN KEY (member_id) REFERENCES members (id)
);
