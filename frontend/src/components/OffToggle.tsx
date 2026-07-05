interface OffToggleProps {
  active: boolean;
  onChange: (next: boolean) => void;
}

export function OffToggle({ active, onChange }: OffToggleProps) {
  return (
    <button
      type="button"
      className={`off-toggle ${active ? 'is-on' : 'is-off'}`}
      onClick={() => onChange(!active)}
      aria-pressed={active}
    >
      <span className="off-toggle-pill" />
      <span className="off-toggle-label">{active ? 'ON' : 'OFF'}</span>
    </button>
  );
}
