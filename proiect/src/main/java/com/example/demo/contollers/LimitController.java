package com.example.demo.contollers;

import com.example.demo.dto.BankLimitRequestDTO;
import com.example.demo.dto.BankLimitResponseDTO;
import com.example.demo.dto.UserLimitRequestDTO;
import com.example.demo.dto.UserLimitResponseDTO;
import com.example.demo.services.LimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.cdi.Eager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LimitController {
    private final LimitService limitService;


    public LimitController(LimitService limitService) {
        this.limitService = limitService;
    }

    @GetMapping("/admin/bank-limits")
    public BankLimitResponseDTO getBankLimits() {
        return limitService.getBankLimits();
    }

//    @GetMapping("/test")
//    public String test() {
//        return "merge";
//    }

    @PutMapping("/admin/bank-limits")
    public BankLimitResponseDTO updateBankLimits(@RequestBody BankLimitRequestDTO bankLimitRequestDTO) {
        return limitService.updateBankLimits(bankLimitRequestDTO);
    }

    @GetMapping("/user/{userId}/limits")
    public UserLimitResponseDTO getUserLimits(@PathVariable Integer userId) {
        return limitService.getUserLimits(userId);
    }

    @PutMapping("/user/{userId}/limits")
    public UserLimitResponseDTO updateUserLimits(@RequestBody UserLimitRequestDTO userLimitRequestDTO, @PathVariable Integer userId) {
        return limitService.updateUserLimits(userId, userLimitRequestDTO);
    }
}
