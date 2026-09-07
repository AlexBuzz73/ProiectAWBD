package com.example.transactionservice.controllers;

import com.example.transactionservice.client.AccountClient;
import com.example.transactionservice.dto.AccountInternalSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/internal/transactions")
@RequiredArgsConstructor
public class InternalTransactionController {

    private final AccountClient accountClient;

    @GetMapping("/feign-test/account/{accountId}")
    public ResponseEntity<Map<String, Object>> testFeignToAccountService(@PathVariable Long accountId) {
        AccountInternalSummaryDTO account = accountClient.getAccount(accountId);
        Map<String, Object> map = new HashMap<>();
        map.put("account", account);
        map.put("callerService", "transaction-service");
        return ResponseEntity.ok(map);
    }

    @org.springframework.beans.factory.annotation.Value("${server.port:8083}")
    private int serverPort;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.example.transactionservice.client.AccountFeignClient accountFeignClient;

    @GetMapping("/instance-info")
    public ResponseEntity<Map<String, Object>> getInstanceInfo() {
        return ResponseEntity.ok(Map.of("service", "transaction-service", "port", serverPort));
    }

    @GetMapping("/feign-test/lb")
    public ResponseEntity<Map<String, Object>> testFeignLb() {
        if (accountFeignClient != null) {
            return ResponseEntity.ok(accountFeignClient.getInstanceInfo());
        }
        return ResponseEntity.ok(Map.of("status", "mock", "service", "transaction-service"));
    }
}
