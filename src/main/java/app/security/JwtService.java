package app.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class JwtService {
    private static final String DEFAULT_SECRET = "change-me-in-production";
    private static final long EXPIRATION_HOURS = 2L;

    private final Algorithm algorithm;

    private final JWTVerifier verifier;

    public JwtService(){
        String secret = Optional.ofNullable(System.getenv("JWT_SECRET")).filter(s -> !s.isBlank()).orElse(DEFAULT_SECRET);
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer("rateit").build();
    }

    public String generateToken(String userId){
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer("rateit")
                .withSubject(userId)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(EXPIRATION_HOURS, ChronoUnit.HOURS))
                .sign(algorithm);
    }

    public String verifyToken(String token) throws TokenVerificationException {
        try {
            DecodedJWT jwt = verifier.verify(token);
            return jwt.getSubject();
        } catch (JWTVerificationException ex) {
            throw new TokenVerificationException("Invalid or expired token");
        }
    }

    public static class TokenVerificationException extends Exception {
        public TokenVerificationException(String message) {
            super(message);
        }
    }

}