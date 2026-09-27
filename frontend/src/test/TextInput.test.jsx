import React, { useState } from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import TextInput from '../components/TextInput';

describe('TextInput Component', () => {
  it('renders correctly with label, placeholder, and counter', () => {
    render(
      <TextInput
        value=""
        onChange={() => {}}
        placeholder="Enter speech text here..."
      />
    );

    expect(screen.getByLabelText(/Speech Input Text/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Enter speech text here.../i)).toBeInTheDocument();
    const counter = document.getElementById('tts-text-input-counter');
    expect(counter).toHaveTextContent('0 / 5,000 chars');
    expect(counter).toHaveTextContent('0 words');
  });

  it('updates text and calculates character and word count correctly', () => {
    function Wrapper() {
      const [val, setVal] = useState('');
      return <TextInput value={val} onChange={setVal} />;
    }

    render(<Wrapper />);
    const textarea = screen.getByRole('textbox', { name: /Speech Input Text/i });

    fireEvent.change(textarea, { target: { value: 'Hello world from VoxFlow studio' } });

    expect(textarea.value).toBe('Hello world from VoxFlow studio');
    // 31 characters, 5 words
    const counter = document.getElementById('tts-text-input-counter');
    expect(counter).toHaveTextContent('31 / 5,000 chars');
    expect(counter).toHaveTextContent('5 words');
  });

  it('displays clear button when text is present and clears on click', () => {
    const handleClear = vi.fn();
    const handleChange = vi.fn();

    const { rerender } = render(
      <TextInput value="Some speech text" onChange={handleChange} onClear={handleClear} />
    );

    const clearBtn = screen.getByRole('button', { name: /Clear all input text/i });
    expect(clearBtn).toBeInTheDocument();

    fireEvent.click(clearBtn);
    expect(handleClear).toHaveBeenCalledTimes(1);
  });

  it('displays validation error alert with role="alert"', () => {
    render(
      <TextInput
        value="hi"
        onChange={() => {}}
        validationError="Input text must be at least 3 characters."
      />
    );

    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent('Input text must be at least 3 characters.');
    const textarea = screen.getByRole('textbox');
    expect(textarea).toHaveAttribute('aria-invalid', 'true');
  });

  it('respects maxLength and prevents input beyond 5000 characters', () => {
    const handleChange = vi.fn();
    render(<TextInput value="" onChange={handleChange} maxLength={5000} />);

    const textarea = screen.getByRole('textbox');
    expect(textarea).toHaveAttribute('maxlength', '5000');
  });
});
