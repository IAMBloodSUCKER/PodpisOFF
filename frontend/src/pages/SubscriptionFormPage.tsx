import { FormEvent, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/api';
import { resolveApiError } from '../api/errors';
import { OffToggle } from '../components/OffToggle';
import { useI18n } from '../context/I18nContext';

export function SubscriptionFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const { t } = useI18n();
  const [title, setTitle] = useState('');
  const [category, setCategory] = useState('');
  const [amount, setAmount] = useState('0');
  const [currency, setCurrency] = useState('USD');
  const [nextBillingDate, setNextBillingDate] = useState(new Date().toISOString().slice(0, 10));
  const [active, setActive] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!id) return;
    api
      .listSubscriptions()
      .then((items) => {
        const current = items.find((item) => String(item.id) === id);
        if (!current) return;
        setTitle(current.title);
        setCategory(current.category);
        setAmount(String(current.amount));
        setCurrency(current.currency);
        setNextBillingDate(current.nextBillingDate);
        setActive(current.active);
      })
      .catch((err) => setError(resolveApiError(err, t)));
  }, [id]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError('');
    const payload = {
      title,
      category,
      amount: Number(amount),
      currency,
      nextBillingDate,
      active,
    };
    try {
      if (isEdit && id) {
        await api.updateSubscription(Number(id), payload);
      } else {
        await api.createSubscription(payload);
      }
      navigate('/subscriptions');
    } catch (err) {
      setError(resolveApiError(err, t));
    }
  }

  return (
    <section className="stack">
      <h2>{isEdit ? t('formEditTitle') : t('formAddTitle')}</h2>
      <form className="card stack" onSubmit={onSubmit}>
        <label>{t('formTitle')}</label>
        <input value={title} onChange={(event) => setTitle(event.target.value)} required />

        <label>{t('formCategory')}</label>
        <input value={category} onChange={(event) => setCategory(event.target.value)} required />

        <label>{t('formAmount')}</label>
        <input type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} required />

        <label>{t('formCurrency')}</label>
        <input value={currency} maxLength={8} onChange={(event) => setCurrency(event.target.value.toUpperCase())} required />

        <label>{t('formNextDate')}</label>
        <input type="date" value={nextBillingDate} onChange={(event) => setNextBillingDate(event.target.value)} required />

        <div className="row-between">
          <label>{t('formStatus')}</label>
          <OffToggle active={active} onChange={setActive} />
        </div>

        {error && <p className="error">{error}</p>}

        <div className="actions">
          <button type="submit" className="primary">
            {t('formSave')}
          </button>
          <Link to="/subscriptions">{t('formCancel')}</Link>
        </div>
      </form>
    </section>
  );
}
