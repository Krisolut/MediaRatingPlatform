package app.security;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptPasswordHasher implements PasswordHasher {
    @Override
    public String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    @Override
    public boolean matches(String plainPassword, String hash) {
        return BCrypt.checkpw(plainPassword, hash);
    }
}
