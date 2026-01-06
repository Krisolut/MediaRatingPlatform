CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    email TEXT,
    favorite_genre TEXT,
    total_ratings INT DEFAULT 0,
    average_given_rating DOUBLE PRECISION DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS media_entries (
    id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    media_type TEXT NOT NULL,
    release_year INT,
    age_restriction TEXT,
    genres TEXT,
    creator_user_id INT REFERENCES users(id),
    average_rating DOUBLE PRECISION DEFAULT 0,
    rating_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ratings (
    id SERIAL PRIMARY KEY,
    media_id INT REFERENCES media_entries(id) ON DELETE CASCADE,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    stars INT NOT NULL,
    comment TEXT,
    is_confirmed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    activity_count INT DEFAULT 1,
    UNIQUE(user_id, media_id)
);

CREATE TABLE IF NOT EXISTS rating_likes (
    id SERIAL PRIMARY KEY,
    rating_id INT REFERENCES ratings(id) ON DELETE CASCADE,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(rating_id, user_id)
);

CREATE TABLE IF NOT EXISTS favorites (
    id SERIAL PRIMARY KEY,
    media_id INT REFERENCES media_entries(id) ON DELETE CASCADE,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(media_id, user_id)
);

CREATE TABLE IF NOT EXISTS tokens (
    token TEXT PRIMARY KEY,
    user_id INT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_media_creator ON media_entries (creator_user_id);
CREATE INDEX IF NOT EXISTS idx_rating_media ON ratings (media_id);
CREATE INDEX IF NOT EXISTS idx_rating_user ON ratings (user_id);
CREATE INDEX IF NOT EXISTS idx_favorite_user ON favorites (user_id);
CREATE INDEX IF NOT EXISTS idx_token_user ON tokens (user_id);