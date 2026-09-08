package com.example.accountservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLookupDTO {
    private Integer userId;
    private String username;
    private String email;
    private String role;
    private boolean enabled;
    private String status;
    private String firstName;
    private String lastName;

    public UserLookupDTO(Integer userId, String username, String email, String role, boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
    }
}
