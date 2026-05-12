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
                       provider_id     VARCHAR(255)    NOT NULL DEFAULT 'LOCAL',                       -- ID do OAuth provider
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

CREATE TABLE local_model_credentials (
                                 credential_id       INTEGER         PRIMARY KEY REFERENCES llm_credentials(id) ON DELETE CASCADE,
                                 port                INTEGER         NOT NULL,
                                 model_name          VARCHAR(255)    NOT NULL
);

CREATE TABLE evolution_configs (
                                   id                      SERIAL          PRIMARY KEY,
                                   user_id                 INTEGER         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                   llm_credential_id       INTEGER         NOT NULL REFERENCES llm_credentials(id) ON DELETE RESTRICT,
                                   model_name              VARCHAR(255)    NOT NULL,
                                   max_iterations          INTEGER         NOT NULL DEFAULT 100,
                                   checkpoint_interval     INTEGER         NOT NULL DEFAULT 10,
                                   additional_params       JSONB,
                                   created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE projects (
                          id                  SERIAL          PRIMARY KEY,
                          user_id             INTEGER         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          config_id           INTEGER         DEFAULT NULL REFERENCES evolution_configs(id) ON DELETE SET NULL,
                          name                VARCHAR(255)    NOT NULL,
                          description         TEXT,
                          initial_program     TEXT            ,       -- código inicial a evoluir
                          evaluator_code      TEXT            ,       -- código que avalia a solução
                          status              job_status      NOT NULL DEFAULT 'CREATED',
                          created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE jobs (
                      id                  SERIAL          PRIMARY KEY,
                      project_id          INTEGER         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                      status              job_status      NOT NULL DEFAULT 'CREATED',
                      container_id        VARCHAR(255),                   -- ID do contentor Docker
                      started_at          TIMESTAMPTZ,
                      finished_at         TIMESTAMPTZ,
                      best_solution       TEXT,                           -- melhor programa encontrado
                      execution_logs      TEXT,
                      failure_reason      TEXT,
                      created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

ALTER TABLE jobs ADD COLUMN IF NOT EXISTS failure_reason TEXT;


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
                             metrics_id          INTEGER         NOT NULL REFERENCES metrics(id) ON DELETE CASCADE,
                             iteration           INTEGER         NOT NULL,
                             solution            TEXT            NOT NULL,       -- caminho para o ficheiro guardado
                             created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);


CREATE INDEX idx_tokens_user_id ON tokens(user_id);
CREATE INDEX idx_llm_credentials_user_id  ON llm_credentials(user_id);
CREATE INDEX idx_projects_user_id         ON projects(user_id);
CREATE INDEX idx_jobs_project_id          ON jobs(project_id);
--CREATE INDEX idx_jobs_worker_id           ON jobs(worker_id);
CREATE INDEX idx_metrics_job_id           ON metrics(job_id);
CREATE INDEX idx_checkpoints_job_id       ON checkpoints(job_id);
