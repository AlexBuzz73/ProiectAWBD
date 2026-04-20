package com.example.demo.mappers;

import com.example.demo.domain.Individual;
import com.example.demo.domain.User;
import com.example.demo.dto.IndividualRegistrationDTO;
import com.example.demo.dto.LoginResponseDTO;
import com.example.demo.dto.UserRegistrationDTO;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class AuthMapper {

    public Individual toIndividual(IndividualRegistrationDTO dto) {
        Individual individual = new Individual();
        individual.setFirstName(dto.getFirstName());
        individual.setLastName(dto.getLastName());
        individual.setCnp(dto.getCnp());
        individual.setPhoneNumber(dto.getPhoneNumber());
        individual.setDateOfBirth(dto.getDateOfBirth());
        individual.setStatus("ACTIVE");
        individual.setCreatedAt(new Date());
        individual.setUpdatedAt(new Date());

        return individual;
    }

    public User toUser(UserRegistrationDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());

        return user;
    }

    public LoginResponseDTO toLoginResponseDTO(User user) {
        return new LoginResponseDTO(user.getUserId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
