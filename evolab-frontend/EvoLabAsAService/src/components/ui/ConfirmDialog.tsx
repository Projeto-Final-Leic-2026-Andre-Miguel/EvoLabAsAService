import { Modal } from './Modal';
import styles from './ConfirmDialog.module.css';

interface ConfirmDialogProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  isConfirming?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({
  isOpen,
  title,
  message,
  confirmLabel = 'Delete',
  cancelLabel = 'Cancel',
  isConfirming = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  if (!isOpen) return null;

  return (
    <Modal ariaLabel={title} onClose={onCancel} className={styles.dialog}>
      <div className={styles.icon} aria-hidden="true">!</div>
      <h2 className={styles.title}>{title}</h2>
      <p className={styles.message}>{message}</p>
      <div className={styles.actions}>
        <button type="button" className={styles.cancelButton} onClick={onCancel} disabled={isConfirming}>
          {cancelLabel}
        </button>
        <button type="button" className={styles.confirmButton} onClick={onConfirm} disabled={isConfirming}>
          {isConfirming ? 'Deleting...' : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
