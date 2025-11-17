package dsn.webmail.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class VerificationCodeGenerator {
    private final SecureRandom random = new SecureRandom();
    
    public String sixDigits() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}