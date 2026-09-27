import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import HistoryCard from '../components/history/HistoryCard';
import HistoryList from '../components/history/HistoryList';
import FavoriteButton from '../components/history/FavoriteButton';
import MainLayout from '../layouts/MainLayout';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';

describe('History and Responsive Layout Components', () => {
  const mockItem = {
    id: 101,
    text: 'This is a long sample speech synthesis text designed to test rendering, word count, character count, and expansion toggles on the history card component.',
    language: 'en-US',
    voice: 'en-female-1',
    speed: 1.0,
    pitch: 0,
    volume: 1.0,
    style: 'default',
    favorite: false,
    characterCount: 153,
    createdAt: new Date().toISOString()
  };

  it('renders HistoryCard with metadata tags, text preview, and controls', () => {
    render(<HistoryCard item={mockItem} onDelete={() => {}} onToggleFavorite={() => {}} />);

    expect(screen.getByText(/#101/i)).toBeInTheDocument();
    expect(screen.getByText('en-US')).toBeInTheDocument();
    expect(screen.getByText('en-female-1')).toBeInTheDocument();
    expect(screen.getByText(/153 chars/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Play speech audio/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Download audio MP3 file/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Delete history record/i })).toBeInTheDocument();
  });

  it('toggles favorite on FavoriteButton click', () => {
    const handleToggle = vi.fn();
    render(<FavoriteButton isFavorite={false} onToggle={handleToggle} />);

    const btn = screen.getByRole('button', { name: /Add to favorites/i });
    fireEvent.click(btn);
    expect(handleToggle).toHaveBeenCalledTimes(1);
  });

  it('renders HistoryList empty state with Reset Filters button', () => {
    const handleClear = vi.fn();
    render(
      <HistoryList
        items={[]}
        emptyMessage="No speech records match your active search or filter criteria."
        onClearFilters={handleClear}
      />
    );

    expect(screen.getByText(/No speech records match your active search/i)).toBeInTheDocument();
    const resetBtn = screen.getByRole('button', { name: /Reset All Filters/i });
    expect(resetBtn).toBeInTheDocument();
    fireEvent.click(resetBtn);
    expect(handleClear).toHaveBeenCalledTimes(1);
  });

  it('renders MainLayout with responsive header, main container, and footer', () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <MainLayout>
            <div data-testid="dashboard-content">Responsive Studio Test View</div>
          </MainLayout>
        </AuthProvider>
      </BrowserRouter>
    );

    // Studio Header & Branding
    expect(screen.getAllByText(/VoxFlow/i).length).toBeGreaterThan(0);
    // Injected child content
    expect(screen.getByTestId('dashboard-content')).toBeInTheDocument();
    // Footer SaaS copy
    expect(screen.getByText(/VoxFlow Audio Studio/i)).toBeInTheDocument();
    expect(screen.getByText(/Spring Boot 3.3/i)).toBeInTheDocument();
  });
});
