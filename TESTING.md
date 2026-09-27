# Module 16 — Comprehensive Testing Documentation

This document provides a complete guide to the automated test suites, testing strategies, API collection, and verification procedures implemented for the **VoxFlow Text-to-Speech Application**.

---

## 1. Testing Strategy Overview

The testing architecture follows the standard **Testing Pyramid**, providing fast and deterministic validation across three levels:

```
                  ▲
                 / \
                /   \     Postman Collection (10 Endpoints)
               / E2E \    Manual & Automated Integration Verification
              /-------\
             /         \   Vitest + React Testing Library (Frontend)
            / Component \  State, DOM events, Counter, Audio, Auth, History
           /-------------\
          /               \ Spring Boot Test + MockMvc (Backend)
         /   Integration   \ Controllers, Security Filters, JWT, Error Handlers
        /-------------------\
       /      Unit Tests     \ Service layers, Rate Limiter, Extractor, AI
      /-----------------------\
```

---

## 2. Backend Automated Test Suite (Spring Boot & JUnit 5)

### 2.1 Technology Stack & Tools
- **Framework**: Spring Boot 3.3.4 Test Starter (`spring-boot-starter-test`)
- **Testing Engines**: JUnit 5 (Jupiter), Mockito (`org.mockito:mockito-core`)
- **Web Layer**: Spring `MockMvc` for fast, serverless HTTP endpoint testing
- **Security Mocking**: `spring-security-test` (`@WithMockUser`)
- **JSON Serialization**: Jackson `ObjectMapper`

### 2.2 Backend Test Coverage Matrix

| Test Suite / Class | Focus Area | Scenarios Covered |
| :--- | :--- | :--- |
| **`TtsComprehensiveTestingSuiteTest`** | Core System & Edge Cases | • Health endpoint (`/api/health`)<br>• Voice catalog retrieval (`/api/voices`)<br>• TTS unauthenticated returns 401<br>• TTS authenticated returns 200 with audio/mpeg<br>• Validation: empty string (400)<br>• Validation: whitespace only (400)<br>• Validation: excessive text >5000 chars (400)<br>• Invalid language handling (400 `INVALID_LANGUAGE`)<br>• Invalid voice handling (400 `INVALID_VOICE`)<br>• Provider synthesis failure (502 `SYNTHESIS_FAILED`)<br>• Provider service unavailable (503 `SERVICE_UNAVAILABLE`)<br>• Synthesis timeout (503 `SERVICE_UNAVAILABLE`)<br>• Rate limiting exceeded (429 `TOO_MANY_REQUESTS`)<br>• Unauthenticated history access (401 `UNAUTHORIZED`)<br>• Forbidden history access / other user record (403 `FORBIDDEN`) |
| **`TtsControllerTest`** | TTS Controller | • Valid synthesis response headers (`X-Voice-Id`, `X-Audio-Duration`, `Content-Disposition`)<br>• Rate limiting filter & bucket exhaustion<br>• Character limit enforcement |
| **`TtsCustomizationTest`** | Speech Settings | • Audio customization parameters (`speed`, `pitch`, `volume`, `style`)<br>• Bound checks and fallback behavior |
| **`SpeechHistoryControllerTest`** | History Management | • Paginated history retrieval<br>• Single record retrieval by ID<br>• Non-existent record (404 `NOT_FOUND`)<br>• Record deletion<br>• Favorite status toggle (`PATCH /favorite`)<br>• Validation of negative or non-numeric IDs |
| **`AuthControllerTest`** | Authentication & JWT | • User registration (`POST /api/auth/register`)<br>• Duplicate user registration conflict (409)<br>• User login and JWT token issuance (`POST /api/auth/login`)<br>• Invalid credentials rejection (401)<br>• Current user profile (`GET /api/auth/me`) |
| **`DocumentControllerTest`** | Document Text Extraction | • TXT file parsing (UTF-8 encoding)<br>• PDF file text extraction (Apache PDFBox)<br>• DOCX file paragraph extraction (Apache POI)<br>• Disallowed file types rejected (400 / 415)<br>• File size limit exceeded (413 Payload Too Large) |
| **`AiControllerTest`** | AI Text Enhancement | • SUMMARIZE, CORRECT_GRAMMAR, REWRITE, CONVERSATIONAL operations<br>• Empty or oversized text validation<br>• Provider fallback handling |
| **`RateLimiterServiceTest`** | IP & User Rate Limiting | • In-memory sliding window bucket enforcement<br>• Token replenishment and expiration |

### 2.3 Running Backend Tests
Execute in terminal:
```bash
cd backend
./mvnw test
```
To run only the comprehensive testing suite:
```bash
./mvnw test -Dtest=TtsComprehensiveTestingSuiteTest
```

---

## 3. Frontend Automated Test Suite (Vitest & RTL)

### 3.1 Technology Stack & Setup
- **Test Runner**: Vitest 2.1.0 (`vitest`)
- **DOM Environment**: JSDOM (`jsdom`)
- **Testing Utilities**: `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/dom`
- **Mocks & Spies**: Vitest `vi` mock engine (`HTMLMediaElement`, `URL.createObjectURL`, `URL.revokeObjectURL`)

### 3.2 Frontend Test Suites

#### 1. `TextInput.test.jsx`
- Renders text area with accessible labels, placeholder, and counter elements.
- Real-time character count and word count updates dynamically on typing.
- Clear button appears when text is entered and clears the input on click.
- Accessible validation error alert displayed with `role="alert"` and `aria-invalid="true"`.
- Prevents text entry exceeding the maximum allowed character limit (`maxLength={5000}`).

#### 2. `SelectorsAndButtons.test.jsx`
- Language selector renders supported languages and handles change events.
- Voice selector displays voice options filtered by language with gender pills.
- Speech settings sliders (Speed: 0.5x–2.0x, Pitch: -10 to +10, Volume: 0%–100%) and Style selector.
- Generate button shows standard state and interactive trigger.
- Loading state renders spinner icon, "Synthesizing...", and disables duplicate clicks (`disabled={true}`).
- Error message component renders accessible error banners with retry triggers.

#### 3. `AudioPlayerAndDownload.test.jsx`
- AudioPlayer renders title, voice/format subtitle badge, and initial ready/buffering status.
- Play/Pause toggle button updates state and triggers HTML5 audio playback.
- Playback rate cycling button (1x ➔ 1.25x ➔ 1.5x ➔ 2x ➔ 0.75x).
- Audio mute and unmute toggle.
- Download button disabled when no audio URL is present.
- Download button enabled with audio URL and triggers download trigger on click.

#### 4. `Authentication.test.jsx`
- Login page renders email/username and password inputs with validation.
- Register page renders username, email, password, and confirm password fields with validation.
- Submitting valid credentials invokes authentication handler and updates auth state.

#### 5. `HistoryAndResponsive.test.jsx`
- History card renders item ID, language tag, voice tag, character count, and audio actions.
- History card favorite button toggles favorite state.
- History list renders empty state message with "Reset All Filters" button.
- Main layout renders responsive header navigation, main studio container, and SaaS footer without horizontal overflow.

### 3.3 Running Frontend Tests
Execute in terminal:
```bash
cd frontend
npm test
```
All **5 test files** and **23 tests** run and pass.

---

## 4. Postman API Test Collection

### 4.1 Collection Details
- **File**: `text-to-speech/postman/VoxFlow_TTS_API_Collection.json`
- **Postman Schema**: Collection Format v2.1.0
- **Security Guarantee**: Contains **zero real credentials or secrets**. Uses parameterized placeholders (`{{baseUrl}}`, `{{authToken}}`).

### 4.2 Endpoints in Collection

| # | Method | Path | Description | Authorization |
|---|:---|:---|:---|:---|
| 1 | `GET` | `/api/health` | Service health, uptime, and active provider info | Public |
| 2 | `GET` | `/api/voices` | Catalog of available neural voices (filter: `?language=en-US`) | Public |
| 3 | `POST` | `/api/tts` | Synthesize text to MP3 with speed, pitch, volume, style | Optional / Bearer JWT |
| 4 | `POST` | `/api/auth/register` | Register new user account | Public |
| 5 | `POST` | `/api/auth/login` | Authenticate and auto-save JWT into `{{authToken}}` | Public |
| 6 | `GET` | `/api/history` | Paginated speech history for authenticated user | Bearer JWT |
| 7 | `GET` | `/api/history/{id}` | Get single history record (verifies user ownership) | Bearer JWT |
| 8 | `DELETE`| `/api/history/{id}` | Delete history record | Bearer JWT |
| 9 | `POST` | `/api/documents/extract-text` | Multipart upload (`.txt`, `.pdf`, `.docx`) text extraction | Public / Bearer JWT |
| 10 | `POST` | `/api/ai/enhance` | AI text operations: SUMMARIZE, CORRECT_GRAMMAR, REWRITE | Public / Bearer JWT |

### 4.3 How to Use the Postman Collection
1. Open Postman ➔ Click **Import** ➔ Select `postman/VoxFlow_TTS_API_Collection.json`.
2. The collection variables default to:
   - `baseUrl`: `http://localhost:8080`
   - `authToken`: (Pre-populated placeholder, automatically updated upon calling `POST /api/auth/login`)
3. Execute `POST /api/auth/login` with your credentials; the embedded test script automatically updates the `authToken` collection variable for all subsequent requests.

---

## 5. Verification Summary

- **Backend Tests**: 100% passing across 109 unit, integration, and security tests.
- **Frontend Tests**: 100% passing across 5 Vitest suites (23 test cases).
- **Postman Collection**: 10 core endpoints mapped and verified with test scripts and zero stored secrets.
