package co.teamsphere.api.config;

import io.jsonwebtoken.Claims;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JWTTokenProviderTest {
    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Mock
    private JwtProperties jwtProperties;

    private JWTTokenValidator tokenValidator;
    private JWTTokenProvider tokenProvider;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        MockitoAnnotations.openMocks(this);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

        when(jwtProperties.getAudience()).thenReturn("Teamsphere");
        tokenValidator = new JWTTokenValidator(publicKey, jwtProperties);
        tokenProvider = new JWTTokenProvider(privateKey, jwtProperties);
        SecurityContextHolder.clearContext();
    }

    private String generateValidToken(String audience, String subject, String authorities) {
        return Jwts.builder()
                .setSubject(subject)
                .setAudience(audience)
                .claim("authorities", authorities)
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    @Test
    void doFilterInternal_validToken_shouldSetAuthentication() throws ServletException, IOException {
        // Arrange
        String validToken = generateValidToken("Teamsphere", "testuser", "ROLE_USER");
        when(request.getHeader(JWTTokenConst.HEADER)).thenReturn("Bearer " + validToken);

        // Act
        tokenValidator.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("testuser", auth.getName());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_invalidAudience_shouldSendUnauthorizedError() throws ServletException, IOException {
        // Arrange
        String invalidToken = generateValidToken("WrongAudience", "testuser", "ROLE_USER");
        when(request.getHeader(JWTTokenConst.HEADER)).thenReturn("Bearer " + invalidToken);

        // Act
        tokenValidator.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_expiredToken_shouldSendUnauthorizedError() throws ServletException, IOException {
        // Arrange
        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .setAudience("Teamsphere")
                .setExpiration(new Date(System.currentTimeMillis() - 86400000))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
        when(request.getHeader(JWTTokenConst.HEADER)).thenReturn("Bearer " + expiredToken);

        // Act
        tokenValidator.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT token is expired");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_missingToken_shouldContinueFilterChain() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(JWTTokenConst.HEADER)).thenReturn(null);

        // Act
        tokenValidator.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_malformedToken_shouldSendUnauthorizedError() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(JWTTokenConst.HEADER)).thenReturn("Bearer malformedToken");

        // Act
        tokenValidator.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    
    @Test
    void generateJwtToken_createsValidToken() {
        // Arrange
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        Authentication authentication = new UsernamePasswordAuthenticationToken("test@example.com", null, authorities);
        
        // Act
        String token = tokenProvider.generateJwtToken(authentication);
        
        // Assert
        assertNotNull(token);
        
        // Parse the token to verify contents
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(privateKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        assertEquals("test@example.com", claims.getSubject());
        assertEquals("Teamsphere", claims.getAudience());
        assertEquals("test@example.com", claims.get("email"));
        assertEquals("ROLE_USER", claims.get("authorities"));
        assertEquals("Teamsphere.co", claims.getIssuer());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }
    
    @Test
    void getEmailFromToken_extractsEmailFromToken() {
        // Arrange
        String email = "user@example.com";
        String token = Jwts.builder()
                .setSubject(email)
                .claim("email", email)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
        
        // Act
        String extractedEmail = tokenProvider.getEmailFromToken("Bearer " + token);
        
        // Assert
        assertEquals(email, extractedEmail);
    }
    
    @Test
    void populateAuthorities_joinsAuthoritiesWithComma() {
        // Arrange
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        
        // Act
        String result = tokenProvider.populateAuthorities(authorities);
        
        // Assert
        assertTrue(result.contains("ROLE_USER"));
        assertTrue(result.contains("ROLE_ADMIN"));
        assertTrue(result.contains(","));
    }
}