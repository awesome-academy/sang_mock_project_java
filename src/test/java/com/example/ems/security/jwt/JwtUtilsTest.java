
package com.example.ems.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.ems.security.jwt.JwtUtils;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    @InjectMocks
    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    private String testSecret;
    private final int TEST_EXPIRATION_MS = 60000;
    
    @BeforeEach
    void setUp() {
        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512); 
        testSecret = java.util.Base64.getEncoder().encodeToString(key.getEncoded());
        
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", TEST_EXPIRATION_MS);
    }

    @Test
    @DisplayName("Generate Token: Should generate a valid non-empty string")
    void generateJwtToken_Success() {
        // GIVEN
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@email.com");

        // WHEN
        String token = jwtUtils.generateJwtToken(authentication);

        // THEN
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtUtils.validateJwtToken(token));
    }

    @Test
    @DisplayName("Get Username: Should extract correct username from token")
    void getUserNameFromJwtToken_Success() {
        // GIVEN
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("user_check");
        String token = jwtUtils.generateJwtToken(authentication);

        // WHEN
        String extractedUsername = jwtUtils.getUserNameFromJwtToken(token);

        // THEN
        assertEquals("user_check", extractedUsername);
    }

    @Test
    @DisplayName("Validate: Should return true for valid token")
    void validateJwtToken_Valid() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("valid_user");
        String token = jwtUtils.generateJwtToken(authentication);

        assertTrue(jwtUtils.validateJwtToken(token));
    }

    @Test
    @DisplayName("Validate: Should return false for Invalid Signature (Wrong Secret)")
    void validateJwtToken_InvalidSignature() {
        String otherSecret = "E8/k6F2j+9q5J8w0z3X1vY7u4N8m2L5k9Q6r3T8w1Y4u7I0o2P5s8A1d4F7g0H3j6K9l2Z5x8C1v4B7n0M3q6X==";
        Key otherKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(otherSecret));
        
        String invalidToken = Jwts.builder()
                .setSubject("hacker")
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + TEST_EXPIRATION_MS))
                .signWith(otherKey, SignatureAlgorithm.HS512)
                .compact();

        // WHEN & THEN
        assertFalse(jwtUtils.validateJwtToken(invalidToken));
    }

    @Test
    @DisplayName("Validate: Should return false for Malformed Token")
    void validateJwtToken_Malformed() {
        String malformedToken = "eyJdkjahskdjh.invalid.token";
        assertFalse(jwtUtils.validateJwtToken(malformedToken));
    }

    @Test
    @DisplayName("Validate: Should return false for Expired Token")
    void validateJwtToken_Expired() {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(testSecret));
        
        String expiredToken = Jwts.builder()
                .setSubject("expired_user")
                .setIssuedAt(new Date(System.currentTimeMillis() - 10000)) 
                .setExpiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        // WHEN & THEN
        assertFalse(jwtUtils.validateJwtToken(expiredToken));
    }

    @Test
    @DisplayName("Validate: Should return false for Empty or Null Token")
    void validateJwtToken_Empty() {
        assertFalse(jwtUtils.validateJwtToken(""));
        assertFalse(jwtUtils.validateJwtToken(null));
    }
}
