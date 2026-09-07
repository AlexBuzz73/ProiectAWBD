package com.example.accountservice.client;

import com.example.accountservice.dto.UserLookupDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", configuration = FeignClientConfig.class)
public interface UserFeignClient {

    @GetMapping("/api/users/{id}")
    UserLookupDTO getUserById(@PathVariable("id") Integer id);

    @GetMapping("/api/users/by-email")
    UserLookupDTO getUserByEmail(@RequestParam("email") String email);

    @GetMapping("/api/internal/users/instance-info")
    java.util.Map<String, Object> getInstanceInfo();
}
