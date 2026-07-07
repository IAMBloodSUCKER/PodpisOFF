import { Link } from 'react-router-dom';

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  actionLabel?: string;
  actionTo?: string;
  onActionClick?: () => void;
  backTo?: string;
  backLabel?: string;
}

export function PageHeader({
  title,
  subtitle,
  actionLabel,
  actionTo,
  onActionClick,
  backTo,
  backLabel,
}: PageHeaderProps) {
  return (
    <header className="page-header-block stack">
      {backTo && backLabel && (
        <Link to={backTo} className="page-back-link">
          ← {backLabel}
        </Link>
      )}
      <div className="page-header">
        <div className="page-header-text">
          <h2>{title}</h2>
          {subtitle && <p className="muted">{subtitle}</p>}
        </div>
        {actionLabel && actionTo && (
          <Link className="primary-link" to={actionTo}>
            {actionLabel}
          </Link>
        )}
        {actionLabel && !actionTo && onActionClick && (
          <button type="button" className="primary-link" onClick={onActionClick}>
            {actionLabel}
          </button>
        )}
      </div>
    </header>
  );
}
