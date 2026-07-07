import { FormEvent, Fragment, useEffect, useState } from 'react';
import { adminApi } from '../api/adminApi';
import { resolveApiError } from '../api/errors';
import { useI18n } from '../context/I18nContext';
import { clearAdminKey, getAdminKey, setAdminKey } from '../utils/adminKey';
import { scheduleNotificationsPolling, notifyNotificationsChanged } from '../utils/notificationEvents';
import type { AdminFeedbackItem, AdminMetrics, AdminUserRow, DailyCount, LabelCount } from '../types/api';

type Tab = 'metrics' | 'users' | 'feedback' | 'notify-test';
type UserPlanFilter = 'all' | 'pro' | 'free';

export function AdminPage() {
  const { t } = useI18n();
  const [keyInput, setKeyInput] = useState('');
  const [unlocked, setUnlocked] = useState(Boolean(getAdminKey()));
  const [tab, setTab] = useState<Tab>('metrics');
  const [error, setError] = useState('');
  const [metrics, setMetrics] = useState<AdminMetrics | null>(null);
  const [users, setUsers] = useState<AdminUserRow[]>([]);
  const [feedback, setFeedback] = useState<AdminFeedbackItem[]>([]);
  const [replyDrafts, setReplyDrafts] = useState<Record<number, string>>({});
  const [testNotifyStatus, setTestNotifyStatus] = useState('');
  const [testNotifyBusy, setTestNotifyBusy] = useState(false);
  const [notifyUserId, setNotifyUserId] = useState<number | null>(null);
  const [notifyTitle, setNotifyTitle] = useState('');
  const [notifyBody, setNotifyBody] = useState('');
  const [userActionStatus, setUserActionStatus] = useState('');
  const [userPlanFilter, setUserPlanFilter] = useState<UserPlanFilter>('all');

  async function unlock(event: FormEvent) {
    event.preventDefault();
    setError('');
    try {
      const result = await adminApi.verifyKey(keyInput.trim());
      if (!result.valid) {
        setError(t('adminKeyInvalid'));
        return;
      }
      setAdminKey(keyInput.trim());
      setUnlocked(true);
    } catch (err) {
      setError(resolveApiError(err, t));
    }
  }

  async function load() {
    setError('');
    try {
      setMetrics(await adminApi.metrics());
      if (tab === 'users') {
        setUsers(await adminApi.users(userPlanFilter));
      } else if (tab === 'feedback') {
        setFeedback(await adminApi.feedback());
      }
    } catch (err) {
      setError(resolveApiError(err, t));
      if (String(err).includes('403') || String(err).includes('Invalid admin key')) {
        clearAdminKey();
        setUnlocked(false);
      }
    }
  }

  useEffect(() => {
    if (unlocked) {
      void load();
    }
  }, [unlocked, tab, userPlanFilter]);

  async function sendTestNotification(delaySeconds: 0 | 5 | 10) {
    setError('');
    setTestNotifyStatus('');
    setTestNotifyBusy(true);
    try {
      await adminApi.sendTestNotification(delaySeconds);
      setTestNotifyStatus(
        delaySeconds === 0
          ? t('adminTestNotifyScheduled')
          : t('adminTestNotifyScheduledDelayed', { seconds: String(delaySeconds) }),
      );
      if (delaySeconds === 0) {
        notifyNotificationsChanged();
      } else {
        scheduleNotificationsPolling(delaySeconds);
      }
    } catch (err) {
      setError(resolveApiError(err, t));
    } finally {
      setTestNotifyBusy(false);
    }
  }

  async function sendReply(id: number) {
    const reply = replyDrafts[id]?.trim();
    if (!reply) return;
    try {
      await adminApi.replyToFeedback(id, reply);
      setReplyDrafts((prev) => ({ ...prev, [id]: '' }));
      await load();
    } catch (err) {
      setError(resolveApiError(err, t));
    }
  }

  function defaultNotifyTitle(): string {
    return t('adminUserNotifyDefaultTitle');
  }

  function defaultNotifyBody(): string {
    const signoff = t('adminUserNotifyBodySignoff', { brand: t('brand') });
    return `\n\n${signoff}`;
  }

  function openUserNotify(userId: number) {
    setNotifyUserId(userId);
    setNotifyTitle(defaultNotifyTitle());
    setNotifyBody(defaultNotifyBody());
  }

  function closeUserNotify() {
    setNotifyUserId(null);
    setNotifyTitle('');
    setNotifyBody('');
  }

  async function sendUserNotify(userId: number) {
    const title = notifyTitle.trim();
    if (!title) return;
    setError('');
    setUserActionStatus('');
    try {
      await adminApi.notifyUser(userId, title, notifyBody.trim());
      closeUserNotify();
      setUserActionStatus(t('adminUserActionSuccess'));
      await load();
    } catch (err) {
      setError(resolveApiError(err, t));
    }
  }

  async function blockUser(userId: number, permanent: boolean, hours?: number) {
    setError('');
    setUserActionStatus('');
    try {
      await adminApi.blockUser(userId, permanent, hours);
      setUserActionStatus(t('adminUserActionSuccess'));
      await load();
    } catch (err) {
      setError(resolveApiError(err, t));
    }
  }

  async function unblockUser(userId: number) {
    setError('');
    setUserActionStatus('');
    try {
      await adminApi.unblockUser(userId);
      setUserActionStatus(t('adminUserActionSuccess'));
      await load();
    } catch (err) {
      setError(resolveApiError(err, t));
    }
  }

  async function setUserPlan(userId: number, plan: 'FREE' | 'PRO') {
    setError('');
    setUserActionStatus('');
    try {
      await adminApi.setUserPlan(userId, plan);
      setUserActionStatus(t('adminUserActionSuccess'));
      await load();
    } catch (err) {
      setError(resolveApiError(err, t));
    }
  }

  async function deleteUser(user: AdminUserRow) {
    if (!window.confirm(t('adminUserDeleteConfirm', { username: user.username }))) return;
    setError('');
    setUserActionStatus('');
    try {
      await adminApi.deleteUser(user.id);
      if (notifyUserId === user.id) setNotifyUserId(null);
      setUserActionStatus(t('adminUserActionSuccess'));
      await load();
    } catch (err) {
      setError(resolveApiError(err, t));
    }
  }

  function userStatusLabel(user: AdminUserRow) {
    if (!user.currentlyBlocked) return t('adminUserStatusActive');
    if (user.blockedPermanently) return t('adminUserStatusBlockedPermanent');
    if (user.blockedUntil) {
      return t('adminUserStatusBlockedUntil', {
        date: new Date(user.blockedUntil).toLocaleString(),
      });
    }
    return t('adminUserStatusBlockedPermanent');
  }

  const pendingFeedback = metrics?.pendingFeedback ?? 0;

  if (!unlocked) {
    return (
      <section className="stack admin-page">
        <article className="card admin-key-card">
          <h2>{t('adminTitle')}</h2>
          <p className="muted">{t('adminKeyHint')}</p>
          <form className="stack" onSubmit={(event) => void unlock(event)}>
            <input
              type="password"
              value={keyInput}
              onChange={(event) => setKeyInput(event.target.value)}
              placeholder={t('adminKeyPlaceholder')}
              autoComplete="new-password"
            />
            {error && <p className="error">{error}</p>}
            <button type="submit" className="primary">{t('adminUnlock')}</button>
          </form>
        </article>
      </section>
    );
  }

  return (
    <section className="admin-page">
      <header className="admin-header">
        <div>
          <h2>{t('adminTitle')}</h2>
          <p className="muted">{t('adminSubtitle')}</p>
        </div>
        <button
          type="button"
          className="ghost"
          onClick={() => {
            clearAdminKey();
            setUnlocked(false);
          }}
        >
          {t('adminLock')}
        </button>
      </header>

      <div className="admin-layout">
        <nav className="admin-nav" aria-label={t('adminTitle')}>
          <button type="button" className={tab === 'metrics' ? 'active' : ''} onClick={() => setTab('metrics')}>
            {t('adminNavOverview')}
          </button>
          <button type="button" className={tab === 'users' ? 'active' : ''} onClick={() => setTab('users')}>
            {t('adminTabUsers')}
          </button>
          <button type="button" className={tab === 'feedback' ? 'active' : ''} onClick={() => setTab('feedback')}>
            <span>{t('adminTabFeedback')}</span>
            {pendingFeedback > 0 && <span className="admin-nav-badge">{pendingFeedback}</span>}
          </button>
          <button type="button" className={tab === 'notify-test' ? 'active' : ''} onClick={() => setTab('notify-test')}>
            {t('adminTabNotifyTest')}
          </button>
        </nav>

        <div className="admin-content stack">
          {error && <p className="error">{error}</p>}

          {tab === 'notify-test' && (
            <article className="card stack admin-test-notify">
              <h3>{t('adminTestNotifyTitle')}</h3>
              <p className="muted">{t('adminTestNotifyHint')}</p>
              <div className="admin-test-notify-actions">
                <button type="button" className="primary" disabled={testNotifyBusy} onClick={() => void sendTestNotification(0)}>
                  {testNotifyBusy ? t('adminTestNotifySending') : t('adminTestNotifyNow')}
                </button>
                <button type="button" className="ghost" disabled={testNotifyBusy} onClick={() => void sendTestNotification(5)}>
                  {t('adminTestNotify5s')}
                </button>
                <button type="button" className="ghost" disabled={testNotifyBusy} onClick={() => void sendTestNotification(10)}>
                  {t('adminTestNotify10s')}
                </button>
              </div>
              {testNotifyStatus && <p className="success">{testNotifyStatus}</p>}
            </article>
          )}

          {tab === 'metrics' && metrics && (
            <>
              <div className="admin-kpi-row">
                <KpiCard label={t('adminMetricUsers')} value={metrics.totalUsers} />
                <KpiCard label={t('adminMetricEffectivePro')} value={metrics.effectiveProUsers} highlight="pro" />
                <KpiCard label={t('adminMetricActiveSubs')} value={metrics.activeSubscriptions} />
                <KpiCard
                  label={t('adminMetricFeedbackPending')}
                  value={metrics.pendingFeedback}
                  highlight={metrics.pendingFeedback > 0 ? 'warn' : undefined}
                />
              </div>

              <div className="admin-groups-grid">
                <MetricGroup
                  title={t('adminSectionUsers')}
                  items={[
                    { label: t('adminMetricNewToday'), value: metrics.newUsersToday },
                    { label: t('adminMetricNewWeek'), value: metrics.newUsersWeek },
                    { label: t('adminMetricNewMonth'), value: metrics.newUsersMonth },
                    { label: t('adminMetricEffectiveFree'), value: metrics.effectiveFreeUsers },
                    { label: t('adminMetricExpiredPro'), value: metrics.expiredProUsers },
                    { label: t('adminMetricBlockedUsers'), value: metrics.blockedUsers },
                    { label: t('adminMetricAvgSubs'), value: metrics.avgSubscriptionsPerUser.toFixed(1) },
                  ]}
                />
                <MetricGroup
                  title={t('adminSectionActivity')}
                  items={[
                    { label: t('adminMetricLoginsToday'), value: metrics.loginsToday },
                    { label: t('adminMetricUniqueToday'), value: metrics.uniqueLoginsToday },
                    { label: t('adminMetricLoginsWeek'), value: metrics.loginsWeek },
                    { label: t('adminMetricUniqueWeek'), value: metrics.uniqueLoginsWeek },
                    { label: t('adminMetricLoginsMonth'), value: metrics.loginsMonth },
                    { label: t('adminMetricUniqueMonth'), value: metrics.uniqueLoginsMonth },
                  ]}
                />
                <MetricGroup
                  title={t('adminSectionContent')}
                  items={[
                    { label: t('adminMetricSubs'), value: metrics.totalSubscriptions },
                    { label: t('adminMetricActiveSubs'), value: metrics.activeSubscriptions },
                    { label: t('adminMetricPausedSubs'), value: metrics.pausedSubscriptions },
                    { label: t('adminMetricOffSubs'), value: metrics.offSubscriptions },
                    { label: t('adminMetricReminders'), value: metrics.totalReminders },
                    { label: t('adminMetricRecurringReminders'), value: metrics.recurringReminders },
                  ]}
                />
                <MetricGroup
                  title={t('adminSectionSupport')}
                  items={[
                    { label: t('adminMetricFeedbackPending'), value: metrics.pendingFeedback, accent: metrics.pendingFeedback > 0 },
                    { label: t('adminMetricFeedbackTotal'), value: metrics.totalFeedback },
                  ]}
                />
                <MetricGroup
                  title={t('adminSectionChannels')}
                  items={[
                    { label: t('adminMetricPushSubs'), value: metrics.pushSubscribers },
                    { label: t('adminMetricEmailNotify'), value: metrics.usersWithEmailNotify },
                    { label: t('adminMetricTelegramNotify'), value: metrics.usersWithTelegramNotify },
                  ]}
                />
              </div>

              <section className="admin-charts-section">
                <h3 className="admin-section-title">{t('adminSectionTrends')}</h3>
                <div className="admin-charts-grid">
                  <TrendCard title={t('adminTrendRegistrations')} items={metrics.registrationsLast7Days} />
                  <TrendCard title={t('adminTrendLogins')} items={metrics.loginsLast7Days} />
                  <TrendCard title={t('adminTrendUniqueLogins')} items={metrics.uniqueLoginsLast7Days} />
                  <TrendCard title={t('adminTrendNewSubscriptions')} items={metrics.subscriptionsCreatedLast7Days} />
                  <TrendCard title={t('adminTrendFeedback')} items={metrics.feedbackLast7Days} />
                </div>
              </section>

              <section className="admin-charts-section">
                <h3 className="admin-section-title">{t('adminSectionSlices')}</h3>
                <div className="admin-charts-grid">
                  <SliceCard title={t('adminChartEffectivePlan')} items={metrics.effectivePlanSlice} labelFor={(label) => sliceLabel(label, t)} />
                  <SliceCard title={t('adminChartSubscriptionStatus')} items={metrics.subscriptionStatusSlice} labelFor={(label) => sliceLabel(label, t)} />
                  <SliceCard title={t('adminChartBillingPeriod')} items={metrics.billingPeriodSlice} labelFor={(label) => sliceLabel(label, t)} />
                  <SliceCard title={t('adminChartTopCategories')} items={metrics.topCategories} labelFor={(label) => label} />
                  <SliceCard title={t('adminChartCurrencies')} items={metrics.currencySlice} labelFor={(label) => label} />
                  <SliceCard title={t('adminChartLocale')} items={metrics.localeSlice} labelFor={(label) => sliceLabel(label, t)} />
                  <SliceCard title={t('adminChartUsersBySubs')} items={metrics.usersBySubscriptionCountSlice} labelFor={(label) => sliceLabel(label, t)} />
                  <SliceCard title={t('adminChartBlockedUsers')} items={metrics.blockedUsersSlice} labelFor={(label) => sliceLabel(label, t)} />
                  <SliceCard title={t('adminChartNotifyChannels')} items={metrics.notificationChannelsSlice} labelFor={(label) => sliceLabel(label, t)} />
                  <SliceCard title={t('adminChartReminderRepeat')} items={metrics.reminderRepeatSlice} labelFor={(label) => sliceLabel(label, t)} />
                </div>
              </section>
            </>
          )}

          {tab === 'metrics' && !metrics && !error && <p className="muted">{t('loading')}</p>}

          {tab === 'users' && (
        <article className="card admin-table-card stack">
          {userActionStatus && <p className="success">{userActionStatus}</p>}
          <div className="filters admin-user-plan-filters">
            <button type="button" className={userPlanFilter === 'all' ? 'active' : 'ghost'} onClick={() => setUserPlanFilter('all')}>
              {t('adminUserFilterAll')}
            </button>
            <button type="button" className={userPlanFilter === 'pro' ? 'active' : 'ghost'} onClick={() => setUserPlanFilter('pro')}>
              {t('adminUserFilterPro')}
            </button>
            <button type="button" className={userPlanFilter === 'free' ? 'active' : 'ghost'} onClick={() => setUserPlanFilter('free')}>
              {t('adminUserFilterFree')}
            </button>
          </div>
          <table className="admin-table">
            <thead>
              <tr>
                <th>{t('adminColUser')}</th>
                <th>{t('adminColPlan')}</th>
                <th>{t('adminColStatus')}</th>
                <th>{t('adminColSubs')}</th>
                <th>{t('adminColReminders')}</th>
                <th>{t('adminColCreated')}</th>
                <th>{t('adminColActions')}</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <Fragment key={user.id}>
                  <tr className={user.currentlyBlocked ? 'admin-user-blocked' : undefined}>
                    <td>{user.username}</td>
                    <td>
                      <span className={`badge ${user.effectivePlan === 'PRO' ? 'active' : 'off'}`}>
                        {user.effectivePlan}
                      </span>
                      {user.plan !== user.effectivePlan && (
                        <span className="muted admin-plan-raw"> ({user.plan})</span>
                      )}
                      {user.planExpiresAt && (
                        <div className="muted admin-plan-expires">
                          {t('adminPlanUntil', { date: new Date(user.planExpiresAt).toLocaleDateString() })}
                        </div>
                      )}
                    </td>
                    <td>
                      <span className={user.currentlyBlocked ? 'admin-user-status-blocked' : 'admin-user-status-active'}>
                        {userStatusLabel(user)}
                      </span>
                    </td>
                    <td>{user.subscriptionCount}</td>
                    <td>{user.reminderCount}</td>
                    <td>{new Date(user.createdAt).toLocaleDateString()}</td>
                    <td>
                      <details className="admin-action-menu">
                        <summary className="ghost compact">{t('adminUserActions')}</summary>
                        <div className="admin-action-menu-panel">
                          <button
                            type="button"
                            className="ghost compact"
                            onClick={() => {
                              if (notifyUserId === user.id) {
                                closeUserNotify();
                              } else {
                                openUserNotify(user.id);
                              }
                            }}
                          >
                            {t('adminUserNotify')}
                          </button>
                          {user.effectivePlan === 'PRO' ? (
                            <button type="button" className="ghost compact" onClick={() => void setUserPlan(user.id, 'FREE')}>
                              {t('adminUserRevokePro')}
                            </button>
                          ) : (
                            <button type="button" className="ghost compact" onClick={() => void setUserPlan(user.id, 'PRO')}>
                              {t('adminUserGrantPro')}
                            </button>
                          )}
                          {user.currentlyBlocked ? (
                            <button type="button" className="ghost compact" onClick={() => void unblockUser(user.id)}>
                              {t('adminUserUnblock')}
                            </button>
                          ) : (
                            <>
                              <button type="button" className="ghost compact" onClick={() => void blockUser(user.id, true)}>
                                {t('adminUserBlockPermanent')}
                              </button>
                              <button type="button" className="ghost compact" onClick={() => void blockUser(user.id, false, 24)}>
                                {t('adminUserBlock1d')}
                              </button>
                              <button type="button" className="ghost compact" onClick={() => void blockUser(user.id, false, 168)}>
                                {t('adminUserBlock7d')}
                              </button>
                              <button type="button" className="ghost compact" onClick={() => void blockUser(user.id, false, 720)}>
                                {t('adminUserBlock30d')}
                              </button>
                            </>
                          )}
                          <button type="button" className="ghost compact admin-user-delete" onClick={() => void deleteUser(user)}>
                            {t('adminUserDelete')}
                          </button>
                        </div>
                      </details>
                    </td>
                  </tr>
                  {notifyUserId === user.id && (
                    <tr className="admin-user-notify-row">
                      <td colSpan={7}>
                        <div className="stack admin-user-notify-form">
                          <strong>{t('adminUserNotifyTitle')}: {user.username}</strong>
                          <input
                            value={notifyTitle}
                            onChange={(event) => setNotifyTitle(event.target.value)}
                            placeholder={t('adminUserNotifyTitlePlaceholder')}
                          />
                          <textarea
                            rows={3}
                            value={notifyBody}
                            onChange={(event) => setNotifyBody(event.target.value)}
                            placeholder={t('adminUserNotifyBodyPlaceholder')}
                          />
                          <div className="admin-user-actions">
                            <button type="button" className="primary compact" onClick={() => void sendUserNotify(user.id)}>
                              {t('adminUserNotifySend')}
                            </button>
                            <button type="button" className="ghost compact" onClick={closeUserNotify}>
                              {t('cancel')}
                            </button>
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </article>
      )}

      {tab === 'feedback' && (
        <div className="stack">
          {feedback.map((item) => (
            <article className={`card admin-feedback-card ${item.adminReply ? '' : 'admin-feedback-pending'}`} key={item.id}>
              <div className="row-between admin-feedback-head">
                <div>
                  <strong>{item.username}</strong>
                  {!item.adminReply && <span className="admin-feedback-badge">{t('adminFeedbackPendingBadge')}</span>}
                  <span className={`admin-feedback-kind ${item.kind === 'SUPPORT' ? 'support' : 'feedback'}`}>
                    {item.kind === 'SUPPORT' ? t('adminFeedbackKindSupport') : t('adminFeedbackKindFeedback')}
                  </span>
                </div>
                <span className="muted">{new Date(item.createdAt).toLocaleString()}</span>
              </div>
              <p>{item.message}</p>
              {item.adminReply ? (
                <div className="admin-reply-box">
                  <strong>{t('adminReplyLabel')}</strong>
                  <p>{item.adminReply}</p>
                </div>
              ) : (
                <div className="stack">
                  <textarea
                    rows={3}
                    value={replyDrafts[item.id] ?? ''}
                    onChange={(event) => setReplyDrafts((prev) => ({ ...prev, [item.id]: event.target.value }))}
                    placeholder={t('adminReplyPlaceholder')}
                  />
                  <button type="button" className="primary" onClick={() => void sendReply(item.id)}>
                    {t('adminReplySend')}
                  </button>
                </div>
              )}
            </article>
          ))}
          {!feedback.length && <p className="muted">{t('adminFeedbackEmpty')}</p>}
        </div>
      )}
        </div>
      </div>
    </section>
  );
}

function sliceLabel(label: string, t: (key: string) => string): string {
  const key = `adminSlice_${label}`;
  const translated = t(key);
  return translated === key ? label : translated;
}

function KpiCard({
  label,
  value,
  highlight,
}: {
  label: string;
  value: string | number;
  highlight?: 'pro' | 'warn';
}) {
  return (
    <article className={`card admin-kpi-card ${highlight ? `admin-kpi-${highlight}` : ''}`}>
      <p className="admin-kpi-label">{label}</p>
      <p className="admin-kpi-value">{value}</p>
    </article>
  );
}

type MetricItem = { label: string; value: string | number; accent?: boolean };

function MetricGroup({ title, items }: { title: string; items: MetricItem[] }) {
  return (
    <article className="card admin-metric-group">
      <h3>{title}</h3>
      <dl className="admin-metric-list">
        {items.map((item) => (
          <div className={item.accent ? 'admin-metric-list-accent' : undefined} key={item.label}>
            <dt>{item.label}</dt>
            <dd>{item.value}</dd>
          </div>
        ))}
      </dl>
    </article>
  );
}

function TrendCard({ title, items }: { title: string; items: DailyCount[] }) {
  const max = Math.max(...items.map((item) => item.count), 1);
  return (
    <article className="card admin-chart-card">
      <h3>{title}</h3>
      <div className="admin-trend-bars">
        {items.map((item) => (
          <div className="admin-trend-row" key={item.date}>
            <span className="muted">{item.date.slice(5)}</span>
            <div className="admin-trend-track">
              <div className="admin-trend-fill admin-trend-fill-primary" style={{ width: `${(item.count / max) * 100}%` }} />
            </div>
            <strong>{item.count}</strong>
          </div>
        ))}
      </div>
    </article>
  );
}

function SliceCard({
  title,
  items,
  labelFor,
}: {
  title: string;
  items: LabelCount[];
  labelFor: (label: string) => string;
}) {
  const max = Math.max(...items.map((item) => item.count), 1);
  const total = items.reduce((sum, item) => sum + item.count, 0);
  return (
    <article className="card admin-chart-card">
      <h3>{title}</h3>
      <div className="admin-trend-bars">
        {items.map((item) => (
          <div className="admin-trend-row" key={item.label}>
            <span className="admin-slice-label" title={labelFor(item.label)}>
              {labelFor(item.label)}
            </span>
            <div className="admin-trend-track">
              <div className="admin-trend-fill admin-trend-fill-slice" style={{ width: `${(item.count / max) * 100}%` }} />
            </div>
            <strong>
              {item.count}
              {total > 0 && <span className="muted admin-slice-pct"> {Math.round((item.count / total) * 100)}%</span>}
            </strong>
          </div>
        ))}
      </div>
    </article>
  );
}
