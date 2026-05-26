package com.sampada.metavault.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtService handles everything related to JWT tokens:
 * - Generating a token when a user logs in
 * - Extracting the username from a token
 * - Validating that a token is genuine and not expired
 *
 * HOW JWT WORKS (simplified):
 * A JWT token has 3 parts separated by dots: header.payload.signature
 *
 * header:    {"alg":"HS256","typ":"JWT"}  → base64 encoded
 * payload:   {"sub":"john","iat":...,"exp":...}  → base64 encoded (NOT encrypted!)
 * signature: HMACSHA256(header + "." + payload, secretKey)
 *
 * The signature is the security mechanism. Anyone can read the payload,
 * but only someone with the SECRET KEY can create a valid signature.
 * When we receive a token, we re-compute the signature and check it matches.
 * If it matches → we generated this token → it's genuine.
 * If not → someone tampered with it → reject.
 *
 * @Service: marks this as a Spring-managed singleton bean.
 *           Spring creates ONE instance and injects it wherever needed.
 */
@Service
public class JwtService {

    /**
     * @Value reads from application.yml: jwt.secret
     * This is our HMAC-SHA256 signing key (base64-encoded).
     * Must be at least 256 bits (32 bytes) for HMAC-SHA256.
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * @Value reads: jwt.expiration (86400000 = 24 hours in milliseconds)
     */
    @Value("${jwt.expiration}")
    private long expiration;

    // ── Token Generation ──────────────────────────────────────────────────────

    /**
     * Generate a JWT token for a user.
     * Called after successful login or registration.
     *
     * @param userDetails — Spring Security's user object (our User entity implements this)
     * @return signed JWT string like "eyJhbGc..."
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generate a token with extra claims (key-value pairs in the payload).
     * Extra claims could be: role, email, userId etc.
     * We keep it simple — just the subject (username) for now.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                // subject = the unique identifier for this user (username)
                .subject(userDetails.getUsername())
                // issuedAt = when the token was created
                .issuedAt(new Date(System.currentTimeMillis()))
                // expiration = when the token becomes invalid (now + 24h)
                .expiration(new Date(System.currentTimeMillis() + expiration))
                // signWith = sign with our secret key using HMAC-SHA256
                .signWith(getSigningKey())
                .compact(); // serialize to the final "xxx.yyy.zzz" string
    }

    // ── Token Validation ──────────────────────────────────────────────────────

    /**
     * Check if a token is valid for a given user.
     * Valid = username matches AND token is not expired.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ── Claims Extraction ─────────────────────────────────────────────────────

    /**
     * Extract the username (subject) from a token.
     * This is the main method the JwtAuthFilter calls.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor — takes a function that reads any field from Claims.
     * Claims::getSubject → reads the "sub" field
     * Claims::getExpiration → reads the "exp" field
     *
     * This pattern (Function<Claims, T>) avoids writing a separate method
     * for each field we want to read.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse the token and return ALL claims from the payload.
     * This is where signature verification happens:
     * - Jwts.parser() with our signing key will THROW an exception
     *   if the signature is invalid or the token is expired.
     * - If it returns without throwing, the token is genuine.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())   // set the key to verify against
                .build()
                .parseSignedClaims(token)      // verify + parse (throws if invalid)
                .getPayload();                 // returns the claims map
    }

    /**
     * Convert our base64-encoded secret string into a SecretKey object
     * that JJWT can use for HMAC-SHA256 signing/verification.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
