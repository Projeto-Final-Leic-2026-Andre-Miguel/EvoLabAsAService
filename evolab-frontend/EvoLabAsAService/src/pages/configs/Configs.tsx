import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import styles from './Configs.module.css';
import { apiConfigs, type Config, type CreateConfigInput, type UpdateConfigInput } from './apiConfigs';
import { apiCredentials } from '../credentials/apiCredentials';
import type { LLMCredentials } from '../../types/credentials';
import { useValidCredentials } from '../../contexts/ValidCredentialsContext';

const Configs: React.FC = () => {
  const [configs, setConfigs] = useState<Config[]>([]);
  const [credentials, setCredentials] = useState<LLMCredentials[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { validCredentialsMap, validateCredential } = useValidCredentials();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingConfig, setEditingConfig] = useState<Config | null>(null);
  const [saving, setSaving] = useState(false);

  // Form State
  const [projectId, setProjectId] = useState<string>('');
  const [llmCredentialsId, setLlmCredentialsId] = useState<string>('');
  const [modelName, setModelName] = useState<string>('gpt-4');
  const [maxIter, setMaxIter] = useState<number>(10);
  const [checkPointInterval, setCheckPointInterval] = useState<number>(5);
  const [additionalParamsStr, setAdditionalParamsStr] = useState<string>('{\n  "temperature": "0.7"\n}');
  const [jsonError, setJsonError] = useState<string | null>(null);

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
             validateCredential(cred.id).catch(() => {});
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
    setJsonError(null);

    if (config) {
      setEditingConfig(config);
      setProjectId(config.projectId?.toString() ?? '');
      setLlmCredentialsId(config.llmCredentialsId.toString());
      setModelName(config.modelName);
      setMaxIter(config.maxIter);
      setCheckPointInterval(config.checkPointInterval);
      setAdditionalParamsStr(JSON.stringify(config.additionalParams, null, 2));
    } else {
      setEditingConfig(null);
      setProjectId('');
      setLlmCredentialsId('');
      setModelName('gpt-4');
      setMaxIter(10);
      setCheckPointInterval(5);
      setAdditionalParamsStr('{\n  "temperature": "0.7"\n}');
    }
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingConfig(null);
  };

  const parseAdditionalParams = (): Record<string, string> | null => {
    try {
      const parsed = JSON.parse(additionalParamsStr);
      // ensure it's an object of strings
      const result: Record<string, string> = {};
      for (const [k, v] of Object.entries(parsed)) {
        result[k] = String(v);
      }
      setJsonError(null);
      return result;
    } catch {
      setJsonError('Invalid JSON format. Please ensure it is a valid JSON object.');
      return null;
    }
  };

  const handleSave = async () => {
    const params = parseAdditionalParams();
    if (!params) return;

    if (!llmCredentialsId && !editingConfig) {
      setJsonError('You must select valid LLM Credentials to create a configuration.');
      return;
    }

    setSaving(true);
    setErrorMessage(null);

    try {
      if (editingConfig) {
        const payload: UpdateConfigInput = {
          modelName,
          maxIter,
          checkPointInterval,
          additionalParams: params
        };
        const res = await apiConfigs.update(editingConfig.configId, payload);
        
        if (res.type === "Success" && res.data) {
          setConfigs(prev => prev.map(c => c.configId === editingConfig.configId ? res.data! : c));
          handleCloseModal();
        } else if (res.type === "Failure") {
          setErrorMessage(`Update failed: ${res.error?.message || 'Unknown Error'}`);
        }
      } else {
        const payload: CreateConfigInput = {
          projectId: projectId ? parseInt(projectId, 10) : null,
          llmCredentialsId: parseInt(llmCredentialsId, 10),
          modelName,
          maxIter,
          checkPointInterval,
          additionalParams: params
        };

        const res = await apiConfigs.create(payload);
        if (res.type === "Success" && res.data) {
          setConfigs(prev => [...prev, res.data!]);
          handleCloseModal();
        } else if (res.type === "Failure") {
          setErrorMessage(`Creation failed: ${res.error?.message || 'Unknown Error'}`);
        }
      }
    } catch (error) {
      console.error(error);
      setErrorMessage('Network error while saving configuration.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    setErrorMessage(null);
    try {
      await apiConfigs.delete(id);
      setConfigs(prev => prev.filter(c => c.configId !== id));
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
          <span>➕</span> New Config
        </button>
      </div>

      {errorMessage && (
        <div className={styles.errorBanner}>
          <strong>Error:</strong> {errorMessage}
        </div>
      )}

      {credentials.length > 0 && validCredentialsCount === 0 && (
        <div className={styles.warningAlert}>
          <span>⚠️</span>
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
              className={styles.card}
            >
              <div className={styles.cardHeader}>
                <h3 className={styles.configIdText}>Config #{config.configId}</h3>
                <span className={styles.modelBadge}>{config.modelName}</span>
              </div>

              <div className={styles.cardDetails}>
                <div className={styles.detailRow}>
                  <span>Linked Project ID:</span>
                  <strong>{config.projectId ? `#${config.projectId}` : 'Unassigned'}</strong>
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
                
                <div style={{ marginTop: '0.5rem' }}>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600 }}>Additional Params:</span>
                  <div className={styles.jsonBox}>
                    {JSON.stringify(config.additionalParams, null, 2)}
                  </div>
                </div>
              </div>

              <div className={styles.cardActions}>
                <button 
                  className={`${styles.actionBtn} ${styles.editBtn}`} 
                  onClick={() => handleOpenModal(config)}
                >
                  <span>Update</span>
                </button>
                <button 
                  className={`${styles.actionBtn} ${styles.deleteBtn}`} 
                  onClick={() => handleDelete(config.configId)}
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
                  <div className={styles.warningAlert}>
                    <span>ℹ️</span>
                    <span>Credentials and Project Links are irreversible once created. To change them, you must create a new Configuration.</span>
                  </div>

                  <div className={styles.formGroup}>
                    <label>
                      LLM Credential
                      <span className={styles.labelHint}>* Required</span>
                    </label>
                    <select 
                      value={llmCredentialsId}
                      onChange={e => setLlmCredentialsId(e.target.value)}
                    >
                      <option value="">-- Select Validated Credential --</option>
                      {credentials.map(c => (
                        <option key={c.id} value={c.id} disabled={!validCredentialsMap[c.id]}>
                          #{c.id} - {c.llm} {validCredentialsMap[c.id] ? '(Valid)' : '(Invalid)'}
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
                <input 
                  type="text" 
                  value={modelName}
                  onChange={e => setModelName(e.target.value)}
                  placeholder="e.g., gpt-4, gemini-pro, llama2"
                />
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
                <label>Additional Parameters (JSON)</label>
                <textarea 
                  className={jsonError ? styles.errorInput : ''}
                  value={additionalParamsStr}
                  onChange={e => {
                    setAdditionalParamsStr(e.target.value);
                    setJsonError(null);
                  }}
                  placeholder='{"temperature": "0.7"}'
                />
                {jsonError && <span className={styles.inputErrorMsg}>{jsonError}</span>}
                <span className={styles.helperText}>Must be a valid JSON object holding string key-value pairs.</span>
              </div>

              <div className={styles.modalActions}>
                <button className={styles.cancelBtn} onClick={handleCloseModal} disabled={saving}>
                  Cancel
                </button>
                <button 
                  className={styles.saveBtn} 
                  onClick={handleSave} 
                  disabled={saving || (!editingConfig && !llmCredentialsId) || !modelName}
                >
                  {saving ? <span>⏳ Saving...</span> : 'Save Configuration'}
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
