import React, { useCallback, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { apiProjects, type Project, type CreateProjectInput, type UpdateProjectDetailsInput } from './apiProjects';
import { apiConfigs } from '../configs/apiConfigs';
import styles from './Projects.module.css';
import { getErrorMessage } from '../../utils/errorsDescriptions';
import { usePolling } from '../../hooks/usePolling';

const Projects: React.FC = () => {
  const navigate = useNavigate();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);

  // Form states
  const [formData, setFormData] = useState<CreateProjectInput>({
    name: '',
    description: '',
    configId: null,
    initialProgram: '',
    evaluatorCode: ''
  });

  const [saving, setSaving] = useState(false);
  const [startingId, setStartingId] = useState<number | null>(null);
  const [restartingId, setRestartingId] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [configsError, setConfigsError] = useState<string | null>(null);
  const [modalError, setModalError] = useState<string | null>(null);

  const initialProgramFileRef = useRef<HTMLInputElement>(null);
  const evaluatorCodeFileRef = useRef<HTMLInputElement>(null);

  const fetchProjectsAndConfigs = useCallback(async () => {
    const [projectsResult, configsResult] = await Promise.all([
      apiProjects.getAll(),
      apiConfigs.getAllMyConfigs()
    ]);
    if (projectsResult.type === 'Failure') {
      throw new Error(getErrorMessage(projectsResult.error?.message || 'unknown-error'));
    }
    if (configsResult.type === 'Failure') {
      setConfigsError(getErrorMessage(configsResult.error?.message || 'unknown-error'));
    } else {
      setConfigsError(null);
    }
    return {
      projects: projectsResult.data ?? [],
      configs: configsResult.type === 'Success' && configsResult.data ? configsResult.data : [],
    };
  }, []);

  const { data: projectsData, isLoading, error: pollingError, refresh } = usePolling(fetchProjectsAndConfigs, 4000);

  const projects = projectsData?.projects ?? [];
  const configs = projectsData?.configs ?? [];

  const handleFileUpload = (field: 'initialProgram' | 'evaluatorCode', file: File) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const content = e.target?.result as string;
      setFormData(prev => ({ ...prev, [field]: content }));
    };
    reader.readAsText(file);
  };

  const handleOpenModal = (project?: Project) => {
    if (project) {
      setEditingProject(project);
      setFormData({
        name: project.name,
        description: project.description || '',
        configId: project.configId,
        initialProgram: project.initialProgram || '',
        evaluatorCode: project.evaluatorCode || ''
      });
    } else {
      setEditingProject(null);
      setFormData({
        name: '',
        description: '',
        configId: null,
        initialProgram: '',
        evaluatorCode: ''
      });
    }
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingProject(null);
    setModalError(null);
  };

  const handleSave = async () => {
    setSaving(true);
    setModalError(null);
    try {
      if (editingProject) {
        const payload: UpdateProjectDetailsInput = {
          name: formData.name,
          description: formData.description,
          configId: formData.configId,
          initialProgram: formData.initialProgram,
          evaluatorCode: formData.evaluatorCode
        };
        const res = await apiProjects.update(editingProject.id, payload);
        if (res.type === "Success" && res.data) {
          refresh();
          handleCloseModal();
        } else if (res.type === "Failure") {
          setModalError(getErrorMessage(res.error?.message || 'unknown-error'));
        }
      } else {
        const res = await apiProjects.create(formData);
        if (res.type === "Success" && res.data) {
          refresh();
          handleCloseModal();
        } else if (res.type === "Failure") {
          setModalError(getErrorMessage(res.error?.message || 'unknown-error'));
        }
      }
    } catch (error) {
      console.error('Error saving project:', error);
      setModalError(getErrorMessage('unknown-error'));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    setErrorMessage(null);
    try {
      const res = await apiProjects.delete(id);
      if (res.type === "Success") {
        refresh();
      } else {
        setErrorMessage(getErrorMessage(res.error?.message || 'unknown-error'));
      }
    } catch (error) {
      console.error('Failed to delete project:', error);
      setErrorMessage(getErrorMessage('unknown-error'));
    }
  };

  const handleStart = async (id: number) => {
    setStartingId(id);
    setErrorMessage(null);
    try {
      const res = await apiProjects.start(id);
      if (res.type === "Success") {
        refresh();
      } else {
        setErrorMessage(getErrorMessage(res.error?.message || 'unknown-error'));
      }
    } catch (error) {
      console.error('Error starting project:', error);
      setErrorMessage(getErrorMessage('unknown-error'));
    } finally {
      setStartingId(null);
    }
  };

  const handleRestart = async (id: number) => {
    setRestartingId(id);
    setErrorMessage(null);
    try {
      const res = await apiProjects.restart(id);
      if (res.type === "Success" && res.data) {
        refresh();
      } else if (res.type === "Failure") {
        setErrorMessage(getErrorMessage(res.error?.message || 'unknown-error'));
      }
    } catch {
      setErrorMessage(getErrorMessage('unknown-error'));
    } finally {
      setRestartingId(null);
    }
  };

  const getStatusColorClass = (status: string) => {
    switch (status) {
      case 'CREATED': return styles.statusCREATED;
      case 'QUEUED': return styles.statusQUEUED;
      case 'RUNNING': return styles.statusRUNNING;
      case 'COMPLETED': return styles.statusCOMPLETED;
      case 'FAILED': return styles.statusFAILED;
      default: return styles.statusCREATED;
    }
  };

  if (isLoading) {
    return (
      <div className={styles.container} style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <span>Loading...</span>
      </div>
    );
  }

  return (
    <motion.div 
      className={styles.container}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>Projects</h1>
          <p className={styles.subtitle}>Manage your experimental evolution projects.</p>
        </div>
        <button className={styles.createBtn} onClick={() => handleOpenModal()}>
          <span>➕</span> New Project
        </button>
      </div>

      {errorMessage && (
        <div style={{ color: '#ef4444', backgroundColor: '#fef2f2', padding: '1rem', borderRadius: '8px', marginBottom: '1rem' }}>
          <strong>Error:</strong> {errorMessage}
        </div>
      )}

      {pollingError && (
        <div style={{ color: '#92400e', backgroundColor: '#fffbeb', padding: '1rem', borderRadius: '8px', marginBottom: '1rem' }}>
          <strong>Warning:</strong> {pollingError}
        </div>
      )}

      {configsError && (
        <div style={{ color: '#92400e', backgroundColor: '#fffbeb', padding: '1rem', borderRadius: '8px', marginBottom: '1rem' }}>
          <strong>Warning:</strong> Configurations are unavailable: {configsError}
        </div>
      )}

      <div className={styles.grid}>
        <AnimatePresence>
          {projects.map((project) => (
            <motion.div
              key={project.id}
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className={styles.card}
            >
              <div className={styles.cardHeader}>
                <div>
                  <h3 className={styles.projectName}>{project.name}</h3>
                  <div className={`${styles.statusBadge} ${getStatusColorClass(project.status)}`}>
                    {project.status === 'COMPLETED' ? <span>✅</span> : null}
                    {project.status === 'RUNNING' ? <span>🔄</span> : null}
                    {project.status}
                  </div>
                </div>
              </div>
              <p className={styles.projectDescription}>
                {project.description || 'No description provided.'}
              </p>

              <div className={styles.cardDetails}>
                <div className={styles.detailRow}>
                  <span>Config ID:</span>
                  <strong>{project.configId || 'None'}</strong>
                </div>
                <div className={styles.detailRow}>
                  <span>Created:</span>
                  <strong>{new Date(project.createdAt).toLocaleDateString()}</strong>
                </div>
              </div>

              <div className={styles.cardActions}>
                <button
                  className={`${styles.actionBtn} ${styles.detailBtn}`}
                  onClick={() => navigate(`/projects/${project.id}`)}
                  title="View Metrics & Checkpoints"
                >
                  <span>Details</span>
                </button>
                <button
                  className={`${styles.actionBtn} ${styles.startBtn}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    handleStart(project.id);
                  }}
                  disabled={project.status === 'RUNNING' || project.status === 'QUEUED' || startingId === project.id}
                  title="Start Experimentation"
                >
                  {startingId === project.id ? <span>⏳ Starting</span> : <span>Start</span>}
                </button>
                {(project.status === 'COMPLETED' || project.status === 'FAILED') && (
                  <button
                    className={`${styles.actionBtn} ${styles.restartBtn}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleRestart(project.id);
                    }}
                    disabled={restartingId === project.id}
                    title="Reset to Created"
                  >
                    {restartingId === project.id ? <span>⏳ Restarting</span> : <span>Restart</span>}
                  </button>
                )}
                <button
                  className={`${styles.actionBtn} ${styles.editBtn}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    handleOpenModal(project);
                  }}
                  title="Edit Project"
                >
                  <span>Update</span>
                </button>
                <button
                  className={`${styles.actionBtn} ${styles.deleteBtn}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    handleDelete(project.id);
                  }}
                  title="Delete Project"
                >
                  <span>Delete</span>
                </button>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>
        
        {projects.length === 0 && (
          <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '4rem', color: '#64748b' }}>
            No projects found. Create your first project to get started.
          </div>
        )}
      </div>

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
                  {editingProject ? 'Edit Project' : 'Create New Project'}
                </h2>
                <button className={styles.closeBtn} onClick={handleCloseModal}>×</button>
              </div>

              <div className={styles.warningAlert}>
                <span>⚠️</span>
                <span><strong>Notice:</strong> Before starting the experimentation, make sure your credential is valid, otherwise it will fail.</span>
              </div>

              <div className={styles.formGroup}>
                <label>Name</label>
                <input 
                  type="text" 
                  value={formData.name} 
                  onChange={e => setFormData({...formData, name: e.target.value})} 
                  placeholder="My Experiment"
                  maxLength={50}
                />
              </div>

              <div className={styles.formGroup}>
                <label>Description (Optional)</label>
                <textarea 
                  value={formData.description || ''} 
                  onChange={e => setFormData({...formData, description: e.target.value || null})} 
                  placeholder="Describe the purpose of this project..."
                  style={{ minHeight: '60px' }}
                />
              </div>

              <div className={styles.formGroup}>
                <label>Configuration</label>
                <select 
                  value={formData.configId || ''} 
                  onChange={e => setFormData({...formData, configId: e.target.value ? Number(e.target.value) : null})}
                >
                  <option value="">-- Select an Existing Config --</option>
                  {configs.map(c => (
                    <option key={c.configId} value={c.configId}>
                      Config {c.configId} ({c.modelName})
                    </option>
                  ))}
                </select>
                <span className={styles.helperText}>Select a previously created configuration or leave empty to assign later.</span>
              </div>

              <div className={styles.formGroup}>
                <div className={styles.labelRow}>
                  <label>Initial Program Code</label>
                  <button type="button" className={styles.uploadBtn} onClick={() => initialProgramFileRef.current?.click()}>
                    📂 Upload file
                  </button>
                  <input
                    ref={initialProgramFileRef}
                    type="file"
                    style={{ display: 'none' }}
                    onChange={e => { if (e.target.files?.[0]) handleFileUpload('initialProgram', e.target.files[0]); e.target.value = ''; }}
                  />
                </div>
                <textarea
                  value={formData.initialProgram || ''}
                  onChange={e => setFormData({...formData, initialProgram: e.target.value || null})}
                  placeholder="def main():&#10;    pass"
                />
                <span className={styles.helperText}>The initial codebase for the evolution process.</span>
              </div>

              <div className={styles.formGroup}>
                <div className={styles.labelRow}>
                  <label>Evaluation Function / Fitness Logic</label>
                  <button type="button" className={styles.uploadBtn} onClick={() => evaluatorCodeFileRef.current?.click()}>
                    📂 Upload file
                  </button>
                  <input
                    ref={evaluatorCodeFileRef}
                    type="file"
                    style={{ display: 'none' }}
                    onChange={e => { if (e.target.files?.[0]) handleFileUpload('evaluatorCode', e.target.files[0]); e.target.value = ''; }}
                  />
                </div>
                <textarea
                  value={formData.evaluatorCode || ''}
                  onChange={e => setFormData({...formData, evaluatorCode: e.target.value || null})}
                  placeholder="def evaluate(code):&#10;    return fitness_score"
                />
                <span className={styles.helperText}>The Python/Java logic used to evaluate the correctness of the evolved programs.</span>
              </div>

              {modalError && (
                <div style={{ color: '#ef4444', backgroundColor: '#fef2f2', padding: '0.75rem', borderRadius: '8px', marginBottom: '1rem' }}>
                  {modalError}
                </div>
              )}

              <div className={styles.modalActions}>
                <button className={styles.cancelBtn} onClick={handleCloseModal} disabled={saving}>
                  Cancel
                </button>
                <button 
                  className={styles.saveBtn} 
                  onClick={handleSave} 
                  disabled={saving || !formData.name.trim()}
                >
                  {saving ? <span>⏳ Saving...</span> : 'Save Project'}
                </button>
              </div>

            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};

export default Projects;
