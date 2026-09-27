import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter, MemoryRouter, Routes, Route } from 'react-router-dom';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import ProtectedRoute from '../components/common/ProtectedRoute';
import { AuthProvider } from '../context/AuthContext';

describe('Authentication Components', () => {
  it('renders LoginPage with email and password inputs and handles validation', async () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </BrowserRouter>
    );

    expect(screen.getByRole('heading', { name: /Welcome back/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/Email Address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign In/i })).toBeInTheDocument();

    const submitBtn = screen.getByRole('button', { name: /Sign In/i });
    fireEvent.click(submitBtn);

    // HTML5 validation or state prevents empty submit
    expect(screen.getByLabelText(/Email Address/i)).toBeRequired();
  });

  it('renders RegisterPage and detects password mismatch', async () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <RegisterPage />
        </AuthProvider>
      </BrowserRouter>
    );

    expect(screen.getByRole('heading', { name: /Create an Account/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/Full Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Email Address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Confirm Password/i)).toBeInTheDocument();

    // Fill in mismatched passwords
    fireEvent.change(screen.getByLabelText(/Full Name/i), { target: { value: 'Alice Smith' } });
    fireEvent.change(screen.getByLabelText(/Email Address/i), { target: { value: 'alice@example.com' } });
    fireEvent.change(screen.getByLabelText(/^Password/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/Confirm Password/i), { target: { value: 'differentpassword' } });

    fireEvent.click(screen.getByRole('button', { name: /Create Account/i }));

    const error = await screen.findByRole('alert');
    expect(error).toHaveTextContent(/Passwords do not match/i);
  });

  it('ProtectedRoute redirects unauthenticated users to /login', () => {
    render(
      <MemoryRouter initialEntries={['/protected']}>
        <AuthProvider>
          <Routes>
            <Route
              path="/protected"
              element={
                <ProtectedRoute>
                  <div>Secret Studio Dashboard</div>
                </ProtectedRoute>
              }
            />
            <Route path="/login" element={<div>Login Page Redirect Target</div>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByText('Login Page Redirect Target')).toBeInTheDocument();
    expect(screen.queryByText('Secret Studio Dashboard')).not.toBeInTheDocument();
  });
});
