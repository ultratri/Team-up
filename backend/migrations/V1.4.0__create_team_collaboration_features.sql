-- Create team collaboration enhancement features
-- Version: V1.4.0
-- Date: 2026-02-13
-- Description: Add Sprint management, file version history, daily standup records

-- 1. Create Sprint management table
CREATE TABLE IF NOT EXISTS sprints (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    goal TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status ENUM('PLANNING', 'IN_PROGRESS', 'COMPLETED') DEFAULT 'PLANNING',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_team (team_id),
    INDEX idx_status (status),
    INDEX idx_dates (start_date, end_date),
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Add sprint_id field to tasks table
ALTER TABLE tasks 
ADD COLUMN sprint_id BIGINT NULL,
ADD INDEX idx_sprint (sprint_id);

-- 3. Create daily standup records table
CREATE TABLE IF NOT EXISTS daily_standups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    sprint_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    standup_date DATE NOT NULL,
    yesterday_work TEXT,
    today_plan TEXT,
    blockers TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_team_date (team_id, standup_date),
    INDEX idx_sprint (sprint_id),
    INDEX idx_user (user_id),
    UNIQUE KEY uk_team_user_date (team_id, user_id, standup_date),
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Create Sprint retrospective meeting records table
CREATE TABLE IF NOT EXISTS sprint_retrospectives (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sprint_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    what_went_well TEXT,
    what_to_improve TEXT,
    action_items TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sprint (sprint_id),
    INDEX idx_team (team_id),
    FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE CASCADE,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Create file version history table
CREATE TABLE IF NOT EXISTS file_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    change_description VARCHAR(500),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_file (file_id),
    INDEX idx_version (file_id, version_number),
    INDEX idx_uploaded_at (uploaded_at),
    FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Create file activity records table
CREATE TABLE IF NOT EXISTS file_activities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    activity_type ENUM('UPLOAD', 'DOWNLOAD', 'UPDATE', 'DELETE', 'RENAME', 'MOVE') NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_file (file_id),
    INDEX idx_user (user_id),
    INDEX idx_type (activity_type),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Add team_id field to conversations table
ALTER TABLE conversations 
ADD COLUMN team_id BIGINT NULL,
ADD INDEX idx_team (team_id);

-- 8. Create message read status table
CREATE TABLE IF NOT EXISTS message_read_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_user (message_id, user_id),
    INDEX idx_message (message_id),
    INDEX idx_user (user_id),
    FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. Add mentioned users field to chat_messages table
ALTER TABLE chat_messages 
ADD COLUMN mentioned_users JSON NULL;

-- 10. Create team settings table
CREATE TABLE IF NOT EXISTS team_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_team_key (team_id, setting_key),
    INDEX idx_team (team_id),
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
