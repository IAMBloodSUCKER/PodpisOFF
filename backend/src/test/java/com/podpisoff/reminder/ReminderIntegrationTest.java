package com.podpisoff.reminder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.podpisoff.user.Plan;
import com.podpisoff.user.User;
import com.podpisoff.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
class ReminderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String token;
    private String username;

    @BeforeEach
    void setUp() throws Exception {
        username = "remuser_" + System.nanoTime();
        token = registerUser(username);
    }

    @Test
    void freePlanReminderLimit() throws Exception {
        switchToFreePlan();
        for (int i = 1; i <= 5; i++) {
            createReminder("Reminder " + i).andExpect(status().isOk());
        }

        createReminder("Reminder 6")
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("FREE plan allows up to 5 reminders"));
    }

    @Test
    void createYearlyReminder() throws Exception {
        mockMvc.perform(post("/api/reminders")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title":"Annual check",
                      "note":"Yearly",
                      "remindAt":"2020-05-10T09:00:00",
                      "repeat":"YEARLY",
                      "done":false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.repeat").value("YEARLY"))
            .andExpect(jsonPath("$.nextRemindAt").exists());
    }

    @Test
    void acknowledgeRecurringReminderAdvancesDate() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/reminders")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title":"Monthly bill",
                      "remindAt":"2026-07-01T10:00:00",
                      "repeat":"MONTHLY",
                      "done":false
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/reminders/" + id)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title":"Monthly bill",
                      "remindAt":"2026-07-01T10:00:00",
                      "repeat":"MONTHLY",
                      "done":true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.done").value(false))
            .andExpect(jsonPath("$.remindAt").value("2026-08-01T10:00:00"));

        mockMvc.perform(get("/api/reminders").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].repeat").value("MONTHLY"));
    }

    private void switchToFreePlan() {
        User user = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        user.setPlan(Plan.FREE);
        user.setPlanExpiresAt(null);
        userRepository.save(user);
    }

    private org.springframework.test.web.servlet.ResultActions createReminder(String title) throws Exception {
        return mockMvc.perform(post("/api/reminders")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title":"%s",
                  "remindAt":"2026-08-01T10:00:00",
                  "repeat":"ONCE",
                  "done":false
                }
                """.formatted(title)));
    }

    private String registerUser(String username) throws Exception {
        CaptchaChallenge captcha = issueCaptcha();
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "password":"password123",
                      "timezone":"UTC",
                      "locale":"en",
                      "termsAccepted": true,
                      "captchaId":"%s",
                      "captchaAnswer":"%s"
                    }
                    """.formatted(username, captcha.id(), captcha.answer())))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("auth").get("token").asText();
    }

    private CaptchaChallenge issueCaptcha() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/captcha")).andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String question = json.get("question").asText();
        String expression = question.replace(" = ?", "").trim();
        String[] parts = expression.split(" \\+ ");
        int sum = Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim());
        return new CaptchaChallenge(json.get("captchaId").asText(), String.valueOf(sum));
    }

    private String bearer(String jwt) {
        return "Bearer " + jwt;
    }

    private record CaptchaChallenge(String id, String answer) {
    }
}
