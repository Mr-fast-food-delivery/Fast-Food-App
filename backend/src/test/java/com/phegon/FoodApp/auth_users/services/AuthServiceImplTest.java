package com.phegon.FoodApp.auth_users.services;

import com.phegon.FoodApp.auth_users.dtos.RegistrationRequest;
import com.phegon.FoodApp.auth_users.dtos.LoginRequest;
import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.auth_users.repository.UserRepository;
import com.phegon.FoodApp.exceptions.BadRequestException;
import com.phegon.FoodApp.exceptions.NotFoundException;
import com.phegon.FoodApp.role.entity.Role;
import com.phegon.FoodApp.role.repository.RoleRepository;
import com.phegon.FoodApp.security.JwtUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(AuthServiceImplTest.class);

    // Màu ANSI
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private RoleRepository roleRepository;

    @InjectMocks private AuthServiceImpl authService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        log.info(YELLOW + "---- Starting AuthService Test ----" + RESET);
    }

    // ====================================================================
    // REGISTER TESTS
    // ====================================================================

    @Test
    void testRegisterSuccess() {
        log.info("Running testRegisterSuccess");

        RegistrationRequest req = new RegistrationRequest();
        req.setName("Alice");
        req.setEmail("alice@example.com");
        req.setPassword("Secret123");
        req.setPhoneNumber("0909999999");
        req.setAddress("City");
        req.setRoles(List.of("CUSTOMER"));

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByName("CUSTOMER"))
                .thenReturn(Optional.of(new Role(1L, "CUSTOMER")));
        when(passwordEncoder.encode(any())).thenReturn("encodedPass");

        assertDoesNotThrow(() -> authService.register(req));

        log.info(GREEN + "✔ testRegisterSuccess passed" + RESET);
    }

    @Test
    void testRegisterEmailExists() {
        log.info("Running testRegisterEmailExists");

        RegistrationRequest req = new RegistrationRequest();
        req.setName("Bob");
        req.setEmail("bob@example.com");
        req.setPassword("Secret123");
        req.setPhoneNumber("0901234567");
        req.setAddress("City");
        req.setRoles(List.of("CUSTOMER"));

        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(req));

        log.info(GREEN + "✔ testRegisterEmailExists passed" + RESET);
    }

    @Test
    void testRegisterRoleNotFound() {
        log.info("Running testRegisterRoleNotFound");

        RegistrationRequest req = new RegistrationRequest();
        req.setName("Charlie");
        req.setEmail("charlie@example.com");
        req.setPassword("Secret123");
        req.setPhoneNumber("0902222333");
        req.setAddress("City");
        req.setRoles(List.of("ADMIN"));

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.register(req));

        log.info(GREEN + "✔ testRegisterRoleNotFound passed" + RESET);
    }

    // ====================================================================
    // LOGIN TESTS
    // ====================================================================

    @Test
    void testLoginSuccess() {
        log.info("Running testLoginSuccess");

        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("Secret123");

        User mockUser = new User();
        mockUser.setEmail("test@example.com");
        mockUser.setPassword("encoded");
        mockUser.setActive(true);
        mockUser.setRoles(List.of(new Role(1L, "CUSTOMER")));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("Secret123", "encoded")).thenReturn(true);
        when(jwtUtils.generateToken(anyString())).thenReturn("TOKEN123");

        var response = authService.login(req);

        assertEquals(200, response.getStatusCode());
        assertEquals("Login Successful", response.getMessage());
        assertEquals("TOKEN123", response.getData().getToken());

        log.info(GREEN + "✔ testLoginSuccess passed" + RESET);
    }

    @Test
    void testLoginWrongPassword() {
        log.info("Running testLoginWrongPassword");

        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("wrong");

        User mockUser = new User();
        mockUser.setEmail("test@example.com");
        mockUser.setPassword("encoded");
        mockUser.setActive(true);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.login(req));

        log.info(GREEN + "✔ testLoginWrongPassword passed" + RESET);
    }

    @Test
    void testLoginInactiveUser() {
        log.info("Running testLoginInactiveUser");

        LoginRequest req = new LoginRequest();
        req.setEmail("inactive@example.com");
        req.setPassword("Secret123");

        User mockUser = new User();
        mockUser.setActive(false);

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(mockUser));

        assertThrows(NotFoundException.class, () -> authService.login(req));

        log.info(GREEN + "✔ testLoginInactiveUser passed" + RESET);
    }
}
