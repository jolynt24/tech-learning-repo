-- Sequences required by Hibernate
CREATE SEQUENCE IF NOT EXISTS users_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS daily_entries_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS meals_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS goals_SEQ START WITH 1 INCREMENT BY 50;

-- Create object table

CREATE TABLE users (
   id BIGSERIAL PRIMARY KEY,
   username VARCHAR(50) NOT NULL UNIQUE,
   email VARCHAR(255) NOT NULL UNIQUE,
   password_hash VARCHAR(255) NOT NULL,
   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    roles VARCHAR(255),
   CONSTRAINT users_username_length CHECK (char_length(username) >= 3),
   CONSTRAINT users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE TABLE daily_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entry_date DATE NOT NULL,
    sleep_hours DECIMAL(4,2) CHECK (sleep_hours >= 0 AND sleep_hours <= 24),
    sleep_quality INTEGER CHECK (sleep_quality >= 1 AND sleep_quality <= 5),
    water_ml INTEGER CHECK (water_ml >= 0),
    workout_done BOOLEAN,
    workout_type VARCHAR(100),
    workout_duration_min INTEGER CHECK (workout_duration_min >= 0),
    reading_minutes INTEGER CHECK (reading_minutes >= 0),
    reading_pages INTEGER CHECK (reading_pages >= 0),
    reading_book VARCHAR(255),
    hobby_activity VARCHAR(255),
    hobby_duration_min INTEGER CHECK (hobby_duration_min >= 0),
    mood_rating INTEGER CHECK (mood_rating >= 1 AND mood_rating <= 5),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_daily_entries_user_date UNIQUE (user_id, entry_date)
);

CREATE TABLE meals (
    id BIGSERIAL PRIMARY KEY,
    entry_id BIGINT NOT NULL REFERENCES daily_entries(id) ON DELETE CASCADE,
    meal_type VARCHAR(20) NOT NULL CHECK (meal_type IN ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK')),
    description VARCHAR(255) NOT NULL,
    calories INTEGER CHECK (calories >= 0),
    logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE goals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT  NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_type VARCHAR(20) NOT NULL CHECK ( goal_type  IN ('SLEEP', 'WATER', 'WORKOUT', 'READING', 'HOBBY' )),
    target_value DECIMAL(10,2) NOT NULL,
    frequency VARCHAR(20) NOT NULL CHECK ( frequency IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    start_date DATE NOT NULL,
    end_date DATE,
    active BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);

CREATE INDEX idx_daily_entries_user_id ON daily_entries(user_id);
CREATE INDEX idx_daily_entries_entry_date ON daily_entries(entry_date);
CREATE INDEX idx_daily_entries_user_date ON daily_entries(user_id, entry_date);
CREATE INDEX idx_daily_entries_created_at ON daily_entries(created_at);

CREATE INDEX idx_meals_entry_id  ON meals(entry_id);
CREATE INDEX idx_meals_meal_type ON meals(meal_type);
CREATE INDEX idx_meals_logged_at ON meals(logged_at);

CREATE INDEX idx_goals_user_id ON goals(user_id, active);

-- UPDATED_AT TRIGGER FUNCTION
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to users table
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_daily_entries_updated_at BEFORE UPDATE ON daily_entries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();