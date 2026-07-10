import { useCallback, useState, type ReactNode } from 'react';
import { ToastContext } from './ToastContext';
import styles from './ToastProvider.module.css';

interface Toast {
  id: number;
  message: string;
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const showSuccess = useCallback((message: string) => {
    const id = Date.now() + Math.random();
    setToasts((current) => [...current, { id, message }]);
    window.setTimeout(() => {
      setToasts((current) => current.filter((toast) => toast.id !== id));
    }, 3500);
  }, []);

  return (
    <ToastContext.Provider value={{ showSuccess }}>
      {children}
      <div className={styles.viewport} aria-live="polite" aria-atomic="false">
        {toasts.map((toast) => (
          <div className={styles.toast} role="status" key={toast.id}>
            <span className={styles.mark} aria-hidden="true">OK</span>
            <span>{toast.message}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
