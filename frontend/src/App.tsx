import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { ProtectedRoute } from './components/ProtectedRoute';
import { useAuth } from './context/AuthContext';
import { AuthPage } from './pages/AuthPage';
import { DashboardPage } from './pages/DashboardPage';
import { SettingsPage } from './pages/SettingsPage';
import { SubscriptionFormPage } from './pages/SubscriptionFormPage';
import { SubscriptionsPage } from './pages/SubscriptionsPage';

function HomeRedirect() {
  const { isAuthenticated } = useAuth();
  return <Navigate to={isAuthenticated ? '/dashboard' : '/auth'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/auth" element={<AuthPage />} />
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
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
