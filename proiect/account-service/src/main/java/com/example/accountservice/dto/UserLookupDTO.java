package com.example.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserLookupDTO {
    private Integer userId;
    private String username;
    private String email;
    private String role;
    private boolean enabled;
}
