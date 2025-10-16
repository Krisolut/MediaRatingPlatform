package app.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class JwtService {
    private static final String JWT_SECRET = "IHaveNoIdeaWhatIAmDoingHere123!"; // Das Secret sollte nicht im Code stehen oder?
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(JWT_SECRET);
    private static final long EXPIRATION_HOURS = 2L;    // java.time arbeitet mit long

    private final JWTVerifier verifier;

    public JwtService(){ this.verifier = JWT.require(ALGORITHM).build(); }

    public String generateToken(String userId){
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(userId)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(EXPIRATION_HOURS, ChronoUnit.HOURS))
                .sign(ALGORITHM);
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
