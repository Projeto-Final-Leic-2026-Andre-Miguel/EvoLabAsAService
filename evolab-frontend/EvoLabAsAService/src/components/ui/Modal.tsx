import { useEffect, type ReactNode } from 'react';
import styles from './Modal.module.css';

interface ModalProps {
  children: ReactNode;
  onClose: () => void;
  ariaLabel?: string;
  ariaLabelledBy?: string;
  className?: string;
  overlayClassName?: string;
}

export function Modal({
  children,
  onClose,
  ariaLabel,
  ariaLabelledBy,
  className,
  overlayClassName,
}: ModalProps) {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  return (
    <div
      className={`${styles.overlay} ${overlayClassName ?? ''}`}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={ariaLabel}
        aria-labelledby={ariaLabelledBy}
        className={`${styles.modal} ${className ?? ''}`}
      >
        {children}
      </div>
    </div>
  );
}
