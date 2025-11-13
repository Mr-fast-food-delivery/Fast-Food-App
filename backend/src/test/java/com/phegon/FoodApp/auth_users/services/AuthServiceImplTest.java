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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private RoleRepository roleRepository;

    @InjectMocks private AuthServiceImpl authService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    // ===================== REGISTER INVALID PASSWORD =====================
    @Test
    void testRegisterInvalidPassword() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("Eve");
        req.setEmail("eve@example.com");
        req.setPassword("123");
        req.setPhoneNumber("0901234567");
        req.setAddress("City");
        req.setRoles(List.of("CUSTOMER"));

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(req));
    }

    // ===================== PHONE TOO SHORT =====================
    @Test
    void testRegisterInvalidPhoneTooShort() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("Grace");
        req.setEmail("grace@example.com");
        req.setPassword("Valid123!");
        req.setPhoneNumber("091234567");
        req.setAddress("City");
        req.setRoles(List.of("CUSTOMER"));

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(req));
    }

    // ===================== PHONE LETTERS =====================
    @Test
    void testRegisterInvalidPhoneIncludeLetters() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("Frank");
        req.setEmail("frank@example.com");
        req.setPassword("Valid123!");
        req.setPhoneNumber("InvalidPhone11");
        req.setAddress("City");
        req.setRoles(List.of("CUSTOMER"));

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(req));
    }

    // ===================== INVALID EMAIL =====================
    @Test
    void testRegisterInvalidEmailFormat() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("TestUser");
        req.setEmail("invalid-email");
        req.setPassword("Valid123!");
        req.setPhoneNumber("0901234567");
        req.setAddress("City");
        req.setRoles(List.of("CUSTOMER"));

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(req));
    }

    // ===================== DEFAULT ROLE WHEN EMPTY =====================
    @Test
    void testRegisterDefaultRole() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("David");
        req.setEmail("david@example.com");
        req.setPassword("Valid123!");
        req.setPhoneNumber("0906666666");
        req.setAddress("City");
        req.setRoles(List.of());

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("CUSTOMER"))
              .thenReturn(Optional.of(new Role(1L, "CUSTOMER")));
        when(passwordEncoder.encode(anyString()))
              .thenReturn("encodedPass");

        when(userRepository.save(any(User.class)))
              .thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> authService.register(req));
    }

    // ===================== MISSING EMAIL =====================
    @Test
    void testRegisterMissingEmail() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("NoEmail");
        req.setPassword("Valid123!");
        req.setPhoneNumber("0905555555");
        req.setAddress("City");
        req.setRoles(List.of("CUSTOMER"));

        assertThrows(BadRequestException.class, () -> authService.register(req));
    }

    // ===================== EMPTY BODY =====================
    @Test
    void testRegisterEmptyBody() {
        RegistrationRequest req = new RegistrationRequest();
        assertThrows(BadRequestException.class, () -> authService.register(req));
    }

    // ===================== MISSING NAME =====================
    @Test
    void testRegisterMissingName() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEmail("test@example.com");
        req.setPassword("Valid123!");
        req.setPhoneNumber("0905555555");
        req.setAddress("City");
        req.setRoles(List.of("CUSTOMER"));

        assertThrows(BadRequestException.class, () -> authService.register(req));
    }

    // ===================== MISSING PASSWORD =====================
    @Test
    void testRegisterMissingPassword() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEmail("test@example.com");
        req.setName("NoPass");
        req.setPhoneNumber("0905555555");
        req.setAddress("City");
        req.setRoles(List.of("CUSTOMER"));

        assertThrows(BadRequestException.class, () -> authService.register(req));
    }

    // ===================== MISSING PHONE =====================
    @Test
    void testRegisterMissingPhoneNumber() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEmail("test@example.com");
        req.setName("NoPhone");
        req.setPassword("Valid123!");
        req.setAddress("City");
        req.setRoles(List.of("CUSTOMER"));

        assertThrows(BadRequestException.class, () -> authService.register(req));
    }

    // ===================== LOGIN SUCCESS =====================
    @Test
    void testLoginSuccess() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("Secret123");

        User user = new User();
        user.setActive(true);
        user.setEmail("test@example.com");
        user.setPassword("encodedPass");
        user.setRoles(List.of(new Role(1L, "CUSTOMER")));

        when(userRepository.findByEmail("test@example.com"))
              .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123", "encodedPass"))
              .thenReturn(true);
        when(jwtUtils.generateToken(anyString()))
              .thenReturn("TOKEN123");

        var response = authService.login(req);

        assertEquals(200, response.getStatusCode());
        assertEquals("TOKEN123", response.getData().getToken());
    }

    // ===================== LOGIN WRONG PASSWORD =====================
    @Test
    void testLoginWrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("wrong");

        User user = new User();
        user.setActive(true);
        user.setEmail("test@example.com");
        user.setPassword("encodedPass");

        when(userRepository.findByEmail("test@example.com"))
              .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encodedPass"))
              .thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.login(req));
    }

    // ===================== LOGIN INACTIVE USER =====================
    @Test
    void testLoginInactiveUser() {
        LoginRequest req = new LoginRequest();
        req.setEmail("inactive@example.com");
        req.setPassword("Secret123");

        User user = new User();
        user.setActive(false);

        when(userRepository.findByEmail("inactive@example.com"))
              .thenReturn(Optional.of(user));

        assertThrows(NotFoundException.class, () -> authService.login(req));
    }

    // ===================== LOGIN MISS EMAIL =====================
    @Test
    void testLoginMissingEmail() {
        LoginRequest req = new LoginRequest();
        req.setPassword("Secret123");

        assertThrows(BadRequestException.class, () -> authService.login(req));
    }

    // ===================== LOGIN MISS PASSWORD =====================
    @Test
    void testLoginMissingPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("MISSPASS@example.com");


        assertThrows(BadRequestException.class, () -> authService.login(req));
    }

    // ===================== LOGIN MISS PASSWORD =====================
    @Test
    void testLoginEmpty() {
        LoginRequest req = new LoginRequest();

        assertThrows(BadRequestException.class, () -> authService.login(req));
    }
}
