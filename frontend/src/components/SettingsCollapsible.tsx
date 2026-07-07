import { useEffect, useState } from 'react';

interface SettingsCollapsibleProps {
  title: string;
  sectionId?: string;
  initialOpen?: boolean;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  children: React.ReactNode;
}

export function SettingsCollapsible({
  title,
  sectionId,
  initialOpen = false,
  open: controlledOpen,
  onOpenChange,
  children,
}: SettingsCollapsibleProps) {
  const [internalOpen, setInternalOpen] = useState(initialOpen);
  const open = controlledOpen ?? internalOpen;

  useEffect(() => {
    if (initialOpen) {
      setInternalOpen(true);
    }
  }, [initialOpen]);

  function toggle() {
    const next = !open;
    if (controlledOpen === undefined) {
      setInternalOpen(next);
    }
    onOpenChange?.(next);
  }

  return (
    <article className={`card settings-collapsible ${open ? 'open' : ''}`} id={sectionId}>
      <button type="button" className="settings-collapsible-trigger" onClick={toggle} aria-expanded={open}>
        <h3>{title}</h3>
        <span className="settings-collapsible-chevron" aria-hidden="true" />
      </button>
      {open && <div className="settings-collapsible-body stack">{children}</div>}
    </article>
  );
}
