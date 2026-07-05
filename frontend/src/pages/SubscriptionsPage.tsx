import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/api';
import { useI18n } from '../context/I18nContext';
import { Subscription } from '../types/api';
import { currency, dateLabel } from '../utils/format';

type Filter = 'all' | 'active' | 'paused' | 'off';

function resolveStatus(subscription: Subscription): Filter {
  if (subscription.active) return 'active';
  const targetDate = new Date(subscription.nextBillingDate);
  const now = new Date();
  return targetDate.getTime() > now.getTime() ? 'paused' : 'off';
}

export function SubscriptionsPage() {
  const [items, setItems] = useState<Subscription[]>([]);
  const [filter, setFilter] = useState<Filter>('all');
  const [error, setError] = useState('');
  const { t } = useI18n();

  async function loadSubscriptions() {
    try {
      const list = await api.listSubscriptions();
      setItems(list);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load subscriptions');
    }
  }

  useEffect(() => {
    void loadSubscriptions();
  }, []);

  const filteredItems = useMemo(() => {
    if (filter === 'all') return items;
    return items.filter((item) => resolveStatus(item) === filter);
  }, [filter, items]);

  async function remove(id: number) {
    await api.deleteSubscription(id);
    await loadSubscriptions();
  }

  return (
    <section className="stack">
      <div className="row-between">
        <h2>{t('subscriptionsTitle')}</h2>
        <Link className="primary-link" to="/subscriptions/new">
          {t('addSubscription')}
        </Link>
      </div>
      <div className="filters">
        <button type="button" className={filter === 'all' ? 'active' : ''} onClick={() => setFilter('all')}>
          {t('filterAll')}
        </button>
        <button type="button" className={filter === 'active' ? 'active' : ''} onClick={() => setFilter('active')}>
          {t('filterActive')}
        </button>
        <button type="button" className={filter === 'paused' ? 'active' : ''} onClick={() => setFilter('paused')}>
          {t('filterPaused')}
        </button>
        <button type="button" className={filter === 'off' ? 'active' : ''} onClick={() => setFilter('off')}>
          {t('filterOff')}
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      <div className="stack">
        {filteredItems.map((item) => {
          const status = resolveStatus(item);
          return (
            <article className="card subscription-card" key={item.id}>
              <div className="row-between">
                <div>
                  <h3>{item.title}</h3>
                  <p className="muted">{item.category}</p>
                </div>
                <span className={`badge ${status}`}>
                  {status === 'active' ? t('statusActive') : status === 'paused' ? t('statusPaused') : t('statusOff')}
                </span>
              </div>
              <div className="row-between">
                <p>{currency(item.amount, item.currency)}</p>
                <p className="muted">{dateLabel(item.nextBillingDate)}</p>
              </div>
              <div className="actions">
                <Link to={`/subscriptions/${item.id}/edit`}>{t('editSubscription')}</Link>
                <button type="button" className="danger" onClick={() => void remove(item.id)}>
                  {t('delete')}
                </button>
              </div>
            </article>
          );
        })}
        {!filteredItems.length && <p className="muted">{t('empty')}</p>}
      </div>
    </section>
  );
}
