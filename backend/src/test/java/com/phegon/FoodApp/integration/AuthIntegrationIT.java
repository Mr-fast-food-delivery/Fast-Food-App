package com.phegon.FoodApp.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phegon.FoodApp.FoodAppApplication;
import com.phegon.FoodApp.auth_users.dtos.LoginRequest;
import com.phegon.FoodApp.auth_users.dtos.RegistrationRequest;
import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.auth_users.repository.UserRepository;
import com.phegon.FoodApp.config.FakeS3Config;
import com.phegon.FoodApp.role.entity.Role;
import com.phegon.FoodApp.role.repository.RoleRepository;
import com.phegon.FoodApp.response.Response;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest(
        classes = FoodAppApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import(FakeS3Config.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthIntegrationIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String url(String path) {
        return "http://localhost:" + port + "/api/auth" + path;
    }

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.save(new Role(null, "CUSTOMER"));
        roleRepository.save(new Role(null, "ADMIN"));
    }

    // ============================================================
    // INT001 – REGISTER
    // ============================================================

    @Test @Order(1)
    void INT001a_register_success() {
        // Arrange
        RegistrationRequest req = baseValidRegistration();

        // Act
        ResponseEntity<String> res = rest.postForEntity(url("/register"), req, String.class);

        // Assert
        Assertions.assertEquals(HttpStatus.OK, res.getStatusCode());

        Response<?> body = parse(res.getBody());
        Assertions.assertEquals(200, body.getStatusCode());
        Assertions.assertEquals("User Registered Successfully", body.getMessage());

        // Verify DB
        User u = userRepository.findByEmail(req.getEmail()).orElseThrow();
        Assertions.assertEquals(req.getName(), u.getName());
        Assertions.assertTrue(passwordEncoder.matches(req.getPassword(), u.getPassword()));
        Assertions.assertTrue(u.isActive());
    }

    @Test @Order(2)
    void INT001b_register_email_exists() {
        // Arrange
        createUser("exists@gmail.com", "123456");

        RegistrationRequest req = baseValidRegistration();
        req.setEmail("exists@gmail.com");

        // Act
        ResponseEntity<String> res = rest.postForEntity(url("/register"), req, String.class);

        // Assert
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test @Order(3)
    void INT001c_register_invalid_email() {
        RegistrationRequest req = baseValidRegistration();
        req.setEmail("abc@");

        ResponseEntity<String> res = rest.postForEntity(url("/register"), req, String.class);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test @Order(4)
    void INT001d_register_short_password() {
        RegistrationRequest req = baseValidRegistration();
        req.setPassword("123");

        ResponseEntity<String> res = rest.postForEntity(url("/register"), req, String.class);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test @Order(5)
    void INT001e_register_missing_phone() {
        RegistrationRequest req = baseValidRegistration();
        req.setPhoneNumber(null);

        ResponseEntity<String> res = rest.postForEntity(url("/register"), req, String.class);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test @Order(6)
    void INT001f_register_invalid_phone_digits() {
        RegistrationRequest req = baseValidRegistration();
        req.setPhoneNumber("12345");

        ResponseEntity<String> res = rest.postForEntity(url("/register"), req, String.class);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test @Order(7)
    void INT001g_register_role_not_found() {
        RegistrationRequest req = baseValidRegistration();
        req.setRoles(List.of("UNKNOWN"));

        ResponseEntity<String> res = rest.postForEntity(url("/register"), req, String.class);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }


    // ============================================================
    // INT002 – LOGIN TEST
    // ============================================================

    @Test @Order(8)
    void INT002a_login_success() {
        // Arrange – tạo user đúng
        createUser("abc@gmail.com", "123456");

        LoginRequest req = new LoginRequest();
        req.setEmail("abc@gmail.com");
        req.setPassword("123456");

        // Act
        ResponseEntity<String> res = rest.postForEntity(url("/login"), req, String.class);

        // Assert
        Assertions.assertEquals(HttpStatus.OK, res.getStatusCode());

        Response<?> body = parse(res.getBody());
        Assertions.assertEquals("Login Successful", body.getMessage());
        Assertions.assertNotNull(body.getData()); // body.data chứa token chẳng hạn

        // Nếu login trả JWT → kiểm JWT không rỗng
        String jwt = body.getData().toString();
        Assertions.assertTrue(jwt.length() > 20);
    }

    @Test @Order(9)
    void INT002b_login_email_not_exist() {
        LoginRequest req = new LoginRequest();
        req.setEmail("notfound@gmail.com");
        req.setPassword("123456");

        ResponseEntity<String> res = rest.postForEntity(url("/login"), req, String.class);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test @Order(10)
    void INT002c_login_wrong_password() {
        createUser("login@gmail.com", "123456");

        LoginRequest req = new LoginRequest();
        req.setEmail("login@gmail.com");
        req.setPassword("wrongpass");

        ResponseEntity<String> res = rest.postForEntity(url("/login"), req, String.class);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test @Order(11)
    void INT002d_login_invalid_email_format() {
        LoginRequest req = new LoginRequest();
        req.setEmail("abc@");
        req.setPassword("123456");

        ResponseEntity<String> res = rest.postForEntity(url("/login"), req, String.class);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test @Order(12)
    void INT002e_login_inactive_account() {
        User u = createUser("inactive@gmail.com", "123456");
        u.setActive(false);
        userRepository.save(u);

        LoginRequest req = new LoginRequest();
        req.setEmail("inactive@gmail.com");
        req.setPassword("123456");

        ResponseEntity<String> res = rest.postForEntity(url("/login"), req, String.class);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }


    // ============================================================
    // HELPERS
    // ============================================================

    private RegistrationRequest baseValidRegistration() {
        RegistrationRequest r = new RegistrationRequest();
        r.setName("Ty");
        r.setEmail("valid@gmail.com");
        r.setPassword("123456");
        r.setAddress("HCM");
        r.setPhoneNumber("0123456789");
        return r;
    }

    private User createUser(String email, String rawPassword) {
        Role customer = roleRepository.findByName("CUSTOMER").orElseThrow();

        User u = User.builder()
                .name("User Test")
                .email(email)
                .password(passwordEncoder.encode(rawPassword))   // ✔ Encode đúng chuẩn
                .phoneNumber("0123456789")
                .address("HCM")
                .isActive(true)
                .roles(List.of(customer))
                .build();
        return userRepository.save(u);
    }

    private Response<?> parse(String json) {
        try {
            return mapper.readValue(json, new TypeReference<Response<?>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
