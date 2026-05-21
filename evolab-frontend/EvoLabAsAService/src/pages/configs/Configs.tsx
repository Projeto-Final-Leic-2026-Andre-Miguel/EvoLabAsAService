import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import styles from './Configs.module.css';
import { apiConfigs, type Config, type CreateConfigInput, type UpdateConfigInput } from './apiConfigs';
import { apiCredentials } from '../credentials/apiCredentials';
import type { LLM, LLMCredentials } from '../../types/credentials';
import { useValidCredentials } from '../../contexts/ValidCredentialsContext';
import { getErrorMessage } from '../../utils/errorsDescriptions';

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

const Configs: React.FC = () => {
  const [configs, setConfigs] = useState<Config[]>([]);
  const [credentials, setCredentials] = useState<LLMCredentials[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [modalError, setModalError] = useState<string | null>(null);

  const { validCredentialsMap, validateCredential } = useValidCredentials();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingConfig, setEditingConfig] = useState<Config | null>(null);
  const [viewingConfig, setViewingConfig] = useState<Config | null>(null);
  const [saving, setSaving] = useState(false);

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

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const [configsRes, credsRes] = await Promise.all([
        apiConfigs.getAllMyConfigs(),
        apiCredentials.getAll()
      ]);

      if (configsRes.type === "Success" && configsRes.data) {
        setConfigs(configsRes.data);
      } else if (configsRes.type === "Failure") {
        setErrorMessage(configsRes.error?.message || 'Failed to fetch configs.');
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

  const handleSave = async () => {
    const params = buildAdditionalParams(advancedParams);

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
        } else if (res.type === "Failure") {
          setModalError(getErrorMessage(res.error?.message || 'unknown-error'));
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
        } else if (res.type === "Failure") {
          setModalError(getErrorMessage(res.error?.message || 'unknown-error'));
        }
      }
    } catch (error) {
      console.error(error);
      setModalError(getErrorMessage('unknown-error'));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    setErrorMessage(null);
    try {
      const res = await apiConfigs.delete(id);
      if (res.type === 'Success') {
        setConfigs(prev => prev.filter(c => c.configId !== id));
      } else {
        setErrorMessage(getErrorMessage(res.error?.message || 'unknown-error'));
      }
    } catch (error) {
      console.error(error);
      setErrorMessage('Failed to delete the configuration.');
    }
  };

  if (isLoading) {
    return (
      <div className={styles.container} style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <span>Loading Configurations...</span>
      </div>
    );
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
        <div className={styles.errorBanner}>
          <strong>Error:</strong> {errorMessage}
        </div>
      )}

      {credentials.length > 0 && validCredentialsCount === 0 && (
        <div className={styles.warningAlert}>
          <span>!</span>
          <span><strong>Notice:</strong> You don't have any validated LLM Credentials. You won't be able to run projects successfully.</span>
        </div>
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
                <h3 className={styles.configIdText}>Config {config.configId}</h3>
                <span className={styles.modelBadge}>{config.modelName}</span>
              </div>

              <div className={styles.cardDetails}>
                <div className={styles.detailRow}>
                  <span>Linked Project ID:</span>
                  <strong>{config.projectId ?? 'Unassigned'}</strong>
                </div>
                <div className={styles.detailRow}>
                  <span>Credentials ID:</span>
                  <strong>{config.llmCredentialsId}</strong>
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
                    handleDelete(config.configId);
                  }}
                >
                  <span>Delete</span>
                </button>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>

        {configs.length === 0 && (
          <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '4rem', color: '#64748b' }}>
            No configurations found. Create one to get started.
          </div>
        )}
      </div>

      <AnimatePresence>
        {viewingConfig && (
          <motion.div
            className={styles.modalOverlay}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <motion.div
              className={styles.modal}
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 30 }}
            >
              <div className={styles.modalHeader}>
                <h2 className={styles.modalTitle}>Configuration Details</h2>
                <button className={styles.closeBtn} onClick={() => setViewingConfig(null)}>×</button>
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
                  <span className={styles.detailLabel}>LLM Credentials ID</span>
                  <span className={styles.detailValue}>{viewingConfig.llmCredentialsId}</span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>Linked Project ID</span>
                  <span className={styles.detailValue}>{viewingConfig.projectId ?? 'Unassigned'}</span>
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
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Modal */}
      <AnimatePresence>
        {isModalOpen && (
          <motion.div 
            className={styles.modalOverlay}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <motion.div 
              className={styles.modal}
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 30 }}
            >
              <div className={styles.modalHeader}>
                <h2 className={styles.modalTitle}>
                  {editingConfig ? 'Edit Configuration' : 'Create Configuration'}
                </h2>
                <button className={styles.closeBtn} onClick={handleCloseModal}>×</button>
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
                      <option value="">-- Select Validated Credential --</option>
                      {credentials.map(c => (
                        <option key={c.id} value={c.id} disabled={!validCredentialsMap[c.id]}>
                          {c.id} - {c.llm} {validCredentialsMap[c.id] ? '(Valid)' : '(Invalid)'}
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
                          <option value="" disabled>-- Select {providerLabel} Model (e.g. {placeholderForProvider(provider).replace('e.g., ', '')}) --</option>
                          {list.map(m => <option key={m} value={m}>{m}</option>)}
                          <option disabled>----------</option>
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
                />
                <span className={styles.helperText}>Maximum evolutionary generations before stalling.</span>
              </div>

              <div className={styles.formGroup}>
                <label>Checkpoint Interval</label>
                <input 
                  type="number" 
                  min={1}
                  value={checkPointInterval}
                  onChange={e => setCheckPointInterval(Number(e.target.value))}
                />
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
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="llm.temperature">Temperature</ParamLabel></label>
                          <input type="number" step="0.01" placeholder="0.7" value={advancedParams['llm.temperature']} onChange={e => setAdvancedParams(p => ({...p, 'llm.temperature': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="llm.top_p">Top P</ParamLabel></label>
                          <input type="number" step="0.01" placeholder="0.95" value={advancedParams['llm.top_p']} onChange={e => setAdvancedParams(p => ({...p, 'llm.top_p': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="llm.max_tokens">Max Tokens</ParamLabel></label>
                          <input type="number" placeholder="4096" value={advancedParams['llm.max_tokens']} onChange={e => setAdvancedParams(p => ({...p, 'llm.max_tokens': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="llm.timeout">Timeout (s)</ParamLabel></label>
                          <input type="number" placeholder="60" value={advancedParams['llm.timeout']} onChange={e => setAdvancedParams(p => ({...p, 'llm.timeout': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="llm.retries">Retries</ParamLabel></label>
                          <input type="number" placeholder="3" value={advancedParams['llm.retries']} onChange={e => setAdvancedParams(p => ({...p, 'llm.retries': e.target.value}))} />
                        </div>
                      </div>
                    </div>

                    <div className={styles.paramSection}>
                      <h4 className={styles.paramSectionTitle}>Prompt</h4>
                      <div className={styles.paramGrid}>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="prompt.num_top_programs">Num Top Programs</ParamLabel></label>
                          <input type="number" placeholder="3" value={advancedParams['prompt.num_top_programs']} onChange={e => setAdvancedParams(p => ({...p, 'prompt.num_top_programs': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="prompt.num_diverse_programs">Num Diverse Programs</ParamLabel></label>
                          <input type="number" placeholder="2" value={advancedParams['prompt.num_diverse_programs']} onChange={e => setAdvancedParams(p => ({...p, 'prompt.num_diverse_programs': e.target.value}))} />
                        </div>
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
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="database.population_size">Population Size</ParamLabel></label>
                          <input type="number" placeholder="100" value={advancedParams['database.population_size']} onChange={e => setAdvancedParams(p => ({...p, 'database.population_size': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="database.archive_size">Archive Size</ParamLabel></label>
                          <input type="number" placeholder="50" value={advancedParams['database.archive_size']} onChange={e => setAdvancedParams(p => ({...p, 'database.archive_size': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="database.num_islands">Num Islands</ParamLabel></label>
                          <input type="number" placeholder="4" value={advancedParams['database.num_islands']} onChange={e => setAdvancedParams(p => ({...p, 'database.num_islands': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="database.migration_interval">Migration Interval</ParamLabel></label>
                          <input type="number" placeholder="10" value={advancedParams['database.migration_interval']} onChange={e => setAdvancedParams(p => ({...p, 'database.migration_interval': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="database.migration_rate">Migration Rate</ParamLabel></label>
                          <input type="number" step="0.01" placeholder="0.1" value={advancedParams['database.migration_rate']} onChange={e => setAdvancedParams(p => ({...p, 'database.migration_rate': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="database.elite_selection_ratio">Elite Selection Ratio</ParamLabel></label>
                          <input type="number" step="0.01" placeholder="0.1" value={advancedParams['database.elite_selection_ratio']} onChange={e => setAdvancedParams(p => ({...p, 'database.elite_selection_ratio': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="database.exploration_ratio">Exploration Ratio</ParamLabel></label>
                          <input type="number" step="0.01" placeholder="0.2" value={advancedParams['database.exploration_ratio']} onChange={e => setAdvancedParams(p => ({...p, 'database.exploration_ratio': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="database.exploitation_ratio">Exploitation Ratio</ParamLabel></label>
                          <input type="number" step="0.01" placeholder="0.7" value={advancedParams['database.exploitation_ratio']} onChange={e => setAdvancedParams(p => ({...p, 'database.exploitation_ratio': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="database.feature_bins">Feature Bins</ParamLabel></label>
                          <input type="number" placeholder="10" value={advancedParams['database.feature_bins']} onChange={e => setAdvancedParams(p => ({...p, 'database.feature_bins': e.target.value}))} />
                        </div>
                      </div>
                      <div className={styles.paramField} style={{ marginTop: '0.5rem' }}>
                        <label><ParamLabel paramKey="database.feature_dimensions">Feature Dimensions (comma-separated)</ParamLabel></label>
                        <input type="text" placeholder="complexity,diversity" value={advancedParams['database.feature_dimensions']} onChange={e => setAdvancedParams(p => ({...p, 'database.feature_dimensions': e.target.value}))} />
                      </div>
                    </div>

                    <div className={styles.paramSection}>
                      <h4 className={styles.paramSectionTitle}>Evaluator</h4>
                      <div className={styles.paramGrid}>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="evaluator.timeout">Timeout (s)</ParamLabel></label>
                          <input type="number" placeholder="300" value={advancedParams['evaluator.timeout']} onChange={e => setAdvancedParams(p => ({...p, 'evaluator.timeout': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="evaluator.max_retries">Max Retries</ParamLabel></label>
                          <input type="number" placeholder="3" value={advancedParams['evaluator.max_retries']} onChange={e => setAdvancedParams(p => ({...p, 'evaluator.max_retries': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="evaluator.parallel_evaluations">Parallel Evaluations</ParamLabel></label>
                          <input type="number" placeholder="4" value={advancedParams['evaluator.parallel_evaluations']} onChange={e => setAdvancedParams(p => ({...p, 'evaluator.parallel_evaluations': e.target.value}))} />
                        </div>
                        <div className={`${styles.paramField} ${styles.paramFieldCheckbox}`}>
                          <label><ParamLabel paramKey="evaluator.cascade_evaluation">Cascade Evaluation</ParamLabel></label>
                          <input type="checkbox" checked={advancedParams['evaluator.cascade_evaluation'] === 'true'} onChange={e => setAdvancedParams(p => ({...p, 'evaluator.cascade_evaluation': e.target.checked ? 'true' : 'false'}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="evaluator.cascade_threshold_1">Cascade Threshold 1</ParamLabel></label>
                          <input type="number" step="0.01" placeholder="0.5" value={advancedParams['evaluator.cascade_threshold_1']} onChange={e => setAdvancedParams(p => ({...p, 'evaluator.cascade_threshold_1': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="evaluator.cascade_threshold_2">Cascade Threshold 2</ParamLabel></label>
                          <input type="number" step="0.01" placeholder="0.75" value={advancedParams['evaluator.cascade_threshold_2']} onChange={e => setAdvancedParams(p => ({...p, 'evaluator.cascade_threshold_2': e.target.value}))} />
                        </div>
                        <div className={styles.paramField}>
                          <label><ParamLabel paramKey="evaluator.cascade_threshold_3">Cascade Threshold 3</ParamLabel></label>
                          <input type="number" step="0.01" placeholder="0.9" value={advancedParams['evaluator.cascade_threshold_3']} onChange={e => setAdvancedParams(p => ({...p, 'evaluator.cascade_threshold_3': e.target.value}))} />
                        </div>
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
                <div className={styles.errorBanner}>
                  <strong>Error:</strong> {modalError}
                </div>
              )}

              <div className={styles.modalActions}>
                <button className={styles.cancelBtn} onClick={handleCloseModal} disabled={saving}>
                  Cancel
                </button>
                <button 
                  className={styles.saveBtn} 
                  onClick={handleSave} 
                  disabled={saving || (!editingConfig && !llmCredentialsId) || !modelName}
                >
                  {saving ? <span> Saving...</span> : 'Save Configuration'}
                </button>
              </div>

            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};

export default Configs;
