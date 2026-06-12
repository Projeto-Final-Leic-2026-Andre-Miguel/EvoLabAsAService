import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link } from 'react-router-dom';
import styles from './Configs.module.css';
import { apiConfigs, type Config, type CreateConfigInput, type UpdateConfigInput } from './apiConfigs';
import { apiCredentials } from '../credentials/apiCredentials';
import type { LLM, LLMCredentials } from '../../types/credentials';
import { useValidCredentials } from '../../contexts/ValidCredentialsContext';
import { getErrorMessage } from '../../utils/errorsDescriptions';
import { apiProjects, type Project } from '../projects/apiProjects';
import { Alert } from '../../components/ui/Alert';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { LoadingSpinner } from '../../components/ui/LoadingSpinner';
import { Modal } from '../../components/ui/Modal';
import { getCredentialLabel } from '../../utils/credentialLabels';
import { useToast } from '../../hooks/useToast';
import { usePageTitle } from '../../hooks/usePageTitle';
import { validateConfigValues } from '../../utils/configValidation';

const defaultAdvancedParams = (): Record<string, string> => ({
  'llm.temperature': '',
  'llm.top_p': '',
  'llm.max_tokens': '',
  'llm.timeout': '',
  'llm.retries': '',
  'prompt.system_message': '',
  'prompt.num_top_programs': '',
  'prompt.num_diverse_programs': '',
  'prompt.include_artifacts': '',
  'database.population_size': '',
  'database.archive_size': '',
  'database.num_islands': '',
  'database.migration_interval': '',
  'database.migration_rate': '',
  'database.elite_selection_ratio': '',
  'database.exploration_ratio': '',
  'database.exploitation_ratio': '',
  'database.feature_dimensions': '',
  'database.feature_bins': '',
  'evaluator.timeout': '',
  'evaluator.max_retries': '',
  'evaluator.parallel_evaluations': '',
  'evaluator.cascade_evaluation': '',
  'evaluator.cascade_threshold_1': '',
  'evaluator.cascade_threshold_2': '',
  'evaluator.cascade_threshold_3': '',
  'diff_based_evolution': '',
});

const buildAdditionalParams = (params: Record<string, string>): Record<string, string> => {
  const result: Record<string, string> = {};
  for (const [key, value] of Object.entries(params)) {
    if (value.trim() !== '') result[key] = value.trim();
  }
  return result;
};

const loadAdvancedParams = (existing: Record<string, string>): Record<string, string> => {
  const loaded = defaultAdvancedParams();
  for (const [key, value] of Object.entries(existing)) {
    if (key in loaded) loaded[key] = value;
  }
  return loaded;
};

const sortedStringify = (params: Record<string, string>): string => {
  const sorted = Object.keys(params).sort().reduce<Record<string, string>>((acc, key) => {
    acc[key] = params[key];
    return acc;
  }, {});
  return JSON.stringify(sorted);
};

const OPENAI_MODELS = ['gpt-4.1-mini', 'gpt-4o', 'gpt-4o-mini', 'o1-mini', 'o1-preview'];
const GEMINI_MODELS = ['gemini-3.1-flash-lite', 'gemini-2.5-pro', 'gemini-2.5-flash', 'gemini-2.5-flash-lite'];
const ANTHROPIC_MODELS = ['claude-3-haiku-20240307', 'claude-3-5-haiku-20241022', 'claude-3-5-sonnet-20241022', 'claude-3-7-sonnet-20250219'];

const CUSTOM_MODEL_SENTINEL = '__custom__';
const MODEL_NAME_REGEX = /^[a-zA-Z0-9._\-/]+$/;
const HIDDEN_ADDITIONAL_PARAMS = new Set(['llm.api_base']);

const PARAM_DESCRIPTIONS: Record<string, string> = {
  'llm.temperature': 'Controls creativity. Lower values are more deterministic; higher values explore more varied solutions.',
  'llm.top_p': 'Limits token sampling to the most likely cumulative probability mass.',
  'llm.max_tokens': 'Maximum number of tokens the model can produce per response.',
  'llm.timeout': 'Maximum wait time for each model request, in seconds.',
  'llm.retries': 'Number of retry attempts after transient model request failures.',
  'prompt.num_top_programs': 'How many of the best current programs are shown to the model.',
  'prompt.num_diverse_programs': 'How many diverse programs are included to preserve exploration.',
  'prompt.include_artifacts': 'Includes generated artifacts in prompts when available.',
  'prompt.system_message': 'System instruction that guides the model during evolution.',
  'database.population_size': 'Number of candidate programs kept in the active population.',
  'database.archive_size': 'Number of historical candidates retained for reuse.',
  'database.num_islands': 'Splits evolution into semi-independent populations.',
  'database.migration_interval': 'How often candidates move between islands.',
  'database.migration_rate': 'Fraction of candidates exchanged during migration.',
  'database.elite_selection_ratio': 'Share of top performers preserved between generations.',
  'database.exploration_ratio': 'Budget share spent trying novel candidate directions.',
  'database.exploitation_ratio': 'Budget share spent improving known strong candidates.',
  'database.feature_bins': 'Number of buckets used to organize behavioral diversity.',
  'database.feature_dimensions': 'Feature names used to measure diversity, separated by commas.',
  'evaluator.timeout': 'Maximum runtime for evaluating a candidate solution.',
  'evaluator.max_retries': 'Retry attempts when candidate evaluation fails transiently.',
  'evaluator.parallel_evaluations': 'Number of candidate evaluations that may run concurrently.',
  'evaluator.cascade_evaluation': 'Runs cheaper checks first before more expensive evaluation stages.',
  'evaluator.cascade_threshold_1': 'Minimum score needed to pass the first cascade stage.',
  'evaluator.cascade_threshold_2': 'Minimum score needed to pass the second cascade stage.',
  'evaluator.cascade_threshold_3': 'Minimum score needed to pass the final cascade stage.',
  'diff_based_evolution': 'Evolves patches/diffs instead of full programs where supported.',
};

function isPredefinedModel(llm: LLM | undefined, value: string): boolean {
  if (!llm || !value) return false;
  if (llm === 'OPENAI') return OPENAI_MODELS.includes(value);
  if (llm === 'GEMINI') return GEMINI_MODELS.includes(value);
  if (llm === 'ANTHROPIC') return ANTHROPIC_MODELS.includes(value);
  return true;
}

function placeholderForProvider(llm: LLM | undefined): string {
  if (llm === 'OPENAI') return 'e.g., gpt-4.1-mini';
  if (llm === 'GEMINI') return 'e.g., gemini-2.5-pro';
  if (llm === 'ANTHROPIC') return 'e.g., claude-3-5-sonnet-20241022';
  return 'e.g., model-id';
}

function visibleAdditionalParams(params: Record<string, string>): Record<string, string> {
  return Object.entries(params).reduce<Record<string, string>>((acc, [key, value]) => {
    if (!HIDDEN_ADDITIONAL_PARAMS.has(key)) acc[key] = value;
    return acc;
  }, {});
}

function ParamLabel({ paramKey, children }: { paramKey: string; children: React.ReactNode }) {
  return (
    <span className={styles.paramLabel}>
      <span>{children}</span>
      <span className={styles.helpBadge} title={PARAM_DESCRIPTIONS[paramKey] || 'Advanced OpenEvolve parameter.'}>?</span>
    </span>
  );
}

type NumericParamFieldProps = {
  paramKey: string;
  label: string;
  placeholder: string;
  value: string;
  error?: string;
  min?: number;
  max?: number;
  step?: string;
  onChange: (value: string) => void;
};

function NumericParamField({
  paramKey,
  label,
  placeholder,
  value,
  error,
  min,
  max,
  step,
  onChange,
}: NumericParamFieldProps) {
  const errorId = `${paramKey}-error`;

  return (
    <div className={styles.paramField}>
      <label><ParamLabel paramKey={paramKey}>{label}</ParamLabel></label>
      <input
        type="number"
        min={min}
        max={max}
        step={step}
        placeholder={placeholder}
        value={value}
        onChange={event => onChange(event.target.value)}
        className={error ? styles.errorInput : undefined}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? errorId : undefined}
      />
      {error && <span className={styles.inputErrorMsg} id={errorId}>{error}</span>}
    </div>
  );
}

const Configs: React.FC = () => {
  usePageTitle('Configuration');
  const { showSuccess } = useToast();
  const [configs, setConfigs] = useState<Config[]>([]);
  const [credentials, setCredentials] = useState<LLMCredentials[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [modalError, setModalError] = useState<string | null>(null);

  const { validCredentialsMap, validateCredential } = useValidCredentials();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingConfig, setEditingConfig] = useState<Config | null>(null);
  const [viewingConfig, setViewingConfig] = useState<Config | null>(null);
  const [saving, setSaving] = useState(false);
  const [configToDelete, setConfigToDelete] = useState<Config | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  // Form State
  const [projectId, setProjectId] = useState<string>('');
  const [llmCredentialsId, setLlmCredentialsId] = useState<string>('');
  const [modelName, setModelName] = useState<string>('');
  const [maxIter, setMaxIter] = useState<number>(10);

  const selectedCredential = credentials.find(c => c.id.toString() === llmCredentialsId);

  const [isCustomModel, setIsCustomModel] = useState(false);
  const [modelInputHint, setModelInputHint] = useState<string | null>(null);
  const [modelInputError, setModelInputError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    if (!editingConfig && selectedCredential) {
      if (selectedCredential.llm === 'OPENAI' || selectedCredential.llm === 'GEMINI' || selectedCredential.llm === 'ANTHROPIC') {
        const list =
          selectedCredential.llm === 'OPENAI'
            ? OPENAI_MODELS
            : selectedCredential.llm === 'GEMINI'
              ? GEMINI_MODELS
              : ANTHROPIC_MODELS;
        if (!modelName) {
          setIsCustomModel(false);
          setModelInputHint(null);
          setModelInputError(null);
        } else if (list.includes(modelName)) {
          setIsCustomModel(false);
          setModelInputHint(null);
          setModelInputError(null);
        } else {
          setIsCustomModel(true);
        }
      } else if (selectedCredential.llm === 'LOCAL_MODEL') {
        if (selectedCredential.modelName) {
           setModelName(selectedCredential.modelName);
        } else {
           setModelName('Loading...');
           apiCredentials.getLocalModel(selectedCredential.id).then(res => {
             if (!active) return;
             if (res.type === 'Success' && res.data) {
               // save the modelName on the object
               const model = res.data.modelName;
               setCredentials(prev => prev.map(c => c.id === selectedCredential.id ? { ...c, modelName: model } : c));
               setModelName(model);
             } else {
               setModelName('local-model-error');
             }
           });
        }
      }
    }

    return () => { active = false; };
  }, [llmCredentialsId, editingConfig, selectedCredential]);

  const [checkPointInterval, setCheckPointInterval] = useState<number>(5);
  const [showAdvancedParams, setShowAdvancedParams] = useState<boolean>(false);
  const [advancedParams, setAdvancedParams] = useState<Record<string, string>>(defaultAdvancedParams());
  const configValidationErrors = validateConfigValues(maxIter, checkPointInterval, advancedParams);
  const hasConfigValidationErrors = Object.keys(configValidationErrors).length > 0;
  const updateAdvancedParam = (key: string, value: string) => {
    setAdvancedParams(previous => ({ ...previous, [key]: value }));
  };

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const [configsRes, credsRes, projectsRes] = await Promise.all([
        apiConfigs.getAllMyConfigs(),
        apiCredentials.getAll(),
        apiProjects.getAll(),
      ]);

      if (configsRes.type === "Success" && configsRes.data) {
        setConfigs(configsRes.data);
      } else if (configsRes.type === "Failure") {
        setErrorMessage(getErrorMessage(configsRes.error));
      }

      if (credsRes.type === "Success" && credsRes.data) {
        setCredentials(credsRes.data);
        // validate all fetched credentials just in case
        for (const cred of credsRes.data) {
          if (validCredentialsMap[cred.id] === undefined) {
             validateCredential(cred.id).catch(e => console.error('[Configs] validateCredential failed for id', cred.id, e));
          }
        }
      }

      if (projectsRes.type === "Success" && projectsRes.data) {
        setProjects(projectsRes.data);
      } else if (projectsRes.type === "Failure") {
        setErrorMessage(getErrorMessage(projectsRes.error));
      }
    } catch (error) {
      console.error(error);
      setErrorMessage('A network error occurred while fetching data.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleOpenModal = (config?: Config) => {
    setErrorMessage(null);
    setModalError(null);
    setShowAdvancedParams(false);

    if (config) {
      setEditingConfig(config);
      setProjectId(config.projectId?.toString() ?? '');
      setLlmCredentialsId(config.llmCredentialsId.toString());
      setModelName(config.modelName);
      setMaxIter(config.maxIter);
      setCheckPointInterval(config.checkPointInterval);
      setAdvancedParams(loadAdvancedParams(config.additionalParams));
      const credForConfig = credentials.find(c => c.id === config.llmCredentialsId);
      setIsCustomModel(
        credForConfig?.llm !== 'LOCAL_MODEL' &&
        !isPredefinedModel(credForConfig?.llm, config.modelName)
      );
    } else {
      setEditingConfig(null);
      setProjectId('');
      setLlmCredentialsId('');
      setModelName('');
      setMaxIter(10);
      setCheckPointInterval(5);
      setAdvancedParams(defaultAdvancedParams());
      setIsCustomModel(false);
    }
    setModelInputHint(null);
    setModelInputError(null);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingConfig(null);
    setModalError(null);
    setShowAdvancedParams(false);
    setAdvancedParams(defaultAdvancedParams());
    setIsCustomModel(false);
    setModelInputHint(null);
    setModelInputError(null);
  };

  const handleModelSelectChange = (value: string) => {
    if (value === CUSTOM_MODEL_SENTINEL) {
      setIsCustomModel(true);
      setModelName('');
      setModelInputHint(null);
      setModelInputError('Model name is required.');
    } else {
      setIsCustomModel(false);
      setModelName(value);
      setModelInputHint(null);
      setModelInputError(null);
    }
  };

  const handleCredentialChange = (value: string) => {
    setLlmCredentialsId(value);
    setModelName('');
    setIsCustomModel(false);
    setModelInputHint(null);
    setModelInputError(null);
  };

  const handleCustomModelChange = (raw: string) => {
    let v = raw.trim();
    let hint: string | null = null;

    if (v.startsWith('models/')) {
      v = v.slice('models/'.length);
      hint = "Removed 'models/' prefix (the API expects just the id).";
    }

    setModelName(v);
    setModelInputHint(hint);

    if (!v) {
      setModelInputError('Model name is required.');
    } else if (!MODEL_NAME_REGEX.test(v)) {
      setModelInputError('Only letters, digits, dot, underscore, dash and slash are allowed.');
    } else {
      setModelInputError(null);
    }
  };

  const handleSave = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const params = buildAdditionalParams(advancedParams);

    if (hasConfigValidationErrors) {
      setModalError('Fix the highlighted configuration values before saving.');
      if (Object.keys(configValidationErrors).some(key => key.includes('.'))) {
        setShowAdvancedParams(true);
      }
      return;
    }

    if (!llmCredentialsId && !editingConfig) {
      setModalError('You must select valid LLM Credentials to create a configuration.');
      return;
    }

    const finalModelName = isCustomModel ? modelName.trim() : modelName;

    if (isCustomModel) {
      if (!finalModelName) {
        setModelInputError('Model name is required.');
        setModalError('Custom model name is required.');
        return;
      }
      if (!MODEL_NAME_REGEX.test(finalModelName)) {
        setModelInputError('Only letters, digits, dot, underscore, dash and slash are allowed.');
        setModalError('Custom model name has an invalid format.');
        return;
      }
      if (finalModelName !== modelName) setModelName(finalModelName);
    }

    setSaving(true);
    setModalError(null);

    try {
      if (editingConfig) {
        const payload: UpdateConfigInput = {
          modelName: finalModelName,
          maxIter,
          checkPointInterval,
          additionalParams: params
        };

        const unchanged =
          finalModelName === editingConfig.modelName &&
          maxIter === editingConfig.maxIter &&
          checkPointInterval === editingConfig.checkPointInterval &&
          sortedStringify(params) === sortedStringify(visibleAdditionalParams(editingConfig.additionalParams));

        if (unchanged) {
          handleCloseModal();
          return;
        }

        const res = await apiConfigs.update(editingConfig.configId, payload);

        if (res.type === "Success" && res.data) {
          setConfigs(prev => prev.map(c => c.configId === editingConfig.configId ? res.data! : c));
          handleCloseModal();
          showSuccess('Configuration updated successfully.');
        } else if (res.type === "Failure") {
          setModalError(getErrorMessage(res.error));
        }
      } else {
        const payload: CreateConfigInput = {
          projectId: projectId ? parseInt(projectId, 10) : null,
          llmCredentialsId: parseInt(llmCredentialsId, 10),
          modelName: finalModelName,
          maxIter,
          checkPointInterval,
          additionalParams: params
        };

        const res = await apiConfigs.create(payload);
        if (res.type === "Success" && res.data) {
          setConfigs(prev => [...prev, res.data!]);
          handleCloseModal();
          showSuccess('Configuration created successfully.');
        } else if (res.type === "Failure") {
          setModalError(getErrorMessage(res.error));
        }
      }
    } catch (error) {
      console.error(error);
      setModalError(getErrorMessage());
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    setDeletingId(id);
    setErrorMessage(null);
    try {
      const res = await apiConfigs.delete(id);
      if (res.type === 'Success') {
        setConfigs(prev => prev.filter(c => c.configId !== id));
        setConfigToDelete(null);
        showSuccess('Configuration deleted successfully.');
      } else {
        setConfigToDelete(null);
        setErrorMessage(getErrorMessage(res.error));
      }
    } catch (error) {
      setConfigToDelete(null);
      console.error(error);
      setErrorMessage('Failed to delete the configuration.');
    } finally {
      setDeletingId(null);
    }
  };

  if (isLoading) {
    return <LoadingSpinner label="Loading configurations" />;
  }

  const validCredentialsCount = credentials.filter(c => validCredentialsMap[c.id]).length;

  return (
    <motion.div 
      className={styles.container}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>Configuration Files</h1>
          <p className={styles.subtitle}>Define models and evolutionary parameters for your projects.</p>
          <p className={styles.docLink}>
            <a href="https://github.com/algorithmicsuperintelligence/openevolve/blob/main/configs/default_config.yaml" target="_blank" rel="noopener noreferrer">
               View OpenEvolve Default Config Documentation
            </a>
          </p>
        </div>
        <button className={styles.createBtn} onClick={() => handleOpenModal()}>
          <span>+</span> New Config
        </button>
      </div>

      {errorMessage && (
        <Alert variant="error">{errorMessage}</Alert>
      )}

      {credentials.length > 0 && validCredentialsCount === 0 && (
        <Alert variant="warning" title="Notice">
          You don't have any validated LLM Credentials. You won't be able to run projects successfully.
        </Alert>
      )}

      <div className={styles.grid}>
        <AnimatePresence>
          {configs.map((config) => (
            <motion.div
              key={config.configId}
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className={`${styles.card} ${styles.cardClickable}`}
              onClick={() => setViewingConfig(config)}
            >
              <div className={styles.cardHeader}>
                <h3 className={styles.configIdText}>{config.modelName}</h3>
                <span className={styles.modelBadge}>Config #{config.configId}</span>
              </div>

              <div className={styles.cardDetails}>
                <div className={styles.detailRow}>
                  <span>Linked Project:</span>
                  <strong>
                    {config.projectId
                      ? projects.find(project => project.id === config.projectId)?.name ?? `Project #${config.projectId}`
                      : 'Unassigned'}
                  </strong>
                </div>
                <div className={styles.detailRow}>
                  <span>Credential:</span>
                  <strong>
                    {credentials.find(credential => credential.id === config.llmCredentialsId)
                      ? getCredentialLabel(credentials.find(credential => credential.id === config.llmCredentialsId)!)
                      : `Credential #${config.llmCredentialsId}`}
                  </strong>
                </div>
                <div className={styles.detailRow}>
                  <span>Max Iterations:</span>
                  <strong>{config.maxIter}</strong>
                </div>
                <div className={styles.detailRow}>
                  <span>Checkpoint Interval:</span>
                  <strong>{config.checkPointInterval}</strong>
                </div>
                
                {Object.entries(visibleAdditionalParams(config.additionalParams)).length > 0 && (
                  <div style={{ marginTop: '0.5rem' }}>
                    <span style={{ fontSize: '0.75rem', fontWeight: 600 }}>Additional Params:</span>
                    {Object.entries(visibleAdditionalParams(config.additionalParams)).map(([key, value]) => (
                      <div key={key} className={styles.detailRow} style={{ marginTop: '0.25rem' }}>
                        <span style={{ fontSize: '0.75rem', color: '#64748b' }}>{key}</span>
                        <span style={{ fontSize: '0.75rem', fontWeight: 500 }}>{value}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className={styles.cardActions}>
                <button 
                  className={`${styles.actionBtn} ${styles.editBtn}`} 
                  onClick={(e) => {
                    e.stopPropagation();
                    handleOpenModal(config);
                  }}
                >
                  <span>Update</span>
                </button>
                <button 
                  className={`${styles.actionBtn} ${styles.deleteBtn}`} 
                  onClick={(e) => {
                    e.stopPropagation();
                    setConfigToDelete(config);
                  }}
                >
                  <span>Delete</span>
                </button>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>

        {configs.length === 0 && (
          <div className={styles.emptyState}>
            {credentials.length === 0 ? (
              <>
                <strong>Add an LLM credential before creating a configuration.</strong>
                <Link to="/credentials">Go to Credentials</Link>
              </>
            ) : (
              <span>No configurations found. Create one to get started.</span>
            )}
          </div>
        )}
      </div>

      {viewingConfig && (
        <Modal onClose={() => setViewingConfig(null)} ariaLabelledBy="config-details-title" className={styles.modal}>
              <div className={styles.modalHeader}>
                <h2 className={styles.modalTitle} id="config-details-title">Configuration Details</h2>
                <button type="button" className={styles.closeBtn} onClick={() => setViewingConfig(null)} aria-label="Close modal">×</button>
              </div>

              <div className={styles.detailGrid}>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>Config ID</span>
                  <span className={styles.detailValue}>{viewingConfig.configId}</span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>Model Name</span>
                  <span className={styles.detailValue}>{viewingConfig.modelName}</span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>LLM Credential</span>
                  <span className={styles.detailValue}>
                    {credentials.find(credential => credential.id === viewingConfig.llmCredentialsId)
                      ? getCredentialLabel(credentials.find(credential => credential.id === viewingConfig.llmCredentialsId)!)
                      : `Credential #${viewingConfig.llmCredentialsId}`}
                  </span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>Linked Project</span>
                  <span className={styles.detailValue}>
                    {viewingConfig.projectId
                      ? projects.find(project => project.id === viewingConfig.projectId)?.name ?? `Project #${viewingConfig.projectId}`
                      : 'Unassigned'}
                  </span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>Max Iterations</span>
                  <span className={styles.detailValue}>{viewingConfig.maxIter}</span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>Checkpoint Interval</span>
                  <span className={styles.detailValue}>{viewingConfig.checkPointInterval}</span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>Created At</span>
                  <span className={styles.detailValue}>{new Date(viewingConfig.createdAt).toLocaleString()}</span>
                </div>
                {Object.entries(visibleAdditionalParams(viewingConfig.additionalParams)).length > 0 && (
                  <>
                    <div className={styles.detailRow}>
                      <span className={styles.detailLabel} style={{ fontWeight: 700 }}>Additional Parameters</span>
                    </div>
                    {Object.entries(visibleAdditionalParams(viewingConfig.additionalParams)).map(([key, value]) => (
                      <div key={key} className={styles.detailRow}>
                        <span className={styles.detailLabel}>{key}</span>
                        <span className={styles.detailValue}>{value}</span>
                      </div>
                    ))}
                  </>
                )}
              </div>
        </Modal>
      )}

      {/* Modal */}
      {isModalOpen && (
        <Modal onClose={handleCloseModal} ariaLabelledBy="config-modal-title" className={styles.modal}>
          <form onSubmit={handleSave}>
              <div className={styles.modalHeader}>
                <h2 className={styles.modalTitle} id="config-modal-title">
                  {editingConfig ? 'Edit Configuration' : 'Create Configuration'}
                </h2>
                <button type="button" className={styles.closeBtn} onClick={handleCloseModal} aria-label="Close modal">×</button>
              </div>

              {!editingConfig && (
                <>
                  <div className={styles.formGroup}>
                    <label>
                      LLM Credential
                      <span className={styles.labelHint}>* Required</span>
                    </label>
                    <select 
                      value={llmCredentialsId}
                      onChange={e => handleCredentialChange(e.target.value)}
                    >
                      <option value="">Select a credential</option>
                      {credentials.map(c => (
                        <option key={c.id} value={c.id} disabled={!validCredentialsMap[c.id]}>
                          {getCredentialLabel(c)} {validCredentialsMap[c.id] ? '(Valid)' : '(Invalid)'}
                        </option>
                      ))}
                    </select>
                  </div>
                </>
              )}

              <div className={styles.formGroup}>
                <label>
                  Model Name
                  <span className={styles.labelHint}>* Required</span>
                </label>
                {(() => {
                  const provider: LLM | undefined =
                    selectedCredential?.llm ??
                    (editingConfig && OPENAI_MODELS.includes(editingConfig.modelName)
                      ? 'OPENAI'
                      : editingConfig && GEMINI_MODELS.includes(editingConfig.modelName)
                      ? 'GEMINI'
                      : editingConfig && ANTHROPIC_MODELS.includes(editingConfig.modelName)
                      ? 'ANTHROPIC'
                      : undefined);

                  if (provider === 'LOCAL_MODEL') {
                    return (
                      <input
                        type="text"
                        value={modelName || 'Unknown Local Model'}
                        disabled
                        title="Local models are directly linked to the credential."
                      />
                    );
                  }

                  if (provider === 'OPENAI' || provider === 'GEMINI' || provider === 'ANTHROPIC') {
                    const list =
                      provider === 'OPENAI'
                        ? OPENAI_MODELS
                        : provider === 'GEMINI'
                          ? GEMINI_MODELS
                          : ANTHROPIC_MODELS;
                    const selectValue = isCustomModel ? CUSTOM_MODEL_SENTINEL : modelName;
                    const providerLabel = provider === 'OPENAI' ? 'OpenAI' : provider === 'GEMINI' ? 'Gemini' : 'Anthropic';
                    return (
                      <>
                        <select
                          value={selectValue}
                          onChange={e => handleModelSelectChange(e.target.value)}
                        >
                          <option value="" disabled>Select a {providerLabel} model</option>
                          {list.map(m => <option key={m} value={m}>{m}</option>)}
                          <option value={CUSTOM_MODEL_SENTINEL}>Custom model...</option>
                        </select>
                        {isCustomModel && (
                          <>
                            <input
                              type="text"
                              value={modelName}
                              onChange={e => handleCustomModelChange(e.target.value)}
                              placeholder={placeholderForProvider(provider)}
                              className={modelInputError ? styles.errorInput : undefined}
                              style={{ marginTop: '0.5rem' }}
                              autoFocus
                            />
                            <span className={styles.helperText}>
                              Must match the exact model id used by the {providerLabel} API.
                            </span>
                            {modelInputHint && (
                              <span className={styles.helperTextSuccess}>{modelInputHint}</span>
                            )}
                            {modelInputError && (
                              <span className={styles.inputErrorMsg}>{modelInputError}</span>
                            )}
                          </>
                        )}
                      </>
                    );
                  }

                  return (
                    <input
                      type="text"
                      value={modelName}
                      onChange={e => setModelName(e.target.value)}
                      placeholder="e.g., gpt-4.1-mini, gemini-2.5-pro, claude-3-5-sonnet-20241022"
                    />
                  );
                })()}
              </div>

              <div className={styles.formGroup}>
                <label>Max Iterations</label>
                <input 
                  type="number" 
                  min={1}
                  value={maxIter}
                  onChange={e => setMaxIter(Number(e.target.value))}
                  className={configValidationErrors.maxIter ? styles.errorInput : undefined}
                  aria-invalid={Boolean(configValidationErrors.maxIter)}
                />
                {configValidationErrors.maxIter && (
                  <span className={styles.inputErrorMsg}>{configValidationErrors.maxIter}</span>
                )}
                <span className={styles.helperText}>Maximum evolutionary generations before stalling.</span>
              </div>

              <div className={styles.formGroup}>
                <label>Checkpoint Interval</label>
                <input 
                  type="number" 
                  min={1}
                  value={checkPointInterval}
                  onChange={e => setCheckPointInterval(Number(e.target.value))}
                  className={configValidationErrors.checkPointInterval ? styles.errorInput : undefined}
                  aria-invalid={Boolean(configValidationErrors.checkPointInterval)}
                />
                {configValidationErrors.checkPointInterval && (
                  <span className={styles.inputErrorMsg}>{configValidationErrors.checkPointInterval}</span>
                )}
                <span className={styles.helperText}>Save progress every X iterations.</span>
              </div>

              <div className={styles.formGroup}>
                <button
                  type="button"
                  className={styles.toggleAdvancedBtn}
                  onClick={() => setShowAdvancedParams(prev => !prev)}
                >
                  {showAdvancedParams ? 'Hide Advanced Parameters' : 'Advanced Parameters'}
                </button>

                {showAdvancedParams && (
                  <div className={styles.advancedParamsPanel}>
                    <div className={styles.paramSection}>
                      <h4 className={styles.paramSectionTitle}>LLM</h4>
                      <div className={styles.paramGrid}>
                        <NumericParamField
                          paramKey="llm.temperature"
                          label="Temperature"
                          placeholder="0.7"
                          value={advancedParams['llm.temperature']}
                          error={configValidationErrors['llm.temperature']}
                          min={0}
                          step="0.01"
                          onChange={value => updateAdvancedParam('llm.temperature', value)}
                        />
                        <NumericParamField
                          paramKey="llm.top_p"
                          label="Top P"
                          placeholder="0.95"
                          value={advancedParams['llm.top_p']}
                          error={configValidationErrors['llm.top_p']}
                          min={0}
                          max={1}
                          step="0.01"
                          onChange={value => updateAdvancedParam('llm.top_p', value)}
                        />
                        <NumericParamField
                          paramKey="llm.max_tokens"
                          label="Max Tokens"
                          placeholder="4096"
                          value={advancedParams['llm.max_tokens']}
                          error={configValidationErrors['llm.max_tokens']}
                          min={1}
                          onChange={value => updateAdvancedParam('llm.max_tokens', value)}
                        />
                        <NumericParamField
                          paramKey="llm.timeout"
                          label="Timeout (s)"
                          placeholder="60"
                          value={advancedParams['llm.timeout']}
                          error={configValidationErrors['llm.timeout']}
                          min={0}
                          onChange={value => updateAdvancedParam('llm.timeout', value)}
                        />
                        <NumericParamField
                          paramKey="llm.retries"
                          label="Retries"
                          placeholder="3"
                          value={advancedParams['llm.retries']}
                          error={configValidationErrors['llm.retries']}
                          min={0}
                          onChange={value => updateAdvancedParam('llm.retries', value)}
                        />
                      </div>
                    </div>

                    <div className={styles.paramSection}>
                      <h4 className={styles.paramSectionTitle}>Prompt</h4>
                      <div className={styles.paramGrid}>
                        <NumericParamField
                          paramKey="prompt.num_top_programs"
                          label="Num Top Programs"
                          placeholder="3"
                          value={advancedParams['prompt.num_top_programs']}
                          error={configValidationErrors['prompt.num_top_programs']}
                          min={0}
                          onChange={value => updateAdvancedParam('prompt.num_top_programs', value)}
                        />
                        <NumericParamField
                          paramKey="prompt.num_diverse_programs"
                          label="Num Diverse Programs"
                          placeholder="2"
                          value={advancedParams['prompt.num_diverse_programs']}
                          error={configValidationErrors['prompt.num_diverse_programs']}
                          min={0}
                          onChange={value => updateAdvancedParam('prompt.num_diverse_programs', value)}
                        />
                        <div className={`${styles.paramField} ${styles.paramFieldCheckbox}`}>
                          <label><ParamLabel paramKey="prompt.include_artifacts">Include Artifacts</ParamLabel></label>
                          <input type="checkbox" checked={advancedParams['prompt.include_artifacts'] === 'true'} onChange={e => setAdvancedParams(p => ({...p, 'prompt.include_artifacts': e.target.checked ? 'true' : 'false'}))} />
                        </div>
                      </div>
                      <div className={styles.paramField} style={{ marginTop: '0.5rem' }}>
                        <label><ParamLabel paramKey="prompt.system_message">System Message</ParamLabel></label>
                        <textarea placeholder="You are an OpenEvolve assistant..." value={advancedParams['prompt.system_message']} onChange={e => setAdvancedParams(p => ({...p, 'prompt.system_message': e.target.value}))} style={{ minHeight: '60px' }} />
                      </div>
                    </div>

                    <div className={styles.paramSection}>
                      <h4 className={styles.paramSectionTitle}>Database</h4>
                      <div className={styles.paramGrid}>
                        <NumericParamField
                          paramKey="database.population_size"
                          label="Population Size"
                          placeholder="100"
                          value={advancedParams['database.population_size']}
                          error={configValidationErrors['database.population_size']}
                          min={1}
                          onChange={value => updateAdvancedParam('database.population_size', value)}
                        />
                        <NumericParamField
                          paramKey="database.archive_size"
                          label="Archive Size"
                          placeholder="50"
                          value={advancedParams['database.archive_size']}
                          error={configValidationErrors['database.archive_size']}
                          min={1}
                          onChange={value => updateAdvancedParam('database.archive_size', value)}
                        />
                        <NumericParamField
                          paramKey="database.num_islands"
                          label="Num Islands"
                          placeholder="4"
                          value={advancedParams['database.num_islands']}
                          error={configValidationErrors['database.num_islands']}
                          min={1}
                          onChange={value => updateAdvancedParam('database.num_islands', value)}
                        />
                        <NumericParamField
                          paramKey="database.migration_interval"
                          label="Migration Interval"
                          placeholder="10"
                          value={advancedParams['database.migration_interval']}
                          error={configValidationErrors['database.migration_interval']}
                          min={1}
                          onChange={value => updateAdvancedParam('database.migration_interval', value)}
                        />
                        <NumericParamField
                          paramKey="database.migration_rate"
                          label="Migration Rate"
                          placeholder="0.1"
                          value={advancedParams['database.migration_rate']}
                          error={configValidationErrors['database.migration_rate']}
                          min={0}
                          max={1}
                          step="0.01"
                          onChange={value => updateAdvancedParam('database.migration_rate', value)}
                        />
                        <NumericParamField
                          paramKey="database.elite_selection_ratio"
                          label="Elite Selection Ratio"
                          placeholder="0.1"
                          value={advancedParams['database.elite_selection_ratio']}
                          error={configValidationErrors['database.elite_selection_ratio']}
                          min={0}
                          max={1}
                          step="0.01"
                          onChange={value => updateAdvancedParam('database.elite_selection_ratio', value)}
                        />
                        <NumericParamField
                          paramKey="database.exploration_ratio"
                          label="Exploration Ratio"
                          placeholder="0.2"
                          value={advancedParams['database.exploration_ratio']}
                          error={configValidationErrors['database.exploration_ratio']}
                          min={0}
                          max={1}
                          step="0.01"
                          onChange={value => updateAdvancedParam('database.exploration_ratio', value)}
                        />
                        <NumericParamField
                          paramKey="database.exploitation_ratio"
                          label="Exploitation Ratio"
                          placeholder="0.7"
                          value={advancedParams['database.exploitation_ratio']}
                          error={configValidationErrors['database.exploitation_ratio']}
                          min={0}
                          max={1}
                          step="0.01"
                          onChange={value => updateAdvancedParam('database.exploitation_ratio', value)}
                        />
                        <NumericParamField
                          paramKey="database.feature_bins"
                          label="Feature Bins"
                          placeholder="10"
                          value={advancedParams['database.feature_bins']}
                          error={configValidationErrors['database.feature_bins']}
                          min={1}
                          onChange={value => updateAdvancedParam('database.feature_bins', value)}
                        />
                      </div>
                      <div className={styles.paramField} style={{ marginTop: '0.5rem' }}>
                        <label><ParamLabel paramKey="database.feature_dimensions">Feature Dimensions (comma-separated)</ParamLabel></label>
                        <input type="text" placeholder="complexity,diversity" value={advancedParams['database.feature_dimensions']} onChange={e => setAdvancedParams(p => ({...p, 'database.feature_dimensions': e.target.value}))} />
                      </div>
                    </div>

                    <div className={styles.paramSection}>
                      <h4 className={styles.paramSectionTitle}>Evaluator</h4>
                      <div className={styles.paramGrid}>
                        <NumericParamField
                          paramKey="evaluator.timeout"
                          label="Timeout (s)"
                          placeholder="300"
                          value={advancedParams['evaluator.timeout']}
                          error={configValidationErrors['evaluator.timeout']}
                          min={1}
                          onChange={value => updateAdvancedParam('evaluator.timeout', value)}
                        />
                        <NumericParamField
                          paramKey="evaluator.max_retries"
                          label="Max Retries"
                          placeholder="3"
                          value={advancedParams['evaluator.max_retries']}
                          error={configValidationErrors['evaluator.max_retries']}
                          min={0}
                          onChange={value => updateAdvancedParam('evaluator.max_retries', value)}
                        />
                        <NumericParamField
                          paramKey="evaluator.parallel_evaluations"
                          label="Parallel Evaluations"
                          placeholder="4"
                          value={advancedParams['evaluator.parallel_evaluations']}
                          error={configValidationErrors['evaluator.parallel_evaluations']}
                          min={0}
                          onChange={value => updateAdvancedParam('evaluator.parallel_evaluations', value)}
                        />
                        <div className={`${styles.paramField} ${styles.paramFieldCheckbox}`}>
                          <label><ParamLabel paramKey="evaluator.cascade_evaluation">Cascade Evaluation</ParamLabel></label>
                          <input type="checkbox" checked={advancedParams['evaluator.cascade_evaluation'] === 'true'} onChange={e => setAdvancedParams(p => ({...p, 'evaluator.cascade_evaluation': e.target.checked ? 'true' : 'false'}))} />
                        </div>
                        <NumericParamField
                          paramKey="evaluator.cascade_threshold_1"
                          label="Cascade Threshold 1"
                          placeholder="0.5"
                          value={advancedParams['evaluator.cascade_threshold_1']}
                          error={configValidationErrors['evaluator.cascade_threshold_1']}
                          min={0}
                          max={1}
                          step="0.01"
                          onChange={value => updateAdvancedParam('evaluator.cascade_threshold_1', value)}
                        />
                        <NumericParamField
                          paramKey="evaluator.cascade_threshold_2"
                          label="Cascade Threshold 2"
                          placeholder="0.75"
                          value={advancedParams['evaluator.cascade_threshold_2']}
                          error={configValidationErrors['evaluator.cascade_threshold_2']}
                          min={0}
                          max={1}
                          step="0.01"
                          onChange={value => updateAdvancedParam('evaluator.cascade_threshold_2', value)}
                        />
                        <NumericParamField
                          paramKey="evaluator.cascade_threshold_3"
                          label="Cascade Threshold 3"
                          placeholder="0.9"
                          value={advancedParams['evaluator.cascade_threshold_3']}
                          error={configValidationErrors['evaluator.cascade_threshold_3']}
                          min={0}
                          max={1}
                          step="0.01"
                          onChange={value => updateAdvancedParam('evaluator.cascade_threshold_3', value)}
                        />
                      </div>
                    </div>

                    <div className={styles.paramSection}>
                      <h4 className={styles.paramSectionTitle}>General</h4>
                      <div className={styles.paramGrid}>
                        <div className={`${styles.paramField} ${styles.paramFieldCheckbox}`}>
                          <label><ParamLabel paramKey="diff_based_evolution">Diff-Based Evolution</ParamLabel></label>
                          <input type="checkbox" checked={advancedParams['diff_based_evolution'] === 'true'} onChange={e => setAdvancedParams(p => ({...p, 'diff_based_evolution': e.target.checked ? 'true' : 'false'}))} />
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {modalError && (
                <Alert variant="error">{modalError}</Alert>
              )}

              <div className={styles.modalActions}>
                <button type="button" className={styles.cancelBtn} onClick={handleCloseModal} disabled={saving}>
                  Cancel
                </button>
                <button 
                  type="submit"
                  className={styles.saveBtn} 
                  disabled={saving || (!editingConfig && !llmCredentialsId) || !modelName || hasConfigValidationErrors}
                >
                  {saving ? <span> Saving...</span> : 'Save Configuration'}
                </button>
              </div>

          </form>
        </Modal>
      )}

      <ConfirmDialog
        isOpen={configToDelete !== null}
        title="Delete configuration?"
        message={`Delete "${configToDelete?.modelName ?? 'this configuration'}" permanently? Projects using it may need another configuration.`}
        isConfirming={deletingId !== null}
        onCancel={() => {
          if (deletingId === null) setConfigToDelete(null);
        }}
        onConfirm={() => {
          if (configToDelete) handleDelete(configToDelete.configId);
        }}
      />
    </motion.div>
  );
};

export default Configs;
