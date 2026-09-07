package com.example.userservice;

import com.example.userservice.domain.Individual;
import com.example.userservice.domain.User;
import com.example.userservice.dto.IndividualRegistrationDTO;
import com.example.userservice.dto.LoginRequestDTO;
import com.example.userservice.dto.RegistrationRequestDTO;
import com.example.userservice.dto.UserRegistrationDTO;
import com.example.userservice.repositories.IndividualRepository;
import com.example.userservice.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IndividualRepository individualRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    private Date createDateOfBirth(int yearsAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -yearsAgo);
        return calendar.getTime();
    }

    private RegistrationRequestDTO createValidRegistrationDTO(String suffix) {
        IndividualRegistrationDTO individual = new IndividualRegistrationDTO(
                "Ion" + suffix,
                "Popescu" + suffix,
                "1900101" + suffix,
                "0712345678",
                createDateOfBirth(25)
        );
        UserRegistrationDTO user = new UserRegistrationDTO(
                "user" + suffix,
                "user" + suffix + "@test.com",
                "Password123!"
        );
        return new RegistrationRequestDTO(individual, user);
    }

    private String obtainToken(String email, String password) throws Exception {
        LoginRequestDTO loginDTO = new LoginRequestDTO(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }

    @Test
    @DisplayName("1. Valid registration creates user and individual in DB with BCrypt password")
    void test1_validRegistration() throws Exception {
        RegistrationRequestDTO dto = createValidRegistrationDTO("101");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("user101@test.com").orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("user101");
        assertThat(user.getRole()).isEqualTo("USER");
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
        assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(user.getIndividual()).isNotNull();
        assertThat(user.getIndividual().getFirstName()).isEqualTo("Ion101");

        // Requirement 9: verify password is BCrypt hashed and not plaintext
        assertThat(user.getPasswordHash()).isNotEqualTo("Password123!");
        assertThat(user.getPasswordHash()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("Password123!", user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("2. Duplicate email returns 400 Bad Request")
    void test2_duplicateEmail() throws Exception {
        RegistrationRequestDTO dto1 = createValidRegistrationDTO("201");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        // Same email, different CNP and username
        RegistrationRequestDTO dto2 = createValidRegistrationDTO("202");
        dto2.getUser().setEmail(dto1.getUser().getEmail());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("This email already exists!"));
    }

    @Test
    @DisplayName("3. Duplicate username returns 400 Bad Request")
    void test3_duplicateUsername() throws Exception {
        RegistrationRequestDTO dto1 = createValidRegistrationDTO("301");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        RegistrationRequestDTO dto2 = createValidRegistrationDTO("302");
        dto2.getUser().setUsername(dto1.getUser().getUsername());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("This username was already chosen!"));
    }

    @Test
    @DisplayName("4. Invalid registration data returns 400 Bad Request")
    void test4_invalidRegistrationData() throws Exception {
        // Under 18 years old
        RegistrationRequestDTO underAgeDto = createValidRegistrationDTO("401");
        underAgeDto.getIndividual().setDateOfBirth(createDateOfBirth(16));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(underAgeDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You must be at least 18 years old to enroll!"));

        // Short password (< 8 chars)
        RegistrationRequestDTO shortPassDto = createValidRegistrationDTO("402");
        shortPassDto.getUser().setPassword("short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPassDto)))
                .andExpect(status().isBadRequest());

        // Missing/blank required fields
        RegistrationRequestDTO blankDto = createValidRegistrationDTO("403");
        blankDto.getIndividual().setFirstName("");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("5, 10 & 11. Valid login returns 200 with JWT containing identity and role claims")
    void test5_10_11_validLoginReturnsJwtWithClaims() throws Exception {
        RegistrationRequestDTO dto = createValidRegistrationDTO("501");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getUser().getEmail(), "Password123!");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(7200))
                .andExpect(jsonPath("$.username").value("user501"))
                .andExpect(jsonPath("$.email").value(dto.getUser().getEmail()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = responseNode.get("token").asText();
        assertThat(token.split("\\.")).hasSize(3);

        // Verify JWT claims
        Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(dto.getUser().getEmail());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(jwt.getClaimAsString("username")).isEqualTo("user501");
        assertThat((Object) jwt.getClaim("userId")).isNotNull();

        // Verify NO sensitive claims leak in the JWT
        assertThat(jwt.hasClaim("password")).isFalse();
        assertThat(jwt.hasClaim("passwordHash")).isFalse();
        assertThat(jwt.hasClaim("cnp")).isFalse();
        assertThat(jwt.hasClaim("phoneNumber")).isFalse();
    }

    @Test
    @DisplayName("6. Invalid password returns 400 Bad Request")
    void test6_invalidPassword() throws Exception {
        RegistrationRequestDTO dto = createValidRegistrationDTO("601");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        LoginRequestDTO wrongPassDTO = new LoginRequestDTO(dto.getUser().getEmail(), "WrongPassword!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid email or password!"));
    }

    @Test
    @DisplayName("7. User blocked after 3 failed login attempts")
    void test7_userBlockedAfterFailedAttempts() throws Exception {
        RegistrationRequestDTO dto = createValidRegistrationDTO("701");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        LoginRequestDTO wrongPassDTO = new LoginRequestDTO(dto.getUser().getEmail(), "WrongPass123!");

        // 3 failed attempts
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongPassDTO)))
                    .andExpect(status().isBadRequest());
        }

        User user = userRepository.findByEmail(dto.getUser().getEmail()).orElseThrow();
        assertThat(user.getStatus()).isEqualTo("BLOCKED");
        assertThat(user.getFailedLoginAttempts()).isEqualTo(3);

        // 4th attempt reports account blocked
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Your account is blocked! Please contact the bank!"));
    }

    @Test
    @DisplayName("8 & 15. Admin unlock restores blocked user access (by ID and by email)")
    void test8_15_adminUnlock() throws Exception {
        // Register regular user and block them
        RegistrationRequestDTO userDto = createValidRegistrationDTO("801");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated());

        LoginRequestDTO wrongPass = new LoginRequestDTO(userDto.getUser().getEmail(), "Wrong!");
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongPass)))
                    .andExpect(status().isBadRequest());
        }

        User user = userRepository.findByEmail(userDto.getUser().getEmail()).orElseThrow();
        assertThat(user.getStatus()).isEqualTo("BLOCKED");

        // Create an ADMIN user
        Individual adminInd = new Individual();
        adminInd.setFirstName("Admin");
        adminInd.setLastName("Boss");
        adminInd.setCnp("1800101888999");
        adminInd.setPhoneNumber("0799999999");
        adminInd.setDateOfBirth(createDateOfBirth(35));
        adminInd.setStatus("ACTIVE");
        adminInd.setCreatedAt(new Date());
        adminInd.setUpdatedAt(new Date());
        individualRepository.save(adminInd);

        User adminUser = new User();
        adminUser.setUsername("admin801");
        adminUser.setEmail("admin801@test.com");
        adminUser.setPasswordHash(passwordEncoder.encode("AdminPass123!"));
        adminUser.setRole("ADMIN");
        adminUser.setStatus("ACTIVE");
        adminUser.setFailedLoginAttempts(0);
        adminUser.setCreatedAt(new Date());
        adminUser.setUpdatedAt(new Date());
        adminUser.setIndividual(adminInd);
        userRepository.save(adminUser);

        String adminToken = obtainToken("admin801@test.com", "AdminPass123!");

        // Requirement 15: Admin can perform unlock
        mockMvc.perform(put("/api/admin/users/" + user.getUserId() + "/unlock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        User unblockedUser = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(unblockedUser.getStatus()).isEqualTo("ACTIVE");
        assertThat(unblockedUser.getFailedLoginAttempts()).isEqualTo(0);

        // Now user can log in with valid credentials
        String userToken = obtainToken(userDto.getUser().getEmail(), "Password123!");
        assertThat(userToken).isNotNull();

        // Block again to test unlock by email
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongPass)))
                    .andExpect(status().isBadRequest());
        }
        assertThat(userRepository.findById(user.getUserId()).orElseThrow().getStatus()).isEqualTo("BLOCKED");

        // Unlock by email
        mockMvc.perform(post("/api/admin/unlock-user")
                        .param("email", userDto.getUser().getEmail())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(user.getUserId()).orElseThrow().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("12. Protected endpoint without JWT returns 401 Unauthorized")
    void test12_protectedEndpointWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Autentificare necesara"));
    }

    @Test
    @DisplayName("13. Protected endpoint with invalid JWT returns 401 Unauthorized")
    void test13_protectedEndpointWithInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer invalid.jwt.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Autentificare necesara"));
    }

    @Test
    @DisplayName("14. Regular USER accessing ADMIN endpoint returns 403 Forbidden")
    void test14_userAccessingAdminEndpoint() throws Exception {
        RegistrationRequestDTO dto = createValidRegistrationDTO("1401");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        String userToken = obtainToken(dto.getUser().getEmail(), "Password123!");

        mockMvc.perform(put("/api/admin/users/1/unlock")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acces interzis"));

        mockMvc.perform(post("/api/admin/unlock-user")
                        .param("email", "anyone@test.com")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acces interzis"));
    }

    @Test
    @DisplayName("16 & 17. GET /api/users/me with valid JWT returns current user without sensitive fields")
    void test16_17_getCurrentUserWithValidJwt() throws Exception {
        RegistrationRequestDTO dto = createValidRegistrationDTO("1601");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        String token = obtainToken(dto.getUser().getEmail(), "Password123!");

        MvcResult result = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1601"))
                .andExpect(jsonPath("$.email").value(dto.getUser().getEmail()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.firstName").value("Ion1601"))
                .andExpect(jsonPath("$.lastName").value("Popescu1601"))
                .andExpect(jsonPath("$.phoneNumber").value("0712345678"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        // Crucial: check that passwordHash and CNP are NEVER exposed in profile response
        assertThat(responseBody).doesNotContain("passwordHash");
        assertThat(responseBody).doesNotContain("cnp");
        assertThat(responseBody).doesNotContain("19001011601");
    }

    @Test
    @DisplayName("18. Validate individual registration endpoint")
    void test18_validateIndividual() throws Exception {
        IndividualRegistrationDTO validDto = new IndividualRegistrationDTO(
                "Maria", "Ionescu", "2900101999999", "0711111111", createDateOfBirth(22)
        );

        mockMvc.perform(post("/api/auth/validate-individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isOk());

        // Register and verify duplicate CNP is rejected
        RegistrationRequestDTO reg = new RegistrationRequestDTO(
                validDto,
                new UserRegistrationDTO("maria999", "maria@test.com", "Password123!")
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/validate-individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("An individual with this CNP already exists!"));
    }

    @Test
    @DisplayName("19. Inter-service lookup endpoint returns safe UserLookupDTO")
    void test19_interServiceLookup() throws Exception {
        RegistrationRequestDTO dto = createValidRegistrationDTO("1901");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(dto.getUser().getEmail()).orElseThrow();
        String token = obtainToken(dto.getUser().getEmail(), "Password123!");

        // Lookup by ID
        MvcResult result = mockMvc.perform(get("/api/users/" + user.getUserId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.username").value("user1901"))
                .andExpect(jsonPath("$.email").value(dto.getUser().getEmail()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.firstName").value("Ion1901"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("password");
        assertThat(body).doesNotContain("cnp");

        // Lookup non-existent returns 404
        mockMvc.perform(get("/api/users/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resursa nu a fost gasita"));

        // Lookup by email
        mockMvc.perform(get("/api/users/by-email")
                        .param("email", dto.getUser().getEmail())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()));
    }

    @Test
    @DisplayName("20. Unsupported HTTP method returns 405 Method Not Allowed")
    void test20_unsupportedMethodReturns405() throws Exception {
        RegistrationRequestDTO dto = createValidRegistrationDTO("2001");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        String token = obtainToken(dto.getUser().getEmail(), "Password123!");

        mockMvc.perform(post("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Metoda HTTP nu este suportata"));
    }

    @Test
    @DisplayName("21. Public JWKS endpoint exposes RSA public key without private components")
    void test21_jwksEndpointReturnsPublicKeysOnly() throws Exception {
        MvcResult result = mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].e").value("AQAB"))
                .andExpect(jsonPath("$.keys[0].n").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].kid").isNotEmpty())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode key = root.get("keys").get(0);
        assertThat(key.has("d")).isFalse();
        assertThat(key.has("p")).isFalse();
        assertThat(key.has("q")).isFalse();
        assertThat(key.has("dp")).isFalse();
        assertThat(key.has("dq")).isFalse();
        assertThat(key.has("qi")).isFalse();
    }
}
