package com.example.demo.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.repositories.UserRepository;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Isolate from MockMvc csrf() tests, which replace the filter's token repository.
@SpringBootTest(properties = "spring.application.name=csrf-cookie-integration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CsrfIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    private final ObjectMapper json = new ObjectMapper();

    private Cookie token() throws Exception {
        var response = mvc.perform(get("/api/csrf").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andReturn().getResponse();
        Cookie cookie = response.getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        assertEquals(cookie.getValue(), json.readTree(response.getContentAsString()).get("token").asText());
        return cookie;
    }

    private MockHttpServletRequestBuilder secured(MockHttpServletRequestBuilder request, Cookie cookie) {
        return request.cookie(cookie).header("X-XSRF-TOKEN", cookie.getValue());
    }

    @Test void anonymousTokenAndCorsPreflight() throws Exception {
        token();
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/accounts?userId=1")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"POST,/api/auth/register", "POST,/api/auth/login", "POST,/api/auth/logout",
            "POST,/api/accounts?userId=1", "PUT,/api/accounts/1/close?userId=1",
            "DELETE,/api/users/1/categories/1", "PATCH,/api/users/1/accounts/1/card/1/status/BLOCKED",
            "POST,/api/payments/transfer-own?userId=1", "PUT,/api/admin/bank-limits"})
    void missingAndInvalidTokensAreRejected(String method, String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path)).andExpect(status().isForbidden());
        mvc.perform(request(HttpMethod.valueOf(method), path).cookie(token()).header("X-XSRF-TOKEN", "invalid"))
                .andExpect(status().isForbidden());
    }

    @Test void realCookieRegisterLoginCrudLogoutAndRelogin() throws Exception {
        Cookie before = token();
        mvc.perform(secured(post("/api/auth/register"), before).contentType(MediaType.APPLICATION_JSON).content("""
            {"individual":{"firstName":"Csrf","lastName":"Test","cnp":"1990101123456",
            "phoneNumber":"0712345678","dateOfBirth":"1999-01-01"},
            "user":{"username":"csrf_user","email":"csrf@test.com","password":"password123"}}
            """)).andExpect(status().isOk());
        String login = """
            {"email":"csrf@test.com","password":"password123"}
            """;
        MockHttpSession session = new MockHttpSession();
        String oldSessionId = session.getId();
        var response = mvc.perform(secured(post("/api/auth/login"), before).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(login)).andExpect(status().isOk()).andReturn().getResponse();
        assertNotEquals(oldSessionId, session.getId());
        assertEquals(0, response.getCookie("XSRF-TOKEN").getMaxAge());
        int id = users.findByEmail("csrf@test.com").orElseThrow().getUserId();
        Cookie after = token();
        assertNotEquals(before.getValue(), after.getValue());
        mvc.perform(post("/api/accounts?userId=" + id).session(session).cookie(after)
                .header("X-XSRF-TOKEN", before.getValue())).andExpect(status().isForbidden());
        var created = mvc.perform(secured(post("/api/users/" + id + "/categories"), after).session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"CSRF category\"}"))
                .andExpect(status().isOk()).andReturn().getResponse();
        int categoryId = json.readTree(created.getContentAsString()).get("categoryId").asInt();
        String categoryPath = "/api/users/" + id + "/categories/" + categoryId;
        mvc.perform(secured(put(categoryPath), after).session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated\"}")).andExpect(status().isOk());
        mvc.perform(secured(delete(categoryPath), after).session(session)).andExpect(status().isOk());
        mvc.perform(secured(post("/api/accounts?userId=" + id), after).session(session)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"alias":"CSRF account","currency":"RON","externalIban":"RO49AAAA1B31007593840000","initialAmount":1000}
                """)).andExpect(status().isOk());
        mvc.perform(secured(post("/api/admin/unlock-user?email=csrf@test.com"), after).session(session))
                .andExpect(status().isForbidden());
        mvc.perform(secured(post("/api/auth/logout"), after).session(session)).andExpect(status().isOk());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/accounts?userId=" + id)).andExpect(status().isUnauthorized());
        mvc.perform(secured(post("/api/auth/login"), token()).contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk());
    }
}
