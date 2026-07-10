import styles from './LoadingSpinner.module.css';

interface LoadingSpinnerProps {
  label?: string;
  fullPage?: boolean;
}

export function LoadingSpinner({ label = 'Loading', fullPage = true }: LoadingSpinnerProps) {
  return (
    <div className={`${styles.loading} ${fullPage ? styles.fullPage : ''}`} role="status" aria-live="polite">
      <span className={styles.spinner} aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}
