import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import MainLayout from './layouts/MainLayout';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import HistoryPage from './pages/HistoryPage';
import NotFoundPage from './pages/NotFoundPage';
import ProtectedRoute from './components/common/ProtectedRoute';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route
            path="/"
            element={
              <MainLayout>
                <HomePage />
              </MainLayout>
            }
          />
          <Route
            path="/login"
            element={
              <MainLayout>
                <LoginPage />
              </MainLayout>
            }
          />
          <Route
            path="/register"
            element={
              <MainLayout>
                <RegisterPage />
              </MainLayout>
            }
          />
          <Route
            path="/history"
            element={
              <MainLayout>
                <ProtectedRoute>
                  <HistoryPage />
                </ProtectedRoute>
              </MainLayout>
            }
          />
          <Route
            path="*"
            element={
              <MainLayout>
                <NotFoundPage />
              </MainLayout>
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
