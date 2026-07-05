package com.podpisoff.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginAndRecoverFlow() throws Exception {
        CaptchaChallenge captcha = issueCaptcha();

        String registerBody = """
            {
              "username": "flowuser",
              "password": "password123",
              "timezone": "Europe/Moscow",
              "locale": "ru",
              "termsAccepted": true,
              "captchaId": "%s",
              "captchaAnswer": "%s"
            }
            """.formatted(captcha.id(), captcha.answer());

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.auth.token").isNotEmpty())
            .andExpect(jsonPath("$.recoveryKey").isNotEmpty())
            .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String recoveryKey = registerJson.get("recoveryKey").asText();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Username already exists"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"flowuser","password":"password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"flowuser","password":"wrongpass1"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid credentials"));

        CaptchaChallenge recoverCaptcha = issueCaptcha();
        mockMvc.perform(post("/api/auth/recover-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"flowuser",
                      "recoveryKey":"%s",
                      "newPassword":"newpassword1",
                      "captchaId":"%s",
                      "captchaAnswer":"%s"
                    }
                    """.formatted(recoveryKey, recoverCaptcha.id(), recoverCaptcha.answer())))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"flowuser","password":"newpassword1"}
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void registerRejectsShortPasswordBeforeCaptcha() throws Exception {
        CaptchaChallenge captcha = issueCaptcha();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"shortpw",
                      "password":"123",
                      "timezone":"UTC",
                      "locale":"en",
                      "termsAccepted": true,
                      "captchaId":"%s",
                      "captchaAnswer":"%s"
                    }
                    """.formatted(captcha.id(), captcha.answer())))
            .andExpect(status().isBadRequest());

        CaptchaChallenge retry = issueCaptcha();
        assertThat(retry.id()).isNotEqualTo(captcha.id());
    }

    @Test
    void registerRejectsDuplicateUsernameBeforeCaptchaConsumption() throws Exception {
        CaptchaChallenge first = issueCaptcha();
        registerUser("dupuser", "password123", first);

        CaptchaChallenge second = issueCaptcha();
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"dupuser",
                      "password":"password123",
                      "timezone":"UTC",
                      "locale":"en",
                      "termsAccepted": true,
                      "captchaId":"%s",
                      "captchaAnswer":"%s"
                    }
                    """.formatted(second.id(), second.answer())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Username already exists"));

        CaptchaChallenge third = issueCaptcha();
        assertThat(third.id()).isNotEqualTo(second.id());
    }

    @Test
    void captchaMismatchAndExpired() throws Exception {
        CaptchaChallenge captcha = issueCaptcha();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"captchauser",
                      "password":"password123",
                      "timezone":"UTC",
                      "locale":"en",
                      "termsAccepted": true,
                      "captchaId":"%s",
                      "captchaAnswer":"999"
                    }
                    """.formatted(captcha.id())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Captcha mismatch"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"captchauser",
                      "password":"password123",
                      "timezone":"UTC",
                      "locale":"en",
                      "termsAccepted": true,
                      "captchaId":"%s",
                      "captchaAnswer":"%s"
                    }
                    """.formatted(captcha.id(), captcha.answer())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Captcha expired"));
    }

    @Test
    void usernameCheckWorks() throws Exception {
        mockMvc.perform(get("/api/auth/username-check").param("username", "freeuser"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(true));

        CaptchaChallenge captcha = issueCaptcha();
        registerUser("freeuser", "password123", captcha);

        mockMvc.perform(get("/api/auth/username-check").param("username", "freeuser"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(false));
    }

    private void registerUser(String username, String password, CaptchaChallenge captcha) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "password":"%s",
                      "timezone":"UTC",
                      "locale":"en",
                      "termsAccepted": true,
                      "captchaId":"%s",
                      "captchaAnswer":"%s"
                    }
                    """.formatted(username, password, captcha.id(), captcha.answer())))
            .andExpect(status().isOk());
    }

    private CaptchaChallenge issueCaptcha() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/captcha"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String id = json.get("captchaId").asText();
        String question = json.get("question").asText();
        return new CaptchaChallenge(id, solve(question));
    }

    private String solve(String question) {
        String expression = question.replace(" = ?", "").trim();
        String[] parts = expression.split(" \\+ ");
        int sum = Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim());
        return String.valueOf(sum);
    }

    private record CaptchaChallenge(String id, String answer) {
    }
}
