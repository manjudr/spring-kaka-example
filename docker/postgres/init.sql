-- Initialize catalog database
\echo 'Creating catalog database schema...'

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set timezone
SET timezone = 'UTC';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE catalog_db TO catalog_user;

\echo 'Database initialization completed.'
