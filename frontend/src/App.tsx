import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { ProtectedRoute } from './components/ProtectedRoute';
import { ReminderAlertsProvider } from './context/ReminderAlertsContext';
import { NotificationSoundUnlock } from './components/NotificationSoundUnlock';
import { useAuth } from './context/AuthContext';
import { AuthPage } from './pages/AuthPage';
import { OAuthCallbackPage } from './pages/OAuthCallbackPage';
import { DashboardPage } from './pages/DashboardPage';
import { SettingsPage } from './pages/SettingsPage';
import { HelpPage } from './pages/HelpPage';
import { AdminPage } from './pages/AdminPage';
import { SubscriptionFormPage } from './pages/SubscriptionFormPage';
import { SubscriptionsPage } from './pages/SubscriptionsPage';

function HomeRedirect() {
  const { isAuthenticated } = useAuth();
  return <Navigate to={isAuthenticated ? '/dashboard' : '/auth'} replace />;
}

export default function App() {
  return (
    <ReminderAlertsProvider>
      <NotificationSoundUnlock />
      <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/auth" element={<AuthPage />} />
      <Route path="/auth/oauth/callback" element={<OAuthCallbackPage />} />
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <AppShell>
              <DashboardPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/subscriptions"
        element={
          <ProtectedRoute>
            <AppShell>
              <SubscriptionsPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/subscriptions/new"
        element={
          <ProtectedRoute>
            <AppShell>
              <SubscriptionFormPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/subscriptions/:id/edit"
        element={
          <ProtectedRoute>
            <AppShell>
              <SubscriptionFormPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route path="/reminders" element={<Navigate to="/dashboard" replace />} />
      <Route
        path="/help"
        element={
          <ProtectedRoute>
            <AppShell>
              <HelpPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/settings"
        element={
          <ProtectedRoute>
            <AppShell>
              <SettingsPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin"
        element={
          <ProtectedRoute>
            <AppShell>
              <AdminPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </ReminderAlertsProvider>
  );
}
