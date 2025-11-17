package dsn.webmail.util;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PasswordCipher {
    
    private final StringEncryptor stringEncryptor;

    @Autowired
    public PasswordCipher(@Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor) {
        this.stringEncryptor = stringEncryptor;
    }

    public String encrypt(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return null;
        }
        return stringEncryptor.encrypt(plainPassword);
    }

    public String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return null;
        }
        return stringEncryptor.decrypt(encryptedPassword);
    }
}