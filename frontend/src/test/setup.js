import '@testing-library/jest-dom';

// JSDOM mock for HTMLMediaElement playback
window.HTMLMediaElement.prototype.play = () => Promise.resolve();
window.HTMLMediaElement.prototype.pause = () => {};
window.HTMLMediaElement.prototype.load = () => {};

// Mock URL.createObjectURL and URL.revokeObjectURL
if (!window.URL.createObjectURL) {
  window.URL.createObjectURL = () => 'blob:http://localhost:5173/mock-audio-uuid';
}
if (!window.URL.revokeObjectURL) {
  window.URL.revokeObjectURL = () => {};
}

// Mock window.matchMedia for responsive checks
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
});
