CREATE TABLE availability_preferences (
 id UUID PRIMARY KEY,
 owner_id TEXT NOT NULL UNIQUE,
 zone_id TEXT NOT NULL CHECK (length(zone_id)>0),
 monday_minutes INTEGER NOT NULL CHECK (monday_minutes BETWEEN 0 AND 1440),
 tuesday_minutes INTEGER NOT NULL CHECK (tuesday_minutes BETWEEN 0 AND 1440),
 wednesday_minutes INTEGER NOT NULL CHECK (wednesday_minutes BETWEEN 0 AND 1440),
 thursday_minutes INTEGER NOT NULL CHECK (thursday_minutes BETWEEN 0 AND 1440),
 friday_minutes INTEGER NOT NULL CHECK (friday_minutes BETWEEN 0 AND 1440),
 saturday_minutes INTEGER NOT NULL CHECK (saturday_minutes BETWEEN 0 AND 1440),
 sunday_minutes INTEGER NOT NULL CHECK (sunday_minutes BETWEEN 0 AND 1440),
 version BIGINT NOT NULL CHECK (version>=0),
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL CHECK (updated_at>=created_at)
);
