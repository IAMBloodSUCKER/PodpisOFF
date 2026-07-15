package com.podpisoff.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AdminUserManagementIntegrationTest {

    private static final String ADMIN_KEY = "test-admin-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private long targetUserId;

    @BeforeEach
    void setUp() throws Exception {
        String adminUsername = "admin_" + System.nanoTime();
        String targetUsername = "target_" + System.nanoTime();
        adminToken = registerUser(adminUsername);
        registerUser(targetUsername);
        targetUserId = findUserIdByUsername(targetUsername);
    }

    @Test
    void adminCanNotifyBlockUnblockAndDeleteUser() throws Exception {
        mockMvc.perform(post("/api/admin/users/" + targetUserId + "/notify")
                .header("Authorization", bearer(adminToken))
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Сообщение от админа\",\"body\":\"Привет!\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", bearer(adminToken))
                .header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/users/" + targetUserId + "/block")
                .header("Authorization", bearer(adminToken))
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"permanent\":false,\"hours\":24}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentlyBlocked").value(true));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"password123"}
                    """.formatted(findUsername(targetUserId))))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/users/" + targetUserId + "/unblock")
                .header("Authorization", bearer(adminToken))
                .header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentlyBlocked").value(false));

        mockMvc.perform(delete("/api/admin/users/" + targetUserId)
                .header("Authorization", bearer(adminToken))
                .header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", bearer(adminToken))
                .header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id==" + targetUserId + ")]").doesNotExist());
    }

    @Test
    void adminCanGrantAndRevokeProWithNotification() throws Exception {
        mockMvc.perform(post("/api/admin/users/" + targetUserId + "/plan")
                .header("Authorization", bearer(adminToken))
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"plan\":\"PRO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.effectivePlan").value("PRO"));

        String targetUsername = findUsername(targetUserId);
        String targetToken = loginUser(targetUsername);

        mockMvc.perform(get("/api/notifications")
                .header("Authorization", bearer(targetToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("PLAN_UPGRADED"));

        mockMvc.perform(post("/api/admin/users/" + targetUserId + "/plan")
                .header("Authorization", bearer(adminToken))
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"plan\":\"FREE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.effectivePlan").value("FREE"));

        mockMvc.perform(get("/api/notifications")
                .header("Authorization", bearer(targetToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("PLAN_DOWNGRADED"));
    }

    @Test
    void usersCanBeFilteredBySearchAndChannelStatus() throws Exception {
        String targetUsername = findUsername(targetUserId);
        String targetToken = loginUser(targetUsername);

        mockMvc.perform(patch("/api/settings")
                .header("Authorization", bearer(targetToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"target-filter@test.local\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", bearer(adminToken))
                .header("X-Admin-Key", ADMIN_KEY)
                .param("emailStatus", "set"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id==" + targetUserId + ")]").exists());

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", bearer(adminToken))
                .header("X-Admin-Key", ADMIN_KEY)
                .param("search", "target-filter@test.local"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value("target-filter@test.local"))
            .andExpect(jsonPath("$[0].emailNotificationsEnabled").exists())
            .andExpect(jsonPath("$[0].telegramLinked").exists());

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", bearer(adminToken))
                .header("X-Admin-Key", ADMIN_KEY)
                .param("telegramStatus", "not_connected")
                .param("search", targetUsername))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].telegramLinked").value(false));
    }

    @Test
    void cannotModifySelf() throws Exception {
        String selfUsername = "self_" + System.nanoTime();
        String token = registerUser(selfUsername);
        long selfId = findUserIdByUsername(selfUsername);
        mockMvc.perform(post("/api/admin/users/" + selfId + "/block")
                .header("Authorization", bearer(token))
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"permanent\":true}"))
            .andExpect(status().isBadRequest());
    }

    private long findUserIdByUsername(String username) throws Exception {
        String list = mockMvc.perform(get("/api/admin/users")
                .header("Authorization", bearer(adminToken))
                .header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        for (JsonNode user : objectMapper.readTree(list)) {
            if (username.equalsIgnoreCase(user.get("username").asText())) {
                return user.get("id").asLong();
            }
        }
        throw new IllegalStateException("User not found: " + username);
    }

    private String findUsername(long userId) throws Exception {
        String list = mockMvc.perform(get("/api/admin/users")
                .header("Authorization", bearer(adminToken))
                .header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        for (JsonNode user : objectMapper.readTree(list)) {
            if (user.get("id").asLong() == userId) {
                return user.get("username").asText();
            }
        }
        throw new IllegalStateException("User not found");
    }

    private String loginUser(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"password123"}
                    """.formatted(username)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
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
                      "locale":"ru",
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

    private static String bearer(String token) {
        return "Bearer " + token;
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

    private record CaptchaChallenge(String id, String answer) {
    }
}
