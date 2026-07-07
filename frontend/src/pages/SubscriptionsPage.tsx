import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/api';
import { resolveApiError } from '../api/errors';
import { ConfirmModal } from '../components/ConfirmModal';
import { EmptyState } from '../components/EmptyState';
import { SubscriptionStatusLegend } from '../components/SubscriptionStatusLegend';
import { ConceptGuide } from '../components/ConceptGuide';
import { PageHeader } from '../components/PageHeader';
import { PlanLimitBanner } from '../components/PlanLimitBanner';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/I18nContext';
import { Subscription } from '../types/api';
import { currency, dateLabel } from '../utils/format';
import { atSubscriptionCreateLimit, effectivePlan, isPro } from '../utils/plan';
import { formatSubscriptionPrice } from '../utils/subscriptionFormat';
import { resolveSubscriptionStatusFromItem } from '../utils/subscriptionStatus';
import { clearSubscriptionDraft, hasSubscriptionDraft } from '../utils/subscriptionDraft';

type Filter = 'all' | 'active' | 'paused' | 'off';

export function SubscriptionsPage() {
  const [items, setItems] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<Filter>('all');
  const [error, setError] = useState('');
  const [exportSuccess, setExportSuccess] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<Subscription | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [draftPending, setDraftPending] = useState(hasSubscriptionDraft);
  const { t, locale } = useI18n();
  const { user } = useAuth();
  const plan = effectivePlan(user);
  const blockedCreate = atSubscriptionCreateLimit(plan, items.length);

  async function loadSubscriptions() {
    try {
      const list = await api.listSubscriptions();
      setItems(list);
      setError('');
    } catch (err) {
      setError(resolveApiError(err, t));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadSubscriptions();
  }, []);

  const filteredItems = useMemo(() => {
    if (filter === 'all') return items;
    return items.filter((item) => resolveSubscriptionStatusFromItem(item) === filter);
  }, [filter, items]);

  async function confirmRemove() {
    if (!deleteTarget) return;
    setDeleting(true);
    setError('');
    try {
      await api.deleteSubscription(deleteTarget.id);
      setDeleteTarget(null);
      await loadSubscriptions();
    } catch (err) {
      setError(resolveApiError(err, t));
    } finally {
      setDeleting(false);
    }
  }

  async function onExport() {
    setError('');
    setExportSuccess('');
    try {
      const blob = await api.exportSubscriptionsExcel();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'podpisoff-subscriptions.xlsx';
      link.click();
      URL.revokeObjectURL(url);
      setExportSuccess(t('subscriptionsExportSuccess'));
    } catch (err) {
      setError(resolveApiError(err, t));
    }
  }

  return (
    <section className="stack">
      <PageHeader
        title={t('subscriptionsTitle')}
        subtitle={t('subscriptionsSubtitle')}
        actionLabel={!loading && items.length > 0 && !blockedCreate ? t('addSubscription') : undefined}
        actionTo={!loading && items.length > 0 && !blockedCreate ? '/subscriptions/new' : undefined}
      />

      {!loading && <PlanLimitBanner user={user} subscriptionCount={items.length} focus="subscriptions" />}

      {isPro(plan) && !loading && items.length > 0 && (
        <article className="card stack subscriptions-export-bar">
          <div className="row-between">
            <div>
              <h3>{t('subscriptionsExport')}</h3>
              <p className="muted">{t('subscriptionsExportHint')}</p>
            </div>
            <button type="button" className="primary" onClick={() => void onExport()}>
              {t('subscriptionsExport')}
            </button>
          </div>
          {exportSuccess && <p className="success">{exportSuccess}</p>}
        </article>
      )}

      <ConceptGuide
        guideId="subscriptions"
        titleKey="conceptSubsTitle"
        bodyKey="conceptSubsBody"
        compareKey="conceptSubsVsReminders"
        preferCollapsed
      />

      {!loading && draftPending && (
        <article className="card subscription-draft-banner stack">
          <p>{t('subscriptionDraftResume')}</p>
          <div className="actions">
            <Link to="/subscriptions/new" className="primary">
              {t('subscriptionDraftResumeAction')}
            </Link>
            <button
              type="button"
              className="ghost"
              onClick={() => {
                clearSubscriptionDraft();
                setDraftPending(false);
              }}
            >
              {t('subscriptionDraftDiscard')}
            </button>
          </div>
        </article>
      )}

      {error && <p className="error">{error}</p>}

      {loading ? (
        <p className="muted subscriptions-loading">{t('loading')}</p>
      ) : !items.length ? (
        <EmptyState
          title={t('subscriptionsEmptyTitle')}
          description={t('subscriptionsEmptyDesc')}
          actionLabel={t('addSubscription')}
          actionTo="/subscriptions/new"
        />
      ) : (
        <>
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
          <SubscriptionStatusLegend />

          <div className="stack">
            {filteredItems.map((item) => {
              const status = resolveSubscriptionStatusFromItem(item);
              return (
                <article className="card subscription-card" key={item.id}>
                  <div className="row-between">
                    <div>
                      <h3>{item.title}</h3>
                      <p className="muted">{item.category}</p>
                      {item.note?.trim() && <p className="subscription-note">{item.note}</p>}
                    </div>
                    <span className={`badge ${status}`}>
                      {status === 'active' ? t('statusActive') : status === 'paused' ? t('statusPaused') : t('statusOff')}
                    </span>
                  </div>
                  <div className="row-between subscription-amount-row">
                    <p className="subscription-amount">
                      {formatSubscriptionPrice(item.amount, item.currency, item.billingPeriod ?? 'MONTHLY', locale, currency)}
                    </p>
                    <p className="muted">
                      {t('formNextDate')}: {dateLabel(item.nextBillingDate)}
                    </p>
                  </div>
                  {item.resourceUrl && (
                    <p className="subscription-resource-link">
                      <a href={item.resourceUrl} target="_blank" rel="noopener noreferrer">
                        {t('subOpenResource')}
                      </a>
                    </p>
                  )}
                  <div className="actions">
                    <Link to={`/subscriptions/${item.id}/edit`}>{t('editSubscription')}</Link>
                    <button type="button" className="danger" onClick={() => setDeleteTarget(item)}>
                      {t('delete')}
                    </button>
                  </div>
                </article>
              );
            })}
            {!filteredItems.length && <p className="muted">{t('subscriptionsFilterEmpty')}</p>}
          </div>
        </>
      )}

      <ConfirmModal
        open={deleteTarget !== null}
        title={t('deleteConfirmTitle')}
        message={deleteTarget ? t('deleteConfirmBody', { title: deleteTarget.title }) : undefined}
        confirmLabel={t('delete')}
        danger
        busy={deleting}
        onConfirm={() => void confirmRemove()}
        onCancel={() => {
          if (!deleting) setDeleteTarget(null);
        }}
      />
    </section>
  );
}
