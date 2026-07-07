import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/api';
import { useI18n } from '../context/I18nContext';

export function AdminPanelLink() {
  const { t } = useI18n();
  const [allowed, setAllowed] = useState(false);

  useEffect(() => {
    api
      .devTools()
      .then((response) => setAllowed(response.devTools))
      .catch(() => setAllowed(false));
  }, []);

  if (!allowed) {
    return null;
  }

  return (
    <Link to="/admin" className="admin-panel-link" title={t('adminTitle')}>
      {t('adminOpen')}
    </Link>
  );
}
