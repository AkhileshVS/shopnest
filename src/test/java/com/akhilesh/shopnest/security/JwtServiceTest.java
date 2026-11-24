package com.akhilesh.shopnest.security;

import com.akhilesh.shopnest.models.Role;
import com.akhilesh.shopnest.models.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "c2hvcG5lc3Qtc2VjcmV0LWtleS1jaGFuZ2UtdGhpcy1pbi1wcm9kdWN0aW9uLTEyMzQ1Ng==";

    @Test
    void generatedTokenIsValidAndHasEmail() {
        JwtService jwtService = new JwtService(SECRET, 60000);
        User user = new User();
        user.setId(1L);
        user.setEmail("akhil@example.com");
        user.setRole(Role.CUSTOMER);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("akhil@example.com");
    }

    @Test
    void tamperedTokenIsInvalid() {
        JwtService jwtService = new JwtService(SECRET, 60000);
        assertThat(jwtService.isValid("abc.def.ghi")).isFalse();
    }
}
