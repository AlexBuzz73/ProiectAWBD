package com.example.accountservice;

import com.example.accountservice.client.UserClient;
import com.example.accountservice.dto.UserLookupDTO;
import com.example.accountservice.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserClientTest {

    private UserClient userClient;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        userClient = new UserClient("http://localhost:8081");

        // Build mock rest service server for the internal RestClient
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8081");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        Field restClientField = UserClient.class.getDeclaredField("restClient");
        restClientField.setAccessible(true);
        restClientField.set(userClient, restClient);
    }

    @Test
    @DisplayName("findUserById returns UserLookupDTO on 200 OK")
    void testFindUserByIdSuccess() throws Exception {
        UserLookupDTO expected = new UserLookupDTO(101, "ion", "ion@test.com", "USER", true);

        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8081/api/users/101"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        Optional<UserLookupDTO> result = userClient.findUserById(101);
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("ion");
        mockServer.verify();
    }

    @Test
    @DisplayName("findUserById returns empty Optional on 404 Not Found")
    void testFindUserByIdNotFound() {
        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8081/api/users/999"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        Optional<UserLookupDTO> result = userClient.findUserById(999);
        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("findUserByEmail returns UserLookupDTO on 200 OK")
    void testFindUserByEmailSuccess() throws Exception {
        UserLookupDTO expected = new UserLookupDTO(102, "maria", "maria@test.com", "USER", true);

        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8081/api/users/by-email?email=maria@test.com"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        Optional<UserLookupDTO> result = userClient.findUserByEmail("maria@test.com");
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("maria@test.com");
        mockServer.verify();
    }

    @Test
    @DisplayName("requireUserByEmail throws ResourceNotFoundException when user missing")
    void testRequireUserByEmailNotFound() {
        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8081/api/users/by-email?email=missing@test.com"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> userClient.requireUserByEmail("missing@test.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing@test.com");
        mockServer.verify();
    }

    @Test
    @DisplayName("requireUserById throws ResourceNotFoundException when user missing")
    void testRequireUserByIdNotFound() {
        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8081/api/users/999"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> userClient.requireUserById(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
        mockServer.verify();
    }
}
