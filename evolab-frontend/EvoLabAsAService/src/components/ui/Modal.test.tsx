// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ConfirmDialog } from './ConfirmDialog';
import { Modal } from './Modal';

afterEach(cleanup);

describe('Modal', () => {
  it('closes on Escape and overlay click but not on dialog click', () => {
    const onClose = vi.fn();
    render(
      <Modal ariaLabel="Example modal" onClose={onClose}>
        <button>Inside</button>
      </Modal>,
    );

    fireEvent.click(screen.getByRole('dialog'));
    expect(onClose).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('dialog').parentElement!);
    expect(onClose).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});

describe('ConfirmDialog', () => {
  it('runs the destructive action from a labelled confirmation dialog', () => {
    const onConfirm = vi.fn();
    render(
      <ConfirmDialog
        isOpen
        title="Delete project?"
        message="This cannot be undone."
        onCancel={() => undefined}
        onConfirm={onConfirm}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

    expect(screen.getByRole('dialog', { name: 'Delete project?' })).toBeTruthy();
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });
});
