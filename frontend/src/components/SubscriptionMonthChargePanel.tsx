import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/api';
import { resolveApiError } from '../api/errors';
import { MonthPicker } from './MonthPicker';
import { useI18n } from '../context/I18nContext';
import { currency } from '../utils/format';
import { currentMonthValue, parseMonthValue } from '../utils/monthValue';

interface SubscriptionMonthChargePanelProps {
  subscriptionId: number;
  plannedAmount: number;
  currencyCode: string;
}

export function SubscriptionMonthChargePanel({
  subscriptionId,
  plannedAmount,
  currencyCode,
}: SubscriptionMonthChargePanelProps) {
  const { t, locale } = useI18n();
  const [monthValue, setMonthValue] = useState(currentMonthValue);
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [hasOverride, setHasOverride] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const parsedMonth = useMemo(() => parseMonthValue(monthValue), [monthValue]);
  const plannedLabel = currency(plannedAmount, currencyCode, locale);

  useEffect(() => {
    if (!parsedMonth) return;
    let cancelled = false;
    setLoading(true);
    setError('');
    setSuccess('');
    api
      .listSubscriptionMonthCharges(subscriptionId)
      .then((items) => {
        if (cancelled) return;
        const match = items.find(
          (item) => item.year === parsedMonth.year && item.month === parsedMonth.month,
        );
        if (match) {
          setAmount(String(match.amount));
          setNote(match.note ?? '');
          setHasOverride(true);
        } else {
          setAmount(String(plannedAmount));
          setNote('');
          setHasOverride(false);
        }
      })
      .catch((err) => {
        if (!cancelled) setError(resolveApiError(err, t));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [subscriptionId, parsedMonth, plannedAmount, t]);

  async function onSave(event: FormEvent) {
    event.preventDefault();
    if (!parsedMonth) return;
    const parsedAmount = Number(amount);
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setError(t('errorSubscriptionAmount'));
      return;
    }
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      await api.upsertSubscriptionMonthCharge(subscriptionId, {
        year: parsedMonth.year,
        month: parsedMonth.month,
        amount: parsedAmount,
        note: note.trim() || null,
      });
      setHasOverride(true);
      setSuccess(t('monthChargeSaved'));
    } catch (err) {
      setError(resolveApiError(err, t));
    } finally {
      setSaving(false);
    }
  }

  async function onReset() {
    if (!parsedMonth || !hasOverride) return;
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      await api.deleteSubscriptionMonthCharge(subscriptionId, parsedMonth.year, parsedMonth.month);
      setAmount(String(plannedAmount));
      setNote('');
      setHasOverride(false);
      setSuccess(t('monthChargeResetDone'));
    } catch (err) {
      setError(resolveApiError(err, t));
    } finally {
      setSaving(false);
    }
  }

  return (
    <article className="card stack month-charge-panel">
      <h3>{t('monthChargeTitle')}</h3>
      <p className="field-hint">{t('monthChargeHint', { amount: plannedLabel })}</p>

      <form className="stack" onSubmit={onSave}>
        <label htmlFor="month-charge-month">{t('monthChargeMonth')}</label>
        <MonthPicker
          id="month-charge-month"
          value={monthValue}
          onChange={setMonthValue}
          disabled={loading || saving}
        />
        <label htmlFor="month-charge-amount">{t('monthChargeAmount')}</label>
        <p className="field-hint">{t('monthChargeAmountHint')}</p>
        <input
          id="month-charge-amount"
          type="number"
          min="0.01"
          step="0.01"
          value={amount}
          onChange={(event) => setAmount(event.target.value)}
          disabled={loading || saving}
          required
        />

        <label htmlFor="month-charge-note">{t('monthChargeNote')}</label>
        <input
          id="month-charge-note"
          value={note}
          maxLength={500}
          placeholder={t('monthChargeNotePlaceholder')}
          onChange={(event) => setNote(event.target.value)}
          disabled={loading || saving}
        />

        {error && <p className="error">{error}</p>}
        {success && (
          <div className="month-charge-success stack">
            <p className="success">{success}</p>
            <Link to="/subscriptions" className="primary-link">
              {t('backToSubscriptions')}
            </Link>
          </div>
        )}

        <div className="actions">
          <button type="submit" className="primary" disabled={loading || saving}>
            {t('monthChargeSave')}
          </button>
          {hasOverride && (
            <button type="button" className="ghost" disabled={loading || saving} onClick={() => void onReset()}>
              {t('monthChargeReset')}
            </button>
          )}
        </div>
      </form>
    </article>
  );
}
