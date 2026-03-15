FROM postgres:16

USER postgres
WORKDIR /app

# Copy SQL scripts — these run automatically on first container start
COPY src/sql/create-schema.sql /docker-entrypoint-initdb.d/1_create.sql
COPY src/sql/insert-test-data.sql /docker-entrypoint-initdb.d/2_insert-test-data.sql

# Copy and set permissions on the wait script
COPY --chown=postgres:postgres docker/scripts/wait-for-postgres.sh ./bin/wait-for-postgres.sh
RUN chmod +x ./bin/wait-for-postgres.sh

EXPOSE 5432

