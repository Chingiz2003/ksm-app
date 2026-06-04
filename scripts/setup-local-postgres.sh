#!/usr/bin/env sh
set -eu

DB_NAME="${DB_NAME:-ksm_fruit}"
DB_USER="${DB_USER:-ksm}"
DB_PASSWORD="${DB_PASSWORD:-ksm}"
ADMIN_DB="${ADMIN_DB:-postgres}"

psql -d "$ADMIN_DB" -v ON_ERROR_STOP=1 \
  -v db_name="$DB_NAME" \
  -v db_user="$DB_USER" \
  -v db_password="$DB_PASSWORD" <<'SQL'
DO
$$
BEGIN
   IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'db_user') THEN
      EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', :'db_user', :'db_password');
   ELSE
      EXECUTE format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'db_user', :'db_password');
   END IF;
END
$$;

SELECT format('CREATE DATABASE %I OWNER %I', :'db_name', :'db_user')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db_name')
\gexec

ALTER DATABASE :"db_name" OWNER TO :"db_user";
GRANT ALL PRIVILEGES ON DATABASE :"db_name" TO :"db_user";
SQL

echo "PostgreSQL database is ready: $DB_NAME / $DB_USER"
