package com.example.tts.service.provider;

import com.example.tts.config.TtsProperties;
import com.example.tts.exception.TtsAuthenticationException;
import com.example.tts.exception.TtsProviderUnavailableException;
import com.example.tts.exception.TtsSynthesisException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TtsProviderImplTest {

    private TtsProperties properties;
    private MockTtsProvider mockTtsProvider;
    private TtsProviderImpl ttsProvider;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        properties = new TtsProperties();
        mockTtsProvider = new MockTtsProvider();
        ttsProvider = new TtsProviderImpl(properties, mockTtsProvider);

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(ttsProvider, "restTemplate");
        assertNotNull(restTemplate);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testFallbackToMockProviderWhenNoCredentials() {
        properties.setApiKey("");
        properties.setProvider("MOCK");

        byte[] result = ttsProvider.synthesize("Hello world", "en-US", "en-female-1");

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertEquals("MOCK", ttsProvider.getProviderName());
    }

    @Test
    void testExternalSynthesisSuccessWhenCredentialsConfigured() {
        properties.setApiKey("test-real-api-key");
        properties.setProvider("AZURE");
        properties.setRegion("eastus");
        properties.setEndpoint("https://eastus.tts.speech.microsoft.com/cognitiveservices/v1");

        byte[] fakeWavAudio = new byte[]{0x52, 0x49, 0x46, 0x46, 0x00, 0x01, 0x02};

        mockServer.expect(requestTo("https://eastus.tts.speech.microsoft.com/cognitiveservices/v1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Ocp-Apim-Subscription-Key", "test-real-api-key"))
                .andRespond(withSuccess(fakeWavAudio, MediaType.valueOf("audio/mpeg")));

        byte[] result = ttsProvider.synthesize("Hello world", "en-US", "en-female-1");

        assertArrayEquals(fakeWavAudio, result);
        mockServer.verify();
    }

    @Test
    void testInvalidCredentialsThrowsTtsAuthenticationException() {
        properties.setApiKey("invalid-bad-key");
        properties.setProvider("AZURE");
        properties.setRegion("eastus");

        mockServer.expect(anything())
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(TtsAuthenticationException.class, () ->
                ttsProvider.synthesize("Hello world", "en-US", "en-female-1"));

        mockServer.verify();
    }

    @Test
    void testProviderUnavailableThrowsTtsProviderUnavailableException() {
        properties.setApiKey("test-api-key");
        properties.setProvider("AZURE");
        properties.setRegion("eastus");

        mockServer.expect(anything())
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(TtsProviderUnavailableException.class, () ->
                ttsProvider.synthesize("Hello world", "en-US", "en-female-1"));

        mockServer.verify();
    }

    @Test
    void testEmptyAudioResponseThrowsTtsSynthesisException() {
        properties.setApiKey("test-api-key");
        properties.setProvider("AZURE");
        properties.setRegion("eastus");

        mockServer.expect(anything())
                .andRespond(withSuccess(new byte[0], MediaType.valueOf("audio/mpeg")));

        assertThrows(TtsSynthesisException.class, () ->
                ttsProvider.synthesize("Hello world", "en-US", "en-female-1"));

        mockServer.verify();
    }

    @Test
    void testCustomProsodyAndStyleSsmlSentToProvider() {
        properties.setApiKey("test-real-api-key");
        properties.setProvider("AZURE");
        properties.setRegion("eastus");
        properties.setEndpoint("https://eastus.tts.speech.microsoft.com/cognitiveservices/v1");

        byte[] fakeAudio = new byte[]{0x01, 0x02, 0x03};

        mockServer.expect(requestTo("https://eastus.tts.speech.microsoft.com/cognitiveservices/v1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("rate='+25%'")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pitch='+10%'")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("volume='80%'")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<mstts:express-as style='cheerful'>")))
                .andRespond(withSuccess(fakeAudio, MediaType.valueOf("audio/mpeg")));

        byte[] result = ttsProvider.synthesize("Hello world", "en-US", "en-female-1", 1.25, 10, 0.8, "cheerful");

        assertArrayEquals(fakeAudio, result);
        mockServer.verify();
    }

    @Test
    void testUnsupportedStyleGracefullyRetriedWithoutStyleTag() {
        properties.setApiKey("test-real-api-key");
        properties.setProvider("AZURE");
        properties.setRegion("eastus");
        properties.setEndpoint("https://eastus.tts.speech.microsoft.com/cognitiveservices/v1");

        byte[] fakeAudio = new byte[]{0x01, 0x02, 0x03};

        // First attempt with style 'whispering' returns 400 Bad Request indicating style is not supported
        mockServer.expect(requestTo("https://eastus.tts.speech.microsoft.com/cognitiveservices/v1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<mstts:express-as style='whispering'>")))
                .andRespond(withBadRequest().body("Unsupported style or express-as for this voice"));

        // Second fallback attempt without style succeeds
        mockServer.expect(requestTo("https://eastus.tts.speech.microsoft.com/cognitiveservices/v1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<mstts:express-as"))))
                .andRespond(withSuccess(fakeAudio, MediaType.valueOf("audio/mpeg")));

        byte[] result = ttsProvider.synthesize("Hello world", "en-US", "en-female-1", 1.0, 0, 1.0, "whispering");

        assertArrayEquals(fakeAudio, result);
        mockServer.verify();
    }

    @Test
    void testUnsupportedProsodyGracefullyRetriedWithoutCustomProsody() {
        properties.setApiKey("test-real-api-key");
        properties.setProvider("AZURE");
        properties.setRegion("eastus");
        properties.setEndpoint("https://eastus.tts.speech.microsoft.com/cognitiveservices/v1");

        byte[] fakeAudio = new byte[]{0x05, 0x06, 0x07};

        // First attempt with prosody returns 400 Bad Request indicating prosody pitch is rejected
        mockServer.expect(requestTo("https://eastus.tts.speech.microsoft.com/cognitiveservices/v1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pitch='+15%'")))
                .andRespond(withBadRequest().body("Unsupported prosody pitch parameter"));

        // Second fallback attempt without custom prosody succeeds
        mockServer.expect(requestTo("https://eastus.tts.speech.microsoft.com/cognitiveservices/v1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pitch='+0%'")))
                .andRespond(withSuccess(fakeAudio, MediaType.valueOf("audio/mpeg")));

        byte[] result = ttsProvider.synthesize("Hello world", "en-US", "en-female-1", 1.0, 15, 1.0, "default");

        assertArrayEquals(fakeAudio, result);
        mockServer.verify();
    }

    @Test
    void testProviderCapabilitiesDefaultToTrue() {
        assertTrue(ttsProvider.supportsSpeed());
        assertTrue(ttsProvider.supportsPitch());
        assertTrue(ttsProvider.supportsVolume());
        assertTrue(ttsProvider.supportsStyles());
    }
}

