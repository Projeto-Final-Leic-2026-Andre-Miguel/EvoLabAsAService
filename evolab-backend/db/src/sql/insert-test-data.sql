-- EvoLab Test Data
-- Seed users (LOCAL + GOOGLE)
INSERT INTO users (id, name, email, password_hash, auth_provider, provider_id)
VALUES
	(1, 'Ana Silva', 'ana.local@evolab.dev', '$2a$10$exampleLocalHashAna', 'LOCAL', 'local-ana-1'),
	(2, 'Bruno Costa', 'bruno.local@evolab.dev', '$2a$10$exampleLocalHashBruno', 'LOCAL', 'local-bruno-2'),
	(3, 'Carla Gomes', 'carla.google@evolab.dev', NULL, 'GOOGLE', 'google-sub-carla-3');

-- Seed tokens for local users (epoch millis)
INSERT INTO tokens (token_validation, user_id, created_at, last_used_at)
VALUES
	('token-validation-ana-1', 1, 1760000000000, 1760000000000),
	('token-validation-bruno-2', 2, 1760000100000, 1760000200000);

-- Seed LLM credentials (one per provider per user)
INSERT INTO llm_credentials (id, user_id, provider, api_key_encrypted)
VALUES
	(1, 1, 'OPENAI', 'encOpenaiAna'),
	(2, 1, 'GEMINI', 'encGeminiAna'),
	(3, 2, 'LOCAL_MODEL', 'encLocalmodelBruno');

-- Seed evolution configs
INSERT INTO evolution_configs (
	id,
	user_id,
	llm_credential_id,
	model_name,
	max_iterations,
	checkpoint_interval,
	additional_params
)
VALUES
	(1, 1, 1, 'gpt-4o-mini', 100, 10, '{"temperature":"0.2","top_p":"1.0"}'::jsonb),
	(2, 1, 2, 'gemini-2.5-flash', 80, 8, '{"temperature":"0.4"}'::jsonb),
	(3, 2, 3, 'local-llm-v1', 60, 6, '{"quantization":"q4"}'::jsonb);

-- Seed projects
INSERT INTO projects (
	id,
	user_id,
	config_id,
	name,
	description,
	initial_program,
	evaluator_code,
	status
)
VALUES
	(1, 1, 1, 'Sorting Optimizer', 'Evolve a faster sorting strategy', 'def candidate(x): return sorted(x)', 'def score(f): return 1.0', 'CREATED'),
	(2, 1, 2, 'Route Planner', 'Improve route planning heuristics', 'def candidate(g): return g', 'def score(f): return 0.8', 'QUEUED'),
	(3, 2, 3, 'Local Tuner', 'Tune local model prompts', 'def candidate(p): return p', 'def score(f): return 0.7', 'RUNNING');

-- Seed jobs
INSERT INTO jobs (
	id,
	project_id,
	status,
	container_id,
	started_at,
	finished_at,
	best_solution,
	execution_logs
)
VALUES
	(1, 1, 'CREATED', NULL, NULL, NULL, NULL, 'Job created'),
	(2, 2, 'COMPLETED', 'container-2', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'best_solution_v2', 'Completed successfully'),
	(3, 3, 'RUNNING', 'container-3', NOW() - INTERVAL '30 minutes', NULL, NULL, 'Running iteration 12');

-- Seed metrics
INSERT INTO metrics (id, job_id, iteration, fitness_score, execution_time)
VALUES
	(1, 2, 1, 0.55, 3.4),
	(2, 2, 2, 0.71, 3.1),
	(3, 3, 1, 0.43, 2.9);

-- Seed checkpoints
INSERT INTO checkpoints (id, job_id, metrics_id, iteration, solution)
VALUES
	(1, 2, 1, 1, 'checkpoint_job2_iter1.py'),
	(2, 2, 2, 2, 'checkpoint_job2_iter2.py'),
	(3, 3, 3, 1, 'checkpoint_job3_iter1.py');

-- Keep SERIAL sequences aligned after explicit IDs
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('llm_credentials_id_seq', (SELECT MAX(id) FROM llm_credentials));
SELECT setval('evolution_configs_id_seq', (SELECT MAX(id) FROM evolution_configs));
SELECT setval('projects_id_seq', (SELECT MAX(id) FROM projects));
SELECT setval('jobs_id_seq', (SELECT MAX(id) FROM jobs));
SELECT setval('metrics_id_seq', (SELECT MAX(id) FROM metrics));
SELECT setval('checkpoints_id_seq', (SELECT MAX(id) FROM checkpoints));
