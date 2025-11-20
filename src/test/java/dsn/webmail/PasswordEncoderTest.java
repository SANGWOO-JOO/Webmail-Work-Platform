package dsn.webmail;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderTest {

    @Test
    public void generatePasswordHash() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "test1234";
        String encodedPassword = encoder.encode(rawPassword);

        System.out.println("Raw Password: " + rawPassword);
        System.out.println("Encoded Password: " + encodedPassword);
        System.out.println("Match test: " + encoder.matches(rawPassword, encodedPassword));
    }
}
