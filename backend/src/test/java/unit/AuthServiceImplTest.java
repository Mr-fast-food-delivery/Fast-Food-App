package unit;

import com.phegon.FoodApp.auth_users.dtos.RegistrationRequest;
import com.phegon.FoodApp.auth_users.dtos.LoginRequest;
import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.auth_users.repository.UserRepository;
import com.phegon.FoodApp.auth_users.services.AuthServiceImpl;
import com.phegon.FoodApp.exceptions.BadRequestException;
import com.phegon.FoodApp.exceptions.NotFoundException;
import com.phegon.FoodApp.role.entity.Role;
import com.phegon.FoodApp.role.repository.RoleRepository;
import com.phegon.FoodApp.security.JwtUtils;
import com.phegon.FoodApp.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    // ===================== REGISTER  =====================
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

    @Test
    void testRegisterInvalidPhoneNotNumeric() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("Frank");
        req.setEmail("frank@example.com");
        req.setPassword("Valid123!");
        req.setPhoneNumber("#123456789");
        req.setAddress("City");
        req.setRoles(List.of("CUSTOMER"));

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(req));
    }

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

    @Test
    void testRegisterEmptyBody() {
        RegistrationRequest req = new RegistrationRequest();
        assertThrows(BadRequestException.class, () -> authService.register(req));
    }

    @Test
    void testRegister_EmailHasWhitespace() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("User");
        req.setEmail("test @gmail.com"); // <= invalid
        req.setPassword("Password123!");
        req.setPhoneNumber("0901234567");
        req.setAddress("City");
        req.setRoles(List.of("CUSTOMER"));

        assertThrows(BadRequestException.class,
                () -> authService.register(req));
    }

    @Test
    void testRegister_MultipleRoles_Success() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("MultiRole");
        req.setEmail("multi@example.com");
        req.setPassword("Password123!");
        req.setPhoneNumber("0902222333");
        req.setAddress("City");
        req.setRoles(List.of("ADMIN", "CUSTOMER"));

        when(userRepository.existsByEmail("multi@example.com"))
                .thenReturn(false);

        // FIXED: Role(Long, String)
        Role adminRole = new Role(1L, "ADMIN");
        Role customerRole = new Role(2L, "CUSTOMER");

        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.of(adminRole));

        when(roleRepository.findByName("CUSTOMER"))
                .thenReturn(Optional.of(customerRole));

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded");

        Response<?> res = authService.register(req);

        assertEquals(200, res.getStatusCode());
    }

    @Test
    void testRegister_MultipleRoles_OneInvalid() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("MultiRole");
        req.setEmail("multi2@example.com");
        req.setPassword("Password123!");
        req.setPhoneNumber("0902222333");
        req.setAddress("City");
        req.setRoles(List.of("ADMIN", "INVALID_ROLE"));

        when(userRepository.existsByEmail("multi2@example.com"))
                .thenReturn(false);

        Role adminRole = new Role(1L, "ADMIN");

        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.of(adminRole));

        when(roleRepository.findByName("INVALID_ROLE"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> authService.register(req));
    }

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

    // ===================== LOGIN =====================

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

    @Test
    void testLoginMissingEmail() {
        LoginRequest req = new LoginRequest();
        req.setPassword("Secret123");

        assertThrows(BadRequestException.class, () -> authService.login(req));
    }

    @Test
    void testLoginMissingPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("MISSPASS@example.com");


        assertThrows(BadRequestException.class, () -> authService.login(req));
    }

    @Test
    void testLoginEmpty() {
        LoginRequest req = new LoginRequest();

        assertThrows(BadRequestException.class, () -> authService.login(req));
    }

    @Test
    void testLoginEmailNotFound() {
        LoginRequest req = new LoginRequest();
        req.setPassword("Secret123");

        req.setEmail("notfound@example.com");
        when(userRepository.findByEmail("notfound@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> authService.login(req));
    }

    @Test
    void testLoginInvalidEmailFormat() {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setEmail("invalid-email");
        req.setPassword("Secret123");

        // Assert
        assertThrows(BadRequestException.class,
                () -> authService.login(req));
    }

}
