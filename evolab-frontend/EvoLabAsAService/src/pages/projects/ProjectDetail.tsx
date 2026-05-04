import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { request } from '../../api/api';
import styles from './ProjectDetail.module.css';
import { getErrorMessage } from '../../utils/errorsDescriptions';

interface Job {
  id: number;
  projectId: number;
  status: string;
  createdAt: string;
  bestSolution: string | null;
}

interface Metric {
  id: number;
  jobId: number;
  iteration: number;
  fitnessScore: number;
  executionTime: number | null;
  createdAt: string;
}

interface Checkpoint {
  id: number;
  jobId: number;
  metricsId: number;
  iteration: number;
  solution: string;
  createdAt: string;
}

const BAR_MAX_HEIGHT = 160;

const ProjectDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [metrics, setMetrics] = useState<Metric[]>([]);
  const [checkpoints, setCheckpoints] = useState<Checkpoint[]>([]);
  const [job, setJob] = useState<Job | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedCheckpoint, setSelectedCheckpoint] = useState<Checkpoint | null>(null);
  const [showFinalSolution, setShowFinalSolution] = useState(false);
  const [hoveredBar, setHoveredBar] = useState<number | null>(null);

  useEffect(() => {
    if (!id) return;
    fetchData(Number(id));
  }, [id]);

  const fetchData = async (projectId: number) => {
    setIsLoading(true);
    setError(null);
    try {
      const jobsRes = await request<Job[]>(`/api/projects/${projectId}/jobs`);
      if (jobsRes.type === 'Failure') {
        setError(getErrorMessage(jobsRes.error?.message || 'unknown-error'));
        setIsLoading(false);
        return;
      }
      if (jobsRes.data.length === 0) {
        setError('This project has no run history yet. Start the project from the Projects page to begin.');
        setIsLoading(false);
        return;
      }

      const latestJob = jobsRes.data.sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      )[0];
      setJob(latestJob);

      const [metricsRes, checkpointsRes] = await Promise.all([
        request<Metric[]>(`/api/jobs/${latestJob.id}/metrics`),
        request<Checkpoint[]>(`/api/jobs/${latestJob.id}/checkpoints`),
      ]);

      if (metricsRes.type === 'Success' && metricsRes.data) {
        setMetrics(metricsRes.data.sort((a, b) => a.iteration - b.iteration));
      }
      if (checkpointsRes.type === 'Success' && checkpointsRes.data) {
        setCheckpoints(checkpointsRes.data.sort((a, b) => a.iteration - b.iteration));
      }
    } catch {
      setError('Failed to load project data.');
    } finally {
      setIsLoading(false);
    }
  };

  const maxScore = metrics.length > 0 ? Math.max(...metrics.map(m => m.fitnessScore)) : 1;

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
      <div className={styles.pageHeader}>
        <button className={styles.backBtn} onClick={() => navigate('/projects')}>
          ← Back
        </button>
        <div className={styles.pageTitle}>
          <h1>Project #{id}</h1>
          {job && (
            <span className={`${styles.statusBadge} ${styles[`status${job.status}`] ?? ''}`}>
              {job.status === 'COMPLETED' && '✅ '}
              {job.status === 'RUNNING' && '🔄 '}
              {job.status === 'FAILED' && '❌ '}
              {job.status}
            </span>
          )}
        </div>
      </div>

      {error && (
        <div style={{ color: '#ef4444', background: '#fef2f2', padding: '1rem', borderRadius: '8px', marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {job?.bestSolution && (
        <motion.div
          className={styles.finalResultCard}
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
        >
          <div className={styles.finalResultLeft}>
            <span className={styles.finalResultIcon}>🏆</span>
            <div>
              <p className={styles.finalResultTitle}>Final Result</p>
              <p className={styles.finalResultSub}>
                Best solution found after {metrics.length} iteration{metrics.length !== 1 ? 's' : ''}
                {metrics.length > 0 && ` — score: ${(Math.max(...metrics.map(m => m.fitnessScore)) * 100).toFixed(1)}%`}
              </p>
            </div>
          </div>
          <button className={styles.viewCodeBtn} onClick={() => setShowFinalSolution(true)}>
            View Final Solution
          </button>
        </motion.div>
      )}

      <div className={styles.columns}>

        {/* ── Left column: Metrics ── */}
        <div className={styles.column}>
          <div className={styles.sectionHeader}>
            <h2 className={styles.sectionTitle}>Metrics</h2>
            <span className={styles.sectionCount}>{metrics.length} iterations</span>
          </div>

          {metrics.length === 0 ? (
            <div className={styles.empty}>No metrics recorded yet.</div>
          ) : (
            <>
              <div className={styles.card}>
                <p className={styles.chartLabel}>Fitness Score per Iteration</p>
                <div className={styles.chart}>
                  {metrics.map((m) => {
                    const height = Math.max(8, (m.fitnessScore / maxScore) * BAR_MAX_HEIGHT);
                    const isHovered = hoveredBar === m.id;
                    return (
                      <div
                        key={m.id}
                        className={styles.barWrapper}
                        onMouseEnter={() => setHoveredBar(m.id)}
                        onMouseLeave={() => setHoveredBar(null)}
                      >
                        {isHovered && (
                          <div className={styles.tooltip}>
                            <strong>{(m.fitnessScore * 100).toFixed(1)}%</strong>
                            {m.executionTime != null && <span>{m.executionTime}s</span>}
                          </div>
                        )}
                        <motion.div
                          className={`${styles.bar} ${m.fitnessScore === maxScore ? styles.barBest : ''}`}
                          style={{ height }}
                          initial={{ height: 0 }}
                          animate={{ height }}
                          transition={{ duration: 0.5, delay: m.iteration * 0.04 }}
                        />
                        <span className={styles.barLabel}>#{m.iteration}</span>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className={styles.card}>
                <p className={styles.chartLabel}>All Iterations</p>
                <div className={styles.metricTable}>
                  <div className={styles.metricTableHeader}>
                    <span>Iteration</span>
                    <span>Fitness Score</span>
                    <span>Exec. Time</span>
                  </div>
                  {metrics.map((m) => (
                    <div key={m.id} className={`${styles.metricRow} ${m.fitnessScore === maxScore ? styles.bestRow : ''}`}>
                      <span>#{m.iteration}</span>
                      <span className={styles.score}>{(m.fitnessScore * 100).toFixed(1)}%</span>
                      <span>{m.executionTime != null ? `${m.executionTime}s` : '—'}</span>
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>

        {/* ── Right column: Checkpoints ── */}
        <div className={styles.column}>
          <div className={styles.sectionHeader}>
            <h2 className={styles.sectionTitle}>Checkpoints</h2>
            <span className={styles.sectionCount}>{checkpoints.length} saved</span>
          </div>

          {checkpoints.length === 0 ? (
            <div className={styles.empty}>No checkpoints saved yet.</div>
          ) : (
            <div className={styles.checkpointList}>
              {checkpoints.map((cp) => {
                const metric = metrics.find(m => m.id === cp.metricsId);
                const isBest = metric != null && metric.fitnessScore === maxScore;
                return (
                  <motion.div
                    key={cp.id}
                    className={`${styles.checkpointCard} ${isBest ? styles.checkpointBest : ''}`}
                    initial={{ opacity: 0, x: 20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ duration: 0.3, delay: cp.id * 0.05 }}
                  >
                    <div className={styles.checkpointHeader}>
                      <div className={styles.checkpointMeta}>
                        <span className={styles.checkpointIteration}>Iteration #{cp.iteration}</span>
                        {isBest && <span className={styles.bestBadge}>Best</span>}
                      </div>
                      {metric && (
                        <span className={styles.checkpointScore}>
                          {(metric.fitnessScore * 100).toFixed(1)}%
                        </span>
                      )}
                    </div>
                    <div className={styles.checkpointDate}>
                      {new Date(cp.createdAt).toLocaleString()}
                    </div>
                    <button
                      className={styles.viewCodeBtn}
                      onClick={() => setSelectedCheckpoint(cp)}
                    >
                      View Solution
                    </button>
                  </motion.div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* ── Final solution modal ── */}
      <AnimatePresence>
        {showFinalSolution && job?.bestSolution && (
          <motion.div
            className={styles.modalOverlay}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setShowFinalSolution(false)}
          >
            <motion.div
              className={styles.modal}
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 30 }}
              onClick={e => e.stopPropagation()}
            >
              <div className={styles.modalHeader}>
                <h3>🏆 Final Best Solution</h3>
                <button className={styles.closeBtn} onClick={() => setShowFinalSolution(false)}>×</button>
              </div>
              <pre className={styles.codeBlock}>{job.bestSolution}</pre>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Checkpoint solution modal ── */}
      <AnimatePresence>
        {selectedCheckpoint && (
          <motion.div
            className={styles.modalOverlay}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setSelectedCheckpoint(null)}
          >
            <motion.div
              className={styles.modal}
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 30 }}
              onClick={e => e.stopPropagation()}
            >
              <div className={styles.modalHeader}>
                <h3>Checkpoint — Iteration #{selectedCheckpoint.iteration}</h3>
                <button className={styles.closeBtn} onClick={() => setSelectedCheckpoint(null)}>×</button>
              </div>
              <pre className={styles.codeBlock}>{selectedCheckpoint.solution}</pre>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};

export default ProjectDetail;
