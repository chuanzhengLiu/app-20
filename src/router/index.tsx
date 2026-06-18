import React from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AppLayout } from '@/components/layout/AppLayout.tsx';
import { Login } from '@/pages/auth/Login.tsx';
import { Register } from '@/pages/auth/Register.tsx';
import { ForgotPassword } from '@/pages/auth/ForgotPassword.tsx';
import { Dashboard } from '@/pages/dashboard/Dashboard.tsx';
import { Programs } from '@/pages/programs/Programs.tsx';
import { ProgramDetail } from '@/pages/programs/ProgramDetail.tsx';
import { Editor } from '@/pages/editor/Editor.tsx';
import { Tasks } from '@/pages/tasks/Tasks.tsx';
import { Distribution } from '@/pages/distribution/Distribution.tsx';
import { TeamMembers } from '@/pages/team/TeamMembers.tsx';
import { TeamAudit } from '@/pages/team/TeamAudit.tsx';
import { ProfileSettings } from '@/pages/settings/ProfileSettings.tsx';
import { SecuritySettings } from '@/pages/settings/SecuritySettings.tsx';
import { ShareViewer } from '@/pages/share/ShareViewer.tsx';
import { useAuthStore } from '@/store/authStore.ts';

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { accessToken, user } = useAuthStore();
  
  if (!accessToken || !user) {
    return <Navigate to="/login" replace />;
  }
  
  return <>{children}</>;
};

const RoleProtectedRoute: React.FC<{ children: React.ReactNode; allowedRoles: string[] }> = ({ 
  children, 
  allowedRoles 
}) => {
  const { user } = useAuthStore();
  
  if (!user || !allowedRoles.includes(user.role)) {
    return <Navigate to="/dashboard" replace />;
  }
  
  return <>{children}</>;
};

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      {
        index: true,
        element: <Navigate to="/dashboard" replace />,
      },
      {
        path: 'login',
        element: <Login />,
      },
      {
        path: 'register',
        element: <Register />,
      },
      {
        path: 'forgot-password',
        element: <ForgotPassword />,
      },
      {
        path: 'dashboard',
        element: (
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        ),
      },
      {
        path: 'programs',
        element: (
          <ProtectedRoute>
            <Programs />
          </ProtectedRoute>
        ),
      },
      {
        path: 'programs/:programId',
        element: (
          <ProtectedRoute>
            <ProgramDetail />
          </ProtectedRoute>
        ),
      },
      {
        path: 'editor/:episodeId',
        element: (
          <ProtectedRoute>
            <Editor />
          </ProtectedRoute>
        ),
      },
      {
        path: 'tasks',
        element: (
          <ProtectedRoute>
            <Tasks />
          </ProtectedRoute>
        ),
      },
      {
        path: 'distribution',
        element: (
          <ProtectedRoute>
            <RoleProtectedRoute allowedRoles={['ADMIN', 'OPERATOR']}>
              <Distribution />
            </RoleProtectedRoute>
          </ProtectedRoute>
        ),
      },
      {
        path: 'team/members',
        element: (
          <ProtectedRoute>
            <RoleProtectedRoute allowedRoles={['ADMIN']}>
              <TeamMembers />
            </RoleProtectedRoute>
          </ProtectedRoute>
        ),
      },
      {
        path: 'team/audit',
        element: (
          <ProtectedRoute>
            <RoleProtectedRoute allowedRoles={['ADMIN']}>
              <TeamAudit />
            </RoleProtectedRoute>
          </ProtectedRoute>
        ),
      },
      {
        path: 'settings/profile',
        element: (
          <ProtectedRoute>
            <ProfileSettings />
          </ProtectedRoute>
        ),
      },
      {
        path: 'settings/security',
        element: (
          <ProtectedRoute>
            <SecuritySettings />
          </ProtectedRoute>
        ),
      },
      {
        path: 'share/:token',
        element: <ShareViewer />,
      },
    ],
  },
]);
