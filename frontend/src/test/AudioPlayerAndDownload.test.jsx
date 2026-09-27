import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import AudioPlayer from '../components/AudioPlayer';
import DownloadButton from '../components/DownloadButton';

describe('Audio Player and Download Component', () => {
  const fakeAudioUrl = 'blob:http://localhost:5173/test-audio-stream';

  it('renders AudioPlayer with title, ready status, and playback controls', () => {
    render(
      <AudioPlayer
        audioUrl={fakeAudioUrl}
        title="Jenny (en-US)"
        subtitle="31 characters • 1.00x rate • MP3 Audio"
      />
    );

    expect(screen.getByText('Jenny (en-US)')).toBeInTheDocument();
    expect(screen.getByText(/31 characters • 1.00x rate • MP3 Audio/i)).toBeInTheDocument();
    expect(screen.getByText(/Ready|Buffering/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Play speech audio/i })).toBeInTheDocument();
  });

  it('toggles play and pause state on AudioPlayer button click', async () => {
    render(
      <AudioPlayer
        audioUrl={fakeAudioUrl}
        title="Jenny (en-US)"
      />
    );

    const playBtn = screen.getByRole('button', { name: /Play speech audio/i });
    expect(playBtn).toBeInTheDocument();

    fireEvent.click(playBtn);
    // After click in mocked environment, play() resolves and toggles
    expect(await screen.findByRole('button', { name: /Pause speech audio/i })).toBeInTheDocument();
  });

  it('cycles playback rate when speed button is clicked', () => {
    render(
      <AudioPlayer
        audioUrl={fakeAudioUrl}
        title="Speed Test"
      />
    );

    const speedBtn = screen.getByTitle(/Change playback rate/i);
    expect(speedBtn).toHaveTextContent('1x');

    fireEvent.click(speedBtn);
    expect(speedBtn).toHaveTextContent('1.25x');

    fireEvent.click(speedBtn);
    expect(speedBtn).toHaveTextContent('1.5x');
  });

  it('toggles mute on mute button click', () => {
    render(
      <AudioPlayer
        audioUrl={fakeAudioUrl}
        title="Mute Test"
      />
    );

    const muteBtn = screen.getByRole('button', { name: /Mute audio/i });
    expect(muteBtn).toBeInTheDocument();

    fireEvent.click(muteBtn);
    expect(screen.getByRole('button', { name: /Unmute audio/i })).toBeInTheDocument();
  });

  it('DownloadButton is disabled when no audioUrl or audioBlob exists', () => {
    render(
      <DownloadButton
        audioUrl={null}
        audioBlob={null}
        filename="speech.mp3"
      />
    );

    const btn = screen.getByRole('button', { name: /Download synthesized MP3 audio file/i });
    expect(btn).toBeDisabled();
    expect(btn).toHaveTextContent(/Download \(No Audio\)/i);
  });

  it('DownloadButton is enabled with audioUrl and triggers download on click', () => {
    const handleClick = vi.fn();
    render(
      <DownloadButton
        audioUrl={fakeAudioUrl}
        filename="test_speech.mp3"
        onClick={handleClick}
      />
    );

    const btn = screen.getByRole('button', { name: /Download synthesized MP3 audio file/i });
    expect(btn).not.toBeDisabled();
    expect(btn).toHaveTextContent(/Download MP3 Audio/i);

    fireEvent.click(btn);
    expect(handleClick).toHaveBeenCalledTimes(1);
  });
});
