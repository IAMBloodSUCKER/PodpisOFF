import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/api';
import { ApiClientError, mapApiFieldErrorsToForm, resolveApiError } from '../api/errors';
import { CategoryPicker } from '../components/CategoryPicker';
import { CurrencyPicker } from '../components/CurrencyPicker';
import { DateTimePicker } from '../components/DateTimePicker';
import { SubscriptionMonthChargePanel } from '../components/SubscriptionMonthChargePanel';
import { PageHeader } from '../components/PageHeader';
import { OffToggle } from '../components/OffToggle';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/I18nContext';
import { BillingPeriod } from '../types/api';
import {
  normalizeResourceUrl,
  SubscriptionFormField,
  todayLocalIso,
  validateSubscriptionFormFields,
} from '../utils/format';
import { canUseForeignCurrency, effectivePlan } from '../utils/plan';
import { resolveSubscriptionStatus } from '../utils/subscriptionStatus';
import {
  clearSubscriptionDraft,
  readSubscriptionDraft,
  writeSubscriptionDraft,
} from '../utils/subscriptionDraft';

function createEmptyDraft(): {
  title: string;
  category: string;
  amount: string;
  currency: string;
  billingPeriod: BillingPeriod;
  nextBillingDate: string;
  note: string;
  resourceUrl: string;
  active: boolean;
} {
  const saved = readSubscriptionDraft();
  if (saved) return saved;
  return {
    title: '',
    category: '',
    amount: '',
    currency: 'RUB',
    billingPeriod: 'MONTHLY',
    nextBillingDate: todayLocalIso(),
    note: '',
    resourceUrl: '',
    active: true,
  };
}

function fieldWrapClass(fieldErrors: Partial<Record<SubscriptionFormField, string>>, field: SubscriptionFormField) {
  return `field-wrap${fieldErrors[field] ? ' field-invalid' : ''}`;
}

export function SubscriptionFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const { t } = useI18n();
  const { user } = useAuth();
  const plan = effectivePlan(user);
  const foreignCurrencyAllowed = canUseForeignCurrency(plan);
  const initialDraft = useMemo(() => (isEdit ? null : createEmptyDraft()), [isEdit]);
  const [title, setTitle] = useState(initialDraft?.title ?? '');
  const [category, setCategory] = useState(initialDraft?.category ?? '');
  const [amount, setAmount] = useState(initialDraft?.amount ?? '');
  const [currency, setCurrency] = useState(initialDraft?.currency ?? 'RUB');
  const [billingPeriod, setBillingPeriod] = useState<BillingPeriod>(initialDraft?.billingPeriod ?? 'MONTHLY');
  const [nextBillingDate, setNextBillingDate] = useState(initialDraft?.nextBillingDate ?? todayLocalIso());
  const [note, setNote] = useState(initialDraft?.note ?? '');
  const [resourceUrl, setResourceUrl] = useState(initialDraft?.resourceUrl ?? '');
  const [active, setActive] = useState(initialDraft?.active ?? true);
  const [loading, setLoading] = useState(isEdit);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<SubscriptionFormField, string>>>({});
  const [submitted, setSubmitted] = useState(false);

  const clearFieldError = useCallback((field: SubscriptionFormField) => {
    setFieldErrors((prev) => {
      if (!prev[field]) return prev;
      const next = { ...prev };
      delete next[field];
      return next;
    });
  }, []);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setNotFound(false);
    api
      .listSubscriptions()
      .then((items) => {
        const current = items.find((item) => String(item.id) === id);
        if (!current) {
          setNotFound(true);
          return;
        }
        setTitle(current.title);
        setCategory(current.category);
        setAmount(String(current.amount));
        setCurrency(current.currency);
        setBillingPeriod(current.billingPeriod ?? 'MONTHLY');
        setNextBillingDate(current.nextBillingDate);
        setNote(current.note ?? '');
        setResourceUrl(current.resourceUrl ?? '');
        setActive(current.active);
      })
      .catch((err) => setError(resolveApiError(err, t)))
      .finally(() => setLoading(false));
  }, [id, t]);

  const predictedStatus = useMemo(
    () => resolveSubscriptionStatus(active, nextBillingDate),
    [active, nextBillingDate],
  );

  const activeStatusHint = useMemo(() => {
    if (active) return t('formActiveHintOn');
    return predictedStatus === 'paused' ? t('formActiveHintPaused') : t('formActiveHintOffStatus');
  }, [active, predictedStatus, t]);

  const statusLabel = useMemo(() => {
    if (predictedStatus === 'active') return t('statusActive');
    if (predictedStatus === 'paused') return t('statusPaused');
    return t('statusOff');
  }, [predictedStatus, t]);

  useEffect(() => {
    if (!foreignCurrencyAllowed && !isEdit && currency !== 'RUB') {
      setCurrency('RUB');
    }
  }, [foreignCurrencyAllowed, isEdit, currency]);

  useEffect(() => {
    if (isEdit) return;
    writeSubscriptionDraft({
      title,
      category,
      amount,
      currency,
      billingPeriod,
      nextBillingDate,
      note,
      resourceUrl,
      active,
    });
  }, [isEdit, title, category, amount, currency, billingPeriod, nextBillingDate, note, resourceUrl, active]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError('');

    const validationErrors = validateSubscriptionFormFields({
      title,
      category,
      amount,
      currency,
      note,
      resourceUrl,
    });
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      setSubmitted(true);
      return;
    }

    setFieldErrors({});
    const payload = {
      title: title.trim(),
      category: category.trim(),
      amount: Number(amount),
      currency: currency.trim().toUpperCase(),
      nextBillingDate,
      billingPeriod,
      active,
      note: note.trim() || null,
      resourceUrl: normalizeResourceUrl(resourceUrl),
    };
    try {
      if (isEdit && id) {
        await api.updateSubscription(Number(id), payload);
      } else {
        await api.createSubscription(payload);
        clearSubscriptionDraft();
      }
      navigate('/subscriptions');
    } catch (err) {
      if (err instanceof ApiClientError && err.fieldErrors) {
        setFieldErrors(mapApiFieldErrorsToForm(err.fieldErrors));
        setSubmitted(true);
        setError('');
        return;
      }
      setError(resolveApiError(err, t));
    }
  }

  const hasFieldErrors = submitted && Object.keys(fieldErrors).length > 0;

  if (loading) return <p className="muted">{t('loading')}</p>;
  if (notFound) return <p className="error">{t('errorSubscriptionNotFound')}</p>;

  return (
    <section className="stack">
      <PageHeader
        title={isEdit ? t('formEditTitle') : t('formAddTitle')}
        subtitle={isEdit ? undefined : t('formAddSubtitle')}
        backTo="/subscriptions"
        backLabel={t('backToSubscriptions')}
      />
      <form className="card stack" onSubmit={onSubmit}>
        <div className={fieldWrapClass(fieldErrors, 'title')}>
          <label>{t('formTitle')}</label>
          <input
            value={title}
            maxLength={120}
            onChange={(event) => {
              setTitle(event.target.value);
              clearFieldError('title');
            }}
            required
          />
          {fieldErrors.title && <p className="field-error">{t(fieldErrors.title)}</p>}
        </div>

        <div className={fieldWrapClass(fieldErrors, 'category')}>
          <label>{t('formCategory')}</label>
          <CategoryPicker
            value={category}
            onChange={(value) => {
              setCategory(value);
              clearFieldError('category');
            }}
          />
          {fieldErrors.category && <p className="field-error">{t(fieldErrors.category)}</p>}
        </div>

        <label>{t('formBillingPeriod')}</label>
        <select value={billingPeriod} onChange={(event) => setBillingPeriod(event.target.value as BillingPeriod)}>
          <option value="MONTHLY">{t('billingPeriodMonthly')}</option>
          <option value="YEARLY">{t('billingPeriodYearly')}</option>
        </select>
        <p className="field-hint">{t('formBillingPeriodHint')}</p>

        <div className={fieldWrapClass(fieldErrors, 'amount')}>
          <label>{t('formAmount')}</label>
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={amount}
            onChange={(event) => {
              setAmount(event.target.value);
              clearFieldError('amount');
            }}
            required
          />
          <p className="field-hint">{t('formAmountHint')}</p>
          {fieldErrors.amount && <p className="field-error">{t(fieldErrors.amount)}</p>}
        </div>

        <div className={fieldWrapClass(fieldErrors, 'currency')}>
          <label>{t('formCurrency')}</label>
          <CurrencyPicker
            value={currency}
            onChange={(value) => {
              setCurrency(value);
              clearFieldError('currency');
            }}
            foreignCurrencyAllowed={foreignCurrencyAllowed}
            isEdit={isEdit}
          />
          {!foreignCurrencyAllowed && <p className="field-hint">{t('formCurrencyFreeHint')}</p>}
          {fieldErrors.currency && <p className="field-error">{t(fieldErrors.currency)}</p>}
        </div>

        <label>{t('formNextDate')}</label>
        <DateTimePicker value={nextBillingDate} onChange={setNextBillingDate} includeTime={false} />
        <p className="field-hint">{t('formNextDateHint')}</p>

        <div className={fieldWrapClass(fieldErrors, 'note')}>
          <label>{t('formNote')}</label>
          <textarea
            value={note}
            maxLength={500}
            rows={3}
            placeholder={t('formNotePlaceholder')}
            onChange={(event) => {
              setNote(event.target.value);
              clearFieldError('note');
            }}
          />
          <p className="field-hint">{t('formNoteHint')}</p>
          {fieldErrors.note && <p className="field-error">{t(fieldErrors.note)}</p>}
        </div>

        <div className={fieldWrapClass(fieldErrors, 'resourceUrl')}>
          <label>{t('formResourceUrl')}</label>
          <input
            type="url"
            value={resourceUrl}
            maxLength={500}
            placeholder={t('formResourceUrlPlaceholder')}
            onChange={(event) => {
              setResourceUrl(event.target.value);
              clearFieldError('resourceUrl');
            }}
          />
          <p className="field-hint">{t('formResourceUrlHint')}</p>
          {fieldErrors.resourceUrl && <p className="field-error">{t(fieldErrors.resourceUrl)}</p>}
        </div>

        <div className="form-status-block">
          <div className="row-between">
            <div>
              <label>{t('formActiveLabel')}</label>
              <div className="form-status-preview">
                <span className={`badge ${predictedStatus}`}>{statusLabel}</span>
                <p className="field-hint">{activeStatusHint}</p>
              </div>
            </div>
            <OffToggle active={active} onChange={setActive} />
          </div>
        </div>

        {error && !hasFieldErrors && <p className="error">{error}</p>}

        <div className="actions">
          <button type="submit" className="primary">
            {t('formSave')}
          </button>
          <Link to="/subscriptions">{t('backToSubscriptions')}</Link>
        </div>
      </form>

      {isEdit && id && amount && (
        <>
          <SubscriptionMonthChargePanel
            subscriptionId={Number(id)}
            plannedAmount={Number(amount)}
            currencyCode={currency}
          />
          <p className="form-bottom-back">
            <Link to="/subscriptions" className="primary-link">
              {t('backToSubscriptions')}
            </Link>
          </p>
        </>
      )}
    </section>
  );
}
