package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class SharedAccountRequest {
    private String alias;
    private String currency;
    private List<UserRoleDTO> users;
}