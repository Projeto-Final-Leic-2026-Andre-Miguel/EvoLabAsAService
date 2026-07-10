import type { ReactNode } from 'react';
import styles from './Alert.module.css';

type AlertVariant = 'error' | 'warning' | 'info';

interface AlertProps {
  children: ReactNode;
  variant?: AlertVariant;
  title?: string;
  className?: string;
}

export function Alert({ children, variant = 'info', title, className }: AlertProps) {
  const defaultTitle = variant === 'error' ? 'Error' : variant === 'warning' ? 'Warning' : 'Info';

  return (
    <div className={`${styles.alert} ${styles[variant]} ${className ?? ''}`} role={variant === 'error' ? 'alert' : 'status'}>
      <span className={styles.mark} aria-hidden="true">
        {variant === 'error' ? '!' : variant === 'warning' ? '!' : 'i'}
      </span>
      <div>
        <strong className={styles.title}>{title ?? defaultTitle}</strong>
        <div className={styles.content}>{children}</div>
      </div>
    </div>
  );
}
