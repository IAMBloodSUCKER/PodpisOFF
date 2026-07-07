import { useEffect, useRef, useState } from 'react';
import { useI18n } from '../context/I18nContext';

export type ConceptGuideId = 'subscriptions' | 'reminders';

type GuideState = 'fresh' | 'seen' | 'dismissed';

function storageKey(guideId: ConceptGuideId): string {
  return `podpisoff.conceptGuide.${guideId}`;
}

function readGuideState(guideId: ConceptGuideId): GuideState {
  try {
    const value = localStorage.getItem(storageKey(guideId));
    if (value === 'dismissed') return 'dismissed';
    if (value === 'seen') return 'seen';
    return 'fresh';
  } catch {
    return 'fresh';
  }
}

function markGuideSeen(guideId: ConceptGuideId): void {
  try {
    localStorage.setItem(storageKey(guideId), 'seen');
  } catch {
    // ignore
  }
}

function markGuideDismissed(guideId: ConceptGuideId): void {
  try {
    localStorage.setItem(storageKey(guideId), 'dismissed');
  } catch {
    // ignore
  }
}

interface ConceptGuideProps {
  guideId: ConceptGuideId;
  titleKey: string;
  bodyKey: string;
  compareKey?: string;
  /** Start folded on first visit instead of opening the full hint. */
  preferCollapsed?: boolean;
}

export function ConceptGuide({ guideId, titleKey, bodyKey, compareKey, preferCollapsed = false }: ConceptGuideProps) {
  const { t } = useI18n();
  const guideStateRef = useRef(readGuideState(guideId));
  const [dismissed, setDismissed] = useState(() => guideStateRef.current === 'dismissed');
  const [open, setOpen] = useState(() => guideStateRef.current === 'fresh' && !preferCollapsed);
  const wasOpenRef = useRef(open);

  useEffect(() => {
    wasOpenRef.current = open;
  }, [open]);

  useEffect(() => {
    return () => {
      if (wasOpenRef.current) {
        markGuideSeen(guideId);
        guideStateRef.current = 'seen';
      }
    };
  }, [guideId]);

  function collapse() {
    setOpen(false);
    markGuideSeen(guideId);
    guideStateRef.current = 'seen';
  }

  function show() {
    setOpen(true);
  }

  function dismiss() {
    setDismissed(true);
    setOpen(false);
    markGuideDismissed(guideId);
    guideStateRef.current = 'dismissed';
  }

  if (dismissed) {
    return null;
  }

  if (!open) {
    return (
      <aside className="concept-guide concept-guide-collapsed" role="note">
        <div className="concept-guide-collapsed-row">
          <button type="button" className="concept-guide-toggle" onClick={show} aria-expanded={false}>
            <span className="concept-guide-toggle-label">{t('conceptGuideShow')}</span>
            <span className="concept-guide-toggle-title">{t(titleKey)}</span>
          </button>
          <button
            type="button"
            className="concept-guide-dismiss"
            onClick={dismiss}
            aria-label={t('conceptGuideDismiss')}
            title={t('conceptGuideDismiss')}
          >
            ×
          </button>
        </div>
      </aside>
    );
  }

  return (
    <aside className="concept-guide" aria-label={t(titleKey)}>
      <div className="concept-guide-header">
        <h3>{t(titleKey)}</h3>
        <div className="concept-guide-actions">
          <button type="button" className="concept-guide-toggle" onClick={collapse} aria-expanded={true}>
            {t('conceptGuideHide')}
          </button>
          <button
            type="button"
            className="concept-guide-dismiss"
            onClick={dismiss}
            aria-label={t('conceptGuideDismiss')}
            title={t('conceptGuideDismiss')}
          >
            ×
          </button>
        </div>
      </div>
      <p>{t(bodyKey)}</p>
      {compareKey && <p className="concept-guide-compare">{t(compareKey)}</p>}
    </aside>
  );
}
