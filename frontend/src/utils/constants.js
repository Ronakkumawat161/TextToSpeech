export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');

export const APP_CONFIG = {
  name: 'VoxFlow',
  subtitle: 'Enterprise Text-to-Speech Platform',
  version: '2.0.0 (API Integrated)',
  academicProject: {
    course: 'Full-Stack Software Engineering',
    backend: 'Spring Boot 3 (Java 17+)',
    frontend: 'React + Vite + Tailwind CSS',
    database: 'PostgreSQL',
  }
};
