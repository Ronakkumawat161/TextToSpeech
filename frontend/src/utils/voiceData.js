/**
 * Initial language options and mock voice dataset.
 * Configured so this can easily be replaced by dynamic backend API responses in future modules.
 */

export const DEFAULT_LANGUAGES = [
  { id: 'en', code: 'en-US', name: 'English', flag: '🇺🇸', nativeName: 'English' },
  { id: 'hi', code: 'hi-IN', name: 'Hindi', flag: '🇮🇳', nativeName: 'हिन्दी' },
  { id: 'gu', code: 'gu-IN', name: 'Gujarati', flag: '🇮🇳', nativeName: 'ગુજરાતી' },
  { id: 'mr', code: 'mr-IN', name: 'Marathi', flag: '🇮🇳', nativeName: 'मराठी' },
  { id: 'es', code: 'es-ES', name: 'Spanish', flag: '🇪🇸', nativeName: 'Español' },
  { id: 'fr', code: 'fr-FR', name: 'French', flag: '🇫🇷', nativeName: 'Français' },
  { id: 'de', code: 'de-DE', name: 'German', flag: '🇩🇪', nativeName: 'Deutsch' },
];

export const MOCK_VOICES_BY_LANGUAGE = {
  en: [
    { id: 'en-female-1', name: 'English Female', label: 'English Female (Emma - Natural)', gender: 'Female', quality: 'HD' },
    { id: 'en-male-1', name: 'English Male', label: 'English Male (Alex - Deep)', gender: 'Male', quality: 'HD' },
    { id: 'en-female-2', name: 'English Female (Studio)', label: 'English Female (Olivia - Expressive)', gender: 'Female', quality: 'Studio' },
    { id: 'en-male-2', name: 'English Male (Studio)', label: 'English Male (David - Clear)', gender: 'Male', quality: 'Studio' },
  ],
  hi: [
    { id: 'hi-female-1', name: 'Hindi Female', label: 'Hindi Female (Swara - मधुर)', gender: 'Female', quality: 'HD' },
    { id: 'hi-male-1', name: 'Hindi Male', label: 'Hindi Male (Madhur - स्पष्ट)', gender: 'Male', quality: 'HD' },
  ],
  gu: [
    { id: 'gu-female-1', name: 'Gujarati Female', label: 'Gujarati Female (Dhwani - સુમધુર)', gender: 'Female', quality: 'HD' },
    { id: 'gu-male-1', name: 'Gujarati Male', label: 'Gujarati Male (Nirav - સ્પષ્ટ)', gender: 'Male', quality: 'HD' },
  ],
  mr: [
    { id: 'mr-female-1', name: 'Marathi Female', label: 'Marathi Female (Aarohi - सुस्पष्ट)', gender: 'Female', quality: 'HD' },
    { id: 'mr-male-1', name: 'Marathi Male', label: 'Marathi Male (Omkar - धीरगंभीर)', gender: 'Male', quality: 'HD' },
  ],
  es: [
    { id: 'es-female-1', name: 'Spanish Female', label: 'Spanish Female (Sofía - Natural)', gender: 'Female', quality: 'HD' },
    { id: 'es-male-1', name: 'Spanish Male', label: 'Spanish Male (Mateo - Cálido)', gender: 'Male', quality: 'HD' },
  ],
  fr: [
    { id: 'fr-female-1', name: 'French Female', label: 'French Female (Camille - Douce)', gender: 'Female', quality: 'HD' },
    { id: 'fr-male-1', name: 'French Male', label: 'French Male (Antoine - Posé)', gender: 'Male', quality: 'HD' },
  ],
  de: [
    { id: 'de-female-1', name: 'German Female', label: 'German Female (Greta - Klar)', gender: 'Female', quality: 'HD' },
    { id: 'de-male-1', name: 'German Male', label: 'German Male (Lukas - Präzise)', gender: 'Male', quality: 'HD' },
  ],
};

export const SAMPLE_TEXTS = {
  en: "Welcome to VoxFlow Studio. Our advanced text-to-speech technology transforms written words into natural, expressive speech with lifelike cadence and tone.",
  hi: "वॉक्सफ़्लो स्टूडियो में आपका स्वागत है। हमारी टेक्स्ट-टू-स्पीच तकनीक आपके शब्दों को प्राकृतिक और सुंदर आवाज़ में बदलती है।",
  gu: "વોક્સફ્લો સ્ટુડિયોમાં આપનું હાર્દિક સ્વાગત છે. અમારી અદ્યતન સ્પીચ ટેકનોલોજી તમારા શબ્દોને કુદરતી અવાજમાં રૂપાંતરિત કરે છે.",
  mr: "वॉकस्फ्लो स्टुडिओमध्ये आपले स्वागत आहे. आमचे टेक्स्ट-टू-स्पीच तंत्रज्ञान आपले शब्द नैसर्गिक आवाजात रूपांतरित करते.",
  es: "Bienvenido a VoxFlow Studio. Nuestra tecnología convierte tus textos en audio claro y natural.",
  fr: "Bienvenue sur VoxFlow Studio. Transformez facilement vos textes en une voix fluide et naturelle.",
  de: "Willkommen bei VoxFlow Studio. Verwandeln Sie geschriebenen Text in lebensechte Sprache.",
};
