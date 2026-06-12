import { useEffect } from 'react';

export function usePageTitle(title: string) {
  useEffect(() => {
    document.title = title ? `${title} — EvoLab` : 'EvoLab';
  }, [title]);
}
