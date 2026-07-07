package com.podpisoff.subscription;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class SubscriptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String username = "subuser_" + System.nanoTime();
        token = registerUser(username);
    }

    @Test
    void subscriptionCrudAndFreeLimit() throws Exception {
        long editId = 0;
        for (int i = 1; i <= 3; i++) {
            MvcResult created = createSubscription("Sub " + i, "cat" + i, "2026-08-" + (10 + i))
                .andExpect(status().isOk())
                .andReturn();
            if (i == 3) {
                editId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
            }
        }

        mockMvc.perform(get("/api/subscriptions").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));

        createSubscription("Sub 4", "overflow", "2026-09-01")
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("FREE plan allows up to 3 subscriptions"));

        mockMvc.perform(put("/api/subscriptions/" + editId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title":"Edited",
                      "category":"tools",
                      "amount":19.99,
                      "currency":"RUB",
                      "nextBillingDate":"2026-11-01",
                      "billingPeriod":"MONTHLY",
                      "active":false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Edited"))
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/subscriptions/" + editId).header("Authorization", bearer(token)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/subscriptions").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        createSubscription("Sub 4", "new", "2026-09-01").andExpect(status().isOk());
    }

    @Test
    void freePlanRejectsForeignCurrency() throws Exception {
        createSubscription("Netflix", "streaming", "2026-07-15", "USD")
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Foreign currencies are available only for PRO plan"));
    }

    @Test
    void dashboardSummaryAndValidation() throws Exception {
        createSubscription("Netflix", "streaming", "2026-07-15");
        createSubscription("VPN", "software", "2026-12-01");

        mockMvc.perform(get("/api/dashboard/summary").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.monthlyByCurrency").exists())
            .andExpect(jsonPath("$.yearlyByCurrency").exists())
            .andExpect(jsonPath("$.monthSpendByCurrency").exists())
            .andExpect(jsonPath("$.selectedYear").exists())
            .andExpect(jsonPath("$.upcomingBilling").isArray());

        mockMvc.perform(get("/api/dashboard/summary").param("month", "13").header("Authorization", bearer(token)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid month"));
    }

    @Test
    void monthChargeOverridesDashboardSpend() throws Exception {
        MvcResult created = createSubscription("Cloud", "hosting", "2026-07-01")
            .andExpect(status().isOk())
            .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/subscriptions/" + id + "/month-charges")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "year": 2026,
                      "month": 3,
                      "amount": 80.00,
                      "note": "Overage"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(80.00))
            .andExpect(jsonPath("$.note").value("Overage"));

        mockMvc.perform(get("/api/dashboard/summary")
                .param("year", "2026")
                .param("month", "3")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.monthSpendByCurrency.RUB").value(80.00));

        mockMvc.perform(delete("/api/subscriptions/" + id + "/month-charges")
                .param("year", "2026")
                .param("month", "3")
                .header("Authorization", bearer(token)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dashboard/summary")
                .param("year", "2026")
                .param("month", "3")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.monthSpendByCurrency.RUB").value(9.99));
    }

    @Test
    void exportRequiresPro() throws Exception {
        createSubscription("ExportMe", "misc", "2026-08-01");

        mockMvc.perform(get("/api/export/subscriptions.xlsx").header("Authorization", bearer(token)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Export is available only for PRO plan"));
    }

    @Test
    void validationRejectsInvalidSubscription() throws Exception {
        mockMvc.perform(post("/api/subscriptions")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title":"",
                      "category":"x",
                      "amount":0,
                      "currency":"RUB",
                      "nextBillingDate":"2026-08-01",
                      "billingPeriod":"MONTHLY",
                      "active":true
                    }
                    """))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/subscriptions")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title":"Test",
                      "category":"x",
                      "amount":9.99,
                      "currency":"XYZ",
                      "nextBillingDate":"2026-08-01",
                      "billingPeriod":"MONTHLY",
                      "active":true
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unsupported currency"));
    }

    private org.springframework.test.web.servlet.ResultActions createSubscription(String title, String category, String date)
        throws Exception {
        return createSubscription(title, category, date, "RUB");
    }

    private org.springframework.test.web.servlet.ResultActions createSubscription(
        String title,
        String category,
        String date,
        String currency
    ) throws Exception {
        return mockMvc.perform(post("/api/subscriptions")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title":"%s",
                  "category":"%s",
                  "amount":9.99,
                  "currency":"%s",
                  "nextBillingDate":"%s",
                  "billingPeriod":"MONTHLY",
                  "active":true
                }
                """.formatted(title, category, currency, date)));
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
