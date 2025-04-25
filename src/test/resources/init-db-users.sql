CREATE ROLE "platform_owner" LOGIN PASSWORD 'platform' NOINHERIT;
GRANT CREATE ON DATABASE "platform" TO "platform_owner";
CREATE EXTENSION pg_stat_statements;
