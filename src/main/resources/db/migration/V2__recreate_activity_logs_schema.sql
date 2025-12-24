DROP TABLE IF EXISTS activity_logs;

CREATE TABLE activity_logs (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    action VARCHAR(255) NOT NULL,
    description TEXT,
    entity_id BINARY(16),         
    entity_type VARCHAR(255),
    user_id BINARY(16),
    PRIMARY KEY (id)
);

ALTER TABLE activity_logs 
ADD CONSTRAINT fk_activity_logs_users 
FOREIGN KEY (user_id) REFERENCES users (id);
