import React, { useCallback, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { request } from '../../api/api';
import styles from './ProjectDetail.module.css';
import { getErrorMessage } from '../../utils/errorsDescriptions';
import { usePolling } from '../../hooks/usePolling';
import { apiProjects } from './apiProjects';
import { Alert } from '../../components/ui/Alert';
import { LoadingSpinner } from '../../components/ui/LoadingSpinner';
import { Modal } from '../../components/ui/Modal';
import { usePageTitle } from '../../hooks/usePageTitle';

interface Job {
  id: number;
  projectId: number;
  status: string;
  createdAt: string;
  bestSolution: string | null;
  failureReason: string | null;
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
const CODE_TOKEN_REGEX = /(#.*$|"(?:\\.|[^"])*"|'(?:\\.|[^'])*'|\b(?:def|class|return|if|elif|else|for|while|try|except|finally|import|from|as|with|in|is|and|or|not|None|True|False|lambda|yield|break|continue|pass|raise)\b|\b\d+(?:\.\d+)?\b)/gm;
const PYTHON_KEYWORDS = new Set([
  'def', 'class', 'return', 'if', 'elif', 'else', 'for', 'while', 'try', 'except', 'finally',
  'import', 'from', 'as', 'with', 'in', 'is', 'and', 'or', 'not', 'lambda', 'yield', 'break',
  'continue', 'pass', 'raise',
]);

function tokenClass(token: string): string {
  if (token.startsWith('#')) return styles.codeComment;
  if (token.startsWith('"') || token.startsWith("'")) return styles.codeString;
  if (/^\d/.test(token)) return styles.codeNumber;
  if (token === 'True' || token === 'False' || token === 'None') return styles.codeLiteral;
  if (PYTHON_KEYWORDS.has(token)) return styles.codeKeyword;
  return '';
}

function renderHighlightedCode(code: string) {
  return code.split('\n').map((line, lineIndex) => {
    const parts = line.split(CODE_TOKEN_REGEX).filter(part => part !== '');
    return (
      <div className={styles.codeLine} key={`${lineIndex}-${line}`}>
        <span className={styles.lineNumber}>{lineIndex + 1}</span>
        <span className={styles.lineContent}>
          {parts.length === 0 ? ' ' : parts.map((part, partIndex) => {
            const className = tokenClass(part);
            return className
              ? <span className={className} key={`${lineIndex}-${partIndex}`}>{part}</span>
              : <React.Fragment key={`${lineIndex}-${partIndex}`}>{part}</React.Fragment>;
          })}
        </span>
      </div>
    );
  });
}

const ProjectDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const projectId = Number(id);

  const fetchDetail = useCallback(async () => {
    const [projectRes, jobsRes] = await Promise.all([
      apiProjects.getById(projectId),
      request<Job[]>(`/api/projects/${projectId}/jobs`),
    ]);
    if (projectRes.type === 'Failure') {
      throw new Error(getErrorMessage(projectRes.error));
    }
    if (jobsRes.type === 'Failure') {
      throw new Error(getErrorMessage(jobsRes.error));
    }
    const jobs = jobsRes.data ?? [];
    if (jobs.length === 0) {
      return {
        project: projectRes.data,
        job: null,
        metrics: [],
        checkpoints: [],
        hasRunHistory: false,
      };
    }
    const latestJob = [...jobs].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )[0];
    const [metricsRes, checkpointsRes] = await Promise.all([
      request<Metric[]>(`/api/jobs/${latestJob.id}/metrics`),
      request<Checkpoint[]>(`/api/jobs/${latestJob.id}/checkpoints`),
    ]);
    return {
      project: projectRes.data,
      job: latestJob,
      metrics: metricsRes.type === 'Success' && metricsRes.data
        ? metricsRes.data.sort((a, b) => a.iteration - b.iteration)
        : [],
      checkpoints: checkpointsRes.type === 'Success' && checkpointsRes.data
        ? checkpointsRes.data.sort((a, b) => a.iteration - b.iteration)
        : [],
      hasRunHistory: true,
    };
  }, [projectId]);

  const { data: detail, isLoading, error } = usePolling(
    fetchDetail,
    4000,
    (d) => !d.job || d.job.status === 'COMPLETED' || d.job.status === 'FAILED'
  );

  const job = detail?.job ?? null;
  const project = detail?.project ?? null;
  const metrics = detail?.metrics ?? [];
  const checkpoints = detail?.checkpoints ?? [];
  const hasRunHistory = detail?.hasRunHistory ?? true;

  const [selectedCheckpoint, setSelectedCheckpoint] = useState<Checkpoint | null>(null);
  const [showFinalSolution, setShowFinalSolution] = useState(false);
  const [hoveredBar, setHoveredBar] = useState<number | null>(null);
  usePageTitle(project?.name ?? 'Project');

  const maxScore = metrics.length > 0 ? Math.max(...metrics.map(m => m.fitnessScore)) : 1;

  if (isLoading) {
    return <LoadingSpinner label="Loading project details" />;
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
          <h1>{project?.name ?? `Project ${id}`}</h1>
          {job && (
            <span className={`${styles.statusBadge} ${styles[`status${job.status}`] ?? ''}`}>
              {job.status === 'COMPLETED'}
              {job.status === 'RUNNING'}
              {job.status === 'FAILED'}
              {job.status}
            </span>
          )}
        </div>
      </div>

      {error && (
        <Alert variant="error">{error}</Alert>
      )}

      {!hasRunHistory && (
        <Alert variant="info">
          This project has no run history yet. Start the project from the Projects page to begin.
        </Alert>
      )}

      {job?.failureReason && (
        <Alert variant="error" title="Failure reason">
          <strong>Failure reason:</strong> {job.failureReason}
        </Alert>
      )}

      {job?.bestSolution && (
        <motion.div
          className={styles.finalResultCard}
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
        >
          <div className={styles.finalResultLeft}>
            <span className={styles.finalResultIcon}></span>
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
          <Modal onClose={() => setShowFinalSolution(false)} ariaLabelledBy="final-solution-title" className={styles.modal}>
              <div className={styles.modalHeader}>
                <h3 id="final-solution-title">Final Best Solution</h3>
                <button className={styles.closeBtn} onClick={() => setShowFinalSolution(false)} aria-label="Close modal">×</button>
              </div>
              <div className={styles.codeBlock}>{renderHighlightedCode(job.bestSolution)}</div>
          </Modal>
        )}
      </AnimatePresence>

      {/* ── Checkpoint solution modal ── */}
      <AnimatePresence>
        {selectedCheckpoint && (
          <Modal onClose={() => setSelectedCheckpoint(null)} ariaLabelledBy="checkpoint-solution-title" className={styles.modal}>
              <div className={styles.modalHeader}>
                <h3 id="checkpoint-solution-title">Checkpoint — Iteration #{selectedCheckpoint.iteration}</h3>
                <button className={styles.closeBtn} onClick={() => setSelectedCheckpoint(null)} aria-label="Close modal">×</button>
              </div>
              <div className={styles.codeBlock}>{renderHighlightedCode(selectedCheckpoint.solution)}</div>
          </Modal>
        )}
      </AnimatePresence>
    </motion.div>
  );
};

export default ProjectDetail;
