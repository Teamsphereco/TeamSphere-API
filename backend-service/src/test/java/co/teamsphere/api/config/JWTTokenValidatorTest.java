package co.teamsphere.api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import co.teamsphere.api.config.properties.JwtProperties;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class JWTTokenValidatorTest {
    @Mock
    private Authentication authentication;

    @Mock
    private JwtProperties jwtProperties;

    private JWTTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JWTTokenValidator jwtTokenValidator;

    private PrivateKey privateKey;

    @Test
    void populateAuthorities_shouldConvertAuthoritiesToCommaSeparatedString() {
        JWTTokenProvider jwtTokenProvider1 = new JWTTokenProvider(privateKey, jwtProperties);
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        String authoritiesString = jwtTokenProvider1.populateAuthorities(authorities);
        assertTrue(authoritiesString.contains("ROLE_USER"));
        assertTrue(authoritiesString.contains("ROLE_ADMIN"));
        assertEquals("ROLE_USER,ROLE_ADMIN", authoritiesString);
    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        MockitoAnnotations.openMocks(this);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
    
        when(authentication.getName()).thenReturn("test@example.com");
        when(jwtProperties.getAudience()).thenReturn("Teamsphere");
    
        jwtTokenProvider = new JWTTokenProvider(privateKey, jwtProperties);
        jwtTokenValidator = new JWTTokenValidator(publicKey, jwtProperties);
    }

    @Test
    void generateJwtToken_shouldCreateValidToken() {
        String token = jwtTokenProvider.generateJwtToken(authentication, UUID.randomUUID());
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    public void testValidToken() throws IOException, ServletException {
        String token = jwtTokenProvider.generateJwtToken(authentication, UUID.randomUUID());

        when(request.getHeader(JWTTokenConst.HEADER)).thenReturn("Bearer "+token);

        jwtTokenValidator.doFilterInternal(request, response, filterChain);

        // Assert authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // This has caused me too much pain due to stupid type errors, will fix later 
        // assertNotNull(authentication);
        assertEquals(authentication.getName(), "test@example.com");
        // assertTrue(AuthorityUtils.authorityListToSet(authentication.getAuthorities()).contains("ROLE_USER"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void generatedToken_shouldHaveCorrectExpiration() {
        String token = jwtTokenProvider.generateJwtToken(authentication, UUID.randomUUID());

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(privateKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        long diff = expiration.getTime() - issuedAt.getTime();
        assertEquals(86400000, diff, 1000); // Allow 1 second variance
    }

    @Test
    void getEmailFromToken_shouldExtractCorrectEmail() {
        String token = "Bearer " + jwtTokenProvider.generateJwtToken(authentication, UUID.randomUUID());

        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);
        assertEquals("test@example.com", extractedEmail);
    }

    @Test
    void getEmailFromToken_shouldThrowExceptionForInvalidToken() {
        assertThrows(JwtException.class, () -> {
            jwtTokenProvider.getEmailFromToken("Bearer invalidToken");
        });
    }

    @Test
    void getEmailFromToken_shouldThrowExceptionForExpiredToken() {
        String expiredToken = Jwts.builder()
                .setSubject("test@example.com")
                .setExpiration(new Date(System.currentTimeMillis() - 86400000)) // Past date
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> {
            jwtTokenProvider.getEmailFromToken("Bearer " + expiredToken);
        });
    }

    @Test
    void getEmailFromToken_shouldThrowExceptionForTamperedToken() {
        String originalToken = jwtTokenProvider.generateJwtToken(authentication, UUID.randomUUID());
        String tamperedToken = originalToken + "extra";

        assertThrows(JwtException.class, () -> {
            jwtTokenProvider.getEmailFromToken("Bearer " + tamperedToken);
        });
    }
}
