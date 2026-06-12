import { createContext } from 'react';

export interface ToastContextValue {
  showSuccess: (message: string) => void;
}

export const ToastContext = createContext<ToastContextValue | undefined>(undefined);
