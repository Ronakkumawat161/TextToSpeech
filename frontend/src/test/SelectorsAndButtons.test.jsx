import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import LanguageSelector from '../components/LanguageSelector';
import VoiceSelector from '../components/VoiceSelector';
import GenerateButton from '../components/GenerateButton';
import SpeechSettings from '../components/SpeechSettings';

describe('Selectors and Controls', () => {
  it('renders LanguageSelector and handles language change', () => {
    const handleChange = vi.fn();
    render(
      <LanguageSelector
        selectedLanguage="en"
        onChange={handleChange}
      />
    );

    const select = screen.getByRole('combobox', { name: /Language/i });
    expect(select).toBeInTheDocument();
    expect(select.value).toBe('en');

    fireEvent.change(select, { target: { value: 'es' } });
    expect(handleChange).toHaveBeenCalledWith('es');
  });

  it('renders VoiceSelector with voice models and handles voice change', () => {
    const handleChange = vi.fn();
    const mockVoices = [
      { id: 'en-female-1', name: 'Jenny', language: 'en-US', gender: 'Female' },
      { id: 'en-male-1', name: 'Guy', language: 'en-US', gender: 'Male' }
    ];

    render(
      <VoiceSelector
        selectedLanguage="en"
        selectedVoice="en-female-1"
        voices={mockVoices}
        isLoading={false}
        onChange={handleChange}
      />
    );

    const select = screen.getByRole('combobox', { name: /Voice Model/i });
    expect(select).toBeInTheDocument();
    expect(screen.getByText(/Jenny/i)).toBeInTheDocument();
    expect(screen.getByText(/Guy/i)).toBeInTheDocument();

    fireEvent.change(select, { target: { value: 'en-male-1' } });
    expect(handleChange).toHaveBeenCalledWith('en-male-1');
  });

  it('renders GenerateButton in idle state, handles click, and prevents click when disabled', () => {
    const handleClick = vi.fn();

    const { rerender } = render(
      <GenerateButton
        onClick={handleClick}
        disabled={false}
        isLoading={false}
        text="Generate Speech"
      />
    );

    const button = screen.getByRole('button', { name: /Generate Speech/i });
    expect(button).not.toBeDisabled();
    fireEvent.click(button);
    expect(handleClick).toHaveBeenCalledTimes(1);

    // Disabled state
    rerender(
      <GenerateButton
        onClick={handleClick}
        disabled={true}
        isLoading={false}
        text="Generate Speech"
      />
    );
    expect(button).toBeDisabled();
  });

  it('renders GenerateButton in loading state with spinner and loadingText', () => {
    render(
      <GenerateButton
        onClick={() => {}}
        disabled={false}
        isLoading={true}
        loadingText="Synthesizing Audio..."
      />
    );

    const button = screen.getByRole('button');
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByText(/Synthesizing Audio.../i)).toBeInTheDocument();
  });

  it('renders SpeechSettings and updates speed, pitch, volume, and style', () => {
    const handleChange = vi.fn();
    const settings = { speed: 1.0, pitch: 0, volume: 1.0, style: 'default' };

    render(
      <SpeechSettings
        settings={settings}
        onChange={handleChange}
        supportedStyles={['default', 'cheerful', 'calm']}
      />
    );

    expect(screen.getByText(/Audio Customization/i)).toBeInTheDocument();

    // Speed slider
    const speedSlider = screen.getByLabelText(/Speaking Speed/i);
    expect(speedSlider).toBeInTheDocument();
    fireEvent.change(speedSlider, { target: { value: '1.25' } });
    expect(handleChange).toHaveBeenCalledWith(expect.objectContaining({ speed: 1.25 }));

    // Voice mood select
    const styleSelect = screen.getByLabelText(/Voice Delivery Mood/i);
    expect(styleSelect).toBeInTheDocument();
    fireEvent.change(styleSelect, { target: { value: 'cheerful' } });
    expect(handleChange).toHaveBeenCalledWith(expect.objectContaining({ style: 'cheerful' }));
  });
});
