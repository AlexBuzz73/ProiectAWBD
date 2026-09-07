package com.example.userservice.mappers;

import com.example.userservice.domain.Individual;
import com.example.userservice.domain.User;
import com.example.userservice.dto.IndividualRegistrationDTO;
import com.example.userservice.dto.UserLookupDTO;
import com.example.userservice.dto.UserRegistrationDTO;
import com.example.userservice.dto.UserResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class UserMapper {

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

    public UserResponseDTO toUserResponseDTO(User user) {
        Individual ind = user.getIndividual();
        return new UserResponseDTO(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                ind != null ? ind.getFirstName() : null,
                ind != null ? ind.getLastName() : null,
                ind != null ? ind.getPhoneNumber() : null,
                ind != null ? ind.getDateOfBirth() : null
        );
    }

    public UserLookupDTO toUserLookupDTO(User user) {
        Individual ind = user.getIndividual();
        return new UserLookupDTO(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                ind != null ? ind.getFirstName() : null,
                ind != null ? ind.getLastName() : null
        );
    }
}
