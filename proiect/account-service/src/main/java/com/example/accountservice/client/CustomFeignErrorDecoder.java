package com.example.accountservice.client;

import com.example.accountservice.exceptions.ResourceNotFoundException;
import com.example.accountservice.exceptions.ServiceUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
public class CustomFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.warn("Feign error calling method {}: HTTP status {}", methodKey, response.status());
        if (response.status() == 404) {
            return new ResourceNotFoundException("Resursa cerută nu a fost găsită în serviciul apelat (" + methodKey + ").");
        }
        if (response.status() == 403) {
            return new AccessDeniedException("Acces interzis pe resursa serviciului apelat (" + methodKey + ").");
        }
        if (response.status() == 400) {
            return new IllegalArgumentException("Parametri invalizi transmiși către serviciul apelat (" + methodKey + ").");
        }
        if (response.status() == 503 || response.status() == 504) {
            return new ServiceUnavailableException("Serviciul apelat este temporar indisponibil (" + methodKey + ").");
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
