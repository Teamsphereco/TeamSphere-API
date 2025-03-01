package co.teamsphere.api.config;

import java.security.PrivateKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JWTTokenProvider {
    private final PrivateKey privateKey;
    private final JwtProperties jwtProperties;

    public JWTTokenProvider(PrivateKey privateKey, JwtProperties jwtProperties) {
        this.privateKey = privateKey;
        this.jwtProperties = jwtProperties;

    }

    public String generateJwtToken(Authentication authentication) {
        log.info("Generating JWT...");
        var currentDate = new Date();

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setIssuer("Teamsphere.co")
                .setSubject(authentication.getName())
                .setAudience(jwtProperties.getAudience())
                .setIssuedAt(currentDate)
                .setNotBefore(currentDate)
                .setExpiration(new Date(currentDate.getTime()+86400000))
                .claim("email", authentication.getName())
                .claim("authorities", "ROLE_USER")
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String generateJwtTokenFromEmail(String email) {
        log.info("Generating JWT...");
        var currentDate = new Date();

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setIssuer("Teamsphere.co")
                .setSubject(email)
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
        log.info("parsing claims ----------- ");

        token = token.substring(7);

        Claims claims= Jwts.parserBuilder()
                .setSigningKey(privateKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return String.valueOf(claims.get("email"));
    }

    public String populateAuthorities(Collection<? extends GrantedAuthority> collection) {
        var authoritieSet = new HashSet<String>();
        for(GrantedAuthority authority:collection) {
            authoritieSet.add(authority.getAuthority());
        }
        return String.join(",", authoritieSet);
    }
}
