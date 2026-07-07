import { Link } from 'react-router-dom';

interface EmptyStateProps {
  title: string;
  description: string;
  actionLabel: string;
  actionTo: string;
  secondaryLabel?: string;
  secondaryTo?: string;
}

export function EmptyState({
  title,
  description,
  actionLabel,
  actionTo,
  secondaryLabel,
  secondaryTo,
}: EmptyStateProps) {
  return (
    <article className="empty-state card">
      <h3>{title}</h3>
      <p className="muted">{description}</p>
      <Link className="primary-link empty-state-cta" to={actionTo}>
        {actionLabel}
      </Link>
      {secondaryLabel && secondaryTo && (
        <Link className="empty-state-secondary" to={secondaryTo}>
          {secondaryLabel}
        </Link>
      )}
    </article>
  );
}
