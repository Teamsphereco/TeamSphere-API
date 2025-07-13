package co.teamsphere.api.config;

import java.security.PrivateKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import co.teamsphere.api.config.properties.JwtProperties;

@Service
@Slf4j
public class JWTTokenProvider {
    private final PrivateKey privateKey;
    private final JwtProperties jwtProperties;

    public JWTTokenProvider(PrivateKey privateKey, JwtProperties jwtProperties) {
        this.privateKey = privateKey;
        this.jwtProperties = jwtProperties;

    }

    public String generateJwtToken(Authentication authentication, UUID userId) {
        log.info("Generating JWT...");
        var currentDate = new Date();

        String authoritiesString = populateAuthorities(authentication.getAuthorities());

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setIssuer("Teamsphere.co")
                .setSubject(userId.toString())
                .setAudience(jwtProperties.getAudience())
                .setIssuedAt(currentDate)
                .setNotBefore(currentDate)
                .setExpiration(new Date(currentDate.getTime()+86400000))
                .claim("email", authentication.getName())
                .claim("authorities", authoritiesString)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String generateJwtTokenFromEmail(String email, UUID userId) {
        log.info("Generating JWT...");
        var currentDate = new Date();

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setIssuer("Teamsphere.co")
                .setSubject(userId.toString())
                .setAudience(jwtProperties.getAudience())
                .setIssuedAt(currentDate)
                .setNotBefore(currentDate)
                .setExpiration(new Date(currentDate.getTime()+86400000))
                .claim("email", email)
                .claim("authorities", "ROLE_USER")
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseTokenForClaims(token);
        return String.valueOf(claims.get("email"));
    }

    public UUID getIdFromToken(String token) {
        Claims claims = parseTokenForClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public String populateAuthorities(Collection<? extends GrantedAuthority> collection) {
        var authoritiesSet = new HashSet<String>();
        for(GrantedAuthority authority:collection) {
            authoritiesSet.add(authority.getAuthority());
        }
        return String.join(",", authoritiesSet);
    }

    private Claims parseTokenForClaims(String token) {
        log.info("Parsing claims for token...");

        if (token == null || !token.startsWith("Bearer ")) {
            log.error("Invalid token format: missing 'Bearer ' prefix or token is null");
            throw new IllegalArgumentException("Invalid token format");
        }

        String actualToken = token.substring(7);

        try {
            return Jwts.parserBuilder()
                .setSigningKey(privateKey)
                .build()
                .parseClaimsJws(actualToken)
                .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            throw e; // Re-throw the specific ExpiredJwtException
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw e; // Re-throw the specific MalformedJwtException
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw e; // Re-throw the specific SignatureException
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw e; // Re-throw the specific UnsupportedJwtException
        } catch (Exception e) {
            log.error("Unexpected error parsing JWT token: {}", e.getMessage());
            throw new RuntimeException("Error parsing JWT token", e); // Catch any other unexpected errors
        }
    }

}
