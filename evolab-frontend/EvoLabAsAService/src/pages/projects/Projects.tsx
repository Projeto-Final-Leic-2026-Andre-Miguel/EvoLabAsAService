import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { apiProjects, type Project, type CreateProjectInput, type UpdateProjectDetailsInput } from './apiProjects';
import { apiConfigs, type Config } from '../configs/apiConfigs';
import styles from './Projects.module.css';

const Projects: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [configs, setConfigs] = useState<Config[]>([]);
  const [isLoading, setIsLoading] = useState(true);
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
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setIsLoading(true);
    try {
      const [projectsResult, configsResult] = await Promise.all([
        apiProjects.getAll(),
        apiConfigs.getAllMyConfigs().catch(() => ({ type: "Success" as const, data: [], error: null })) // Default empty array if no endpoint or error
      ]);

      if (projectsResult.type === "Success" && projectsResult.data) {
        setProjects(projectsResult.data);
      }
      
      if (configsResult && configsResult.type === "Success" && configsResult.data) {
        setConfigs(configsResult.data);
      }
    } catch (error) {
      console.error('Error fetching data:', error);
    } finally {
      setIsLoading(false);
    }
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
  };

  const handleSave = async () => {
    setSaving(true);
    setErrorMessage(null);
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
          setProjects(prev => prev.map(p => p.id === editingProject.id ? res.data! : p));
          handleCloseModal();
        } else if (res.type === "Failure") {
          setErrorMessage(`Update failed: ${res.error?.message || 'Unknown Error'}`);
        }
      } else {
        const res = await apiProjects.create(formData);
        if (res.type === "Success" && res.data) {
          setProjects(prev => [...prev, res.data!]);
          handleCloseModal();
        } else if (res.type === "Failure") {
          setErrorMessage(`Creation failed: ${res.error?.message || 'Unknown Error'}`);
        }
      }
    } catch (error) {
      console.error('Error saving project:', error);
      setErrorMessage('Network error while saving project.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    setErrorMessage(null);
    try {
      await fetch(`/api/projects/${id}`, { method: 'DELETE' });
      setProjects(prev => prev.filter(p => p.id !== id));
    } catch (error) {
      console.error('Failed to delete project:', error);
      setErrorMessage('Failed to delete the project.');
    }
  };

  const handleStart = async (id: number) => {
    setStartingId(id);
    setErrorMessage(null);
    try {
      const res = await apiProjects.start(id);
      if (res.type === "Success") {
        fetchData();
      } else {
        const title = res.error?.message || 'Unknown Error';
        setErrorMessage(`Failed to start: ${title}`);
      }
    } catch (error) {
      console.error('Error starting project:', error);
      setErrorMessage('A network error occurred while starting.');
    } finally {
      setStartingId(null);
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
                  className={`${styles.actionBtn} ${styles.startBtn}`} 
                  onClick={() => handleStart(project.id)}
                  disabled={project.status === 'RUNNING' || project.status === 'QUEUED' || startingId === project.id}
                  title="Start Experimentation"
                >
                  {startingId === project.id ? <span>⏳ Starting</span> : <span>Start</span>}
                </button>
                <button 
                  className={`${styles.actionBtn} ${styles.editBtn}`} 
                  onClick={() => handleOpenModal(project)}
                  title="Edit Project"
                >
                  <span>Update</span>
                </button>
                <button 
                  className={`${styles.actionBtn} ${styles.deleteBtn}`} 
                  onClick={() => handleDelete(project.id)}
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
                      Config #{c.configId} ({c.modelName})
                    </option>
                  ))}
                </select>
                <span className={styles.helperText}>Select a previously created configuration or leave empty to assign later.</span>
              </div>

              <div className={styles.formGroup}>
                <label>Initial Program Code</label>
                <textarea 
                  value={formData.initialProgram || ''} 
                  onChange={e => setFormData({...formData, initialProgram: e.target.value || null})} 
                  placeholder="def main():&#10;    pass"
                />
                <span className={styles.helperText}>The initial codebase for the evolution process.</span>
              </div>

              <div className={styles.formGroup}>
                <label>Evaluation Function / Fitness Logic</label>
                <textarea 
                  value={formData.evaluatorCode || ''} 
                  onChange={e => setFormData({...formData, evaluatorCode: e.target.value || null})} 
                  placeholder="def evaluate(code):&#10;    return fitness_score"
                />
                <span className={styles.helperText}>The Python/Java logic used to evaluate the correctness of the evolved programs.</span>
              </div>

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
