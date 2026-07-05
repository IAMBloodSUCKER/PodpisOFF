import { useI18n } from '../context/I18nContext';
import { termsByLocale } from '../i18n/terms';

export function TermsPanel() {
  const { locale, t } = useI18n();
  const terms = termsByLocale[locale];

  return (
    <details className="terms-details">
      <summary>{t('authTermsLink')}</summary>
      <div className="terms-content">
        <p className="terms-doc-title">{terms.documentTitle}</p>
        {terms.sections.map((section) => (
          <section key={section.title} className="terms-section">
            <h3>{section.title}</h3>
            <p>{section.text}</p>
          </section>
        ))}
      </div>
    </details>
  );
}
