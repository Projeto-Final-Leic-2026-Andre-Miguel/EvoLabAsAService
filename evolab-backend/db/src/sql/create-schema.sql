-- EvoLab Database Schema


CREATE TYPE auth_provider AS ENUM ('LOCAL', 'GOOGLE');

CREATE TYPE job_status AS ENUM ('CREATED', 'QUEUED', 'RUNNING', 'COMPLETED', 'FAILED');

CREATE TYPE worker_status AS ENUM ('IDLE', 'BUSY', 'OFFLINE');

CREATE TYPE llm_provider AS ENUM ('OPENAI', 'GEMINI', 'LOCAL_MODEL');  -- Adicionar outros LLMS se necessário


CREATE TABLE users (
                       id              SERIAL          PRIMARY KEY,
                       name            VARCHAR(255)    NOT NULL,
                       email           VARCHAR(255)    NOT NULL UNIQUE,
                       password_hash   VARCHAR(255),                       -- NULL se OAuth
                       auth_provider   auth_provider   NOT NULL DEFAULT 'LOCAL',
                       provider_id     VARCHAR(255),                       -- ID do OAuth provider
                       created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);


CREATE TABLE tokens (
                        token_validation    VARCHAR(256)    PRIMARY KEY,
                        user_id             INTEGER         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        created_at          BIGINT          NOT NULL,
                        last_used_at        BIGINT          NOT NULL
);


CREATE TABLE llm_credentials (
                                 id                  SERIAL          PRIMARY KEY,
                                 user_id             INTEGER         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 provider            llm_provider    NOT NULL,
                                 api_key_encrypted   TEXT            NOT NULL,
                                 created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                 UNIQUE (user_id, provider)          -- um utilizador, uma key por provider, permitindo que não tenha de fornecer novamente informações para puder utilizar um modelo que já tinha registado antes
);                                                                   -- mas se por ventura, quiser atualizar a key, ou trocar o modelo, fazemos uma atualização.

CREATE TABLE evolution_configs (
                                   id                      SERIAL          PRIMARY KEY,
                                   user_id                 INTEGER         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                   model_provider          llm_provider    NOT NULL,
                                   model_name              VARCHAR(255)    NOT NULL,
                                   max_iterations          INTEGER         NOT NULL DEFAULT 100,
                                   checkpoint_interval     INTEGER         NOT NULL DEFAULT 10,
                                   additional_params       JSONB,
                                   created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE projects (
                          id                  SERIAL          PRIMARY KEY,
                          user_id             INTEGER         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          config_id           INTEGER         REFERENCES evolution_configs(id) ON DELETE SET NULL,
                          name                VARCHAR(255)    NOT NULL,
                          description         TEXT,
                          initial_program     TEXT            NOT NULL,       -- código inicial a evoluir
                          evaluator_code      TEXT            NOT NULL,       -- código que avalia a solução
                          status              job_status      NOT NULL DEFAULT 'CREATED',
                          created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE jobs (
                      id                  SERIAL          PRIMARY KEY,
                      project_id          INTEGER         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                      status              job_status      NOT NULL DEFAULT 'CREATED',
                      container_id        VARCHAR(255),                   -- ID do contentor Docker
                      worker_id           INTEGER,                        -- FK adicionada depois dos workers
                      started_at          TIMESTAMPTZ,
                      finished_at         TIMESTAMPTZ,
                      best_solution       TEXT,                           -- melhor programa encontrado
                      execution_logs      TEXT,
                      created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE workers (
                         id                  SERIAL          PRIMARY KEY,
                         hostname            VARCHAR(255)    NOT NULL,
                         status              worker_status   NOT NULL DEFAULT 'IDLE',
                         current_job_id      INTEGER         REFERENCES jobs(id) ON DELETE SET NULL,
                         last_heartbeat      TIMESTAMPTZ,
                         created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- FK jobs -> workers (adicionada após criar workers)
ALTER TABLE jobs
    ADD CONSTRAINT fk_jobs_worker
        FOREIGN KEY (worker_id) REFERENCES workers(id) ON DELETE SET NULL;

CREATE TABLE metrics (
                         id                  SERIAL          PRIMARY KEY,
                         job_id              INTEGER         NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
                         iteration           INTEGER         NOT NULL,
                         fitness_score       DOUBLE PRECISION NOT NULL,
                         execution_time      DOUBLE PRECISION,               -- em segundos
                         created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE checkpoints (
                             id                  SERIAL          PRIMARY KEY,
                             job_id              INTEGER         NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
                             iteration           INTEGER         NOT NULL,
                             file_path           TEXT            NOT NULL,       -- caminho para o ficheiro guardado
                             created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);


CREATE INDEX idx_tokens_user_id ON tokens(user_id);
CREATE INDEX idx_llm_credentials_user_id  ON llm_credentials(user_id);
CREATE INDEX idx_projects_user_id         ON projects(user_id);
CREATE INDEX idx_jobs_project_id          ON jobs(project_id);
CREATE INDEX idx_jobs_worker_id           ON jobs(worker_id);
CREATE INDEX idx_metrics_job_id           ON metrics(job_id);
CREATE INDEX idx_checkpoints_job_id       ON checkpoints(job_id);
