package com.phegon.FoodApp.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phegon.FoodApp.FoodAppApplication;
import com.phegon.FoodApp.auth_users.dtos.UserDTO;
import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.auth_users.repository.UserRepository;
import com.phegon.FoodApp.response.Response;
import com.phegon.FoodApp.role.entity.Role;
import com.phegon.FoodApp.role.repository.RoleRepository;
import com.phegon.FoodApp.security.JwtUtils;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

@SpringBootTest(
        classes = FoodAppApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserIntegrationIT {

    @LocalServerPort
    private int port;

    @Autowired private TestRestTemplate rest;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private ObjectMapper mapper;
    @Autowired private PasswordEncoder passwordEncoder;

    private String url(String path) {
        return "http://localhost:" + port + "/api/users" + path;
    }

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.save(new Role(null, "CUSTOMER"));
        roleRepository.save(new Role(null, "ADMIN"));
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    private User createUser(String email) {
        Role r = roleRepository.findByName("CUSTOMER").orElseThrow();

        User u = User.builder()
                .name("User")
                .email(email)
                .password(passwordEncoder.encode("123456"))  // ✔ FIX: mã hóa thật
                .isActive(true)
                .phoneNumber("0123456789")
                .address("HCM")
                .roles(List.of(r))
                .build();

        return userRepository.save(u);
    }

    private User createAdmin(String email) {
        Role r = roleRepository.findByName("ADMIN").orElseThrow();

        User u = User.builder()
                .name("Admin")
                .email(email)
                .password(passwordEncoder.encode("123456"))
                .isActive(true)
                .phoneNumber("0123456789")
                .address("HCM")
                .roles(List.of(r))
                .build();

        return userRepository.save(u);
    }

    private HttpHeaders authHeader(String token) {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + token);
        return h;
    }

    /** ✔ FIX: Token expired thật */
    private String generateExpiredToken(String email) {
        return jwtUtils.generateExpiredToken(email);
    }

    private Response<?> parse(String json) {
        try {
            return mapper.readValue(json, new TypeReference<Response<?>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ============================================================
    // INT003 – GET CURRENT ACCOUNT
    // ============================================================

    @Test @Order(1)
    void INT003a_getCurrent_success() {
        User u = createUser("u1@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        ResponseEntity<String> res =
                rest.exchange(url("/account"), HttpMethod.GET,
                        new HttpEntity<>(authHeader(token)), String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());

        Response<?> body = parse(res.getBody());
        Assertions.assertEquals(200, body.getStatusCode());
        Assertions.assertEquals(u.getEmail(), ((Map) body.getData()).get("email"));
    }

    @Test @Order(2)
    void INT003b_missingToken() {
        ResponseEntity<String> res = rest.getForEntity(url("/account"), String.class);
        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(3)
    void INT003c_invalidToken() {
        ResponseEntity<String> res = rest.exchange(
                url("/account"), HttpMethod.GET,
                new HttpEntity<>(authHeader("fake.token.here")), String.class);

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(4)
    void INT003d_userDeleted() {
        User u = createUser("del@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        userRepository.delete(u);

        ResponseEntity<String> res =
                rest.exchange(url("/account"), HttpMethod.GET,
                        new HttpEntity<>(authHeader(token)), String.class);

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(5)
    void INT003e_inactiveUser() {
        User u = createUser("inactive@gmail.com");
        u.setActive(false);
        userRepository.save(u);

        ResponseEntity<String> res =
                rest.exchange(url("/account"), HttpMethod.GET,
                        new HttpEntity<>(authHeader(jwtUtils.generateToken(u.getEmail()))),
                        String.class);

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }


    // ============================================================
    // INT004 – GET ALL USERS (ADMIN ONLY)
    // ============================================================

    @Test @Order(6)
    void INT004a_adminSuccess() {
        User admin = createAdmin("admin@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        ResponseEntity<String> res =
                rest.exchange(url("/all"), HttpMethod.GET,
                        new HttpEntity<>(authHeader(token)), String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(7)
    void INT004b_customerForbidden() {
        User u = createUser("user@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        ResponseEntity<String> res =
                rest.exchange(url("/all"), HttpMethod.GET,
                        new HttpEntity<>(authHeader(token)), String.class);

        Assertions.assertEquals(403, res.getStatusCodeValue());
    }

    @Test @Order(8)
    void INT004c_missingToken() {
        ResponseEntity<String> res = rest.getForEntity(url("/all"), String.class);
        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(9)
    void INT004d_invalidToken() {
        ResponseEntity<String> res =
                rest.exchange(url("/all"), HttpMethod.GET,
                        new HttpEntity<>(authHeader("bad.token")),
                        String.class);

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }


    // ============================================================
    // INT005 – GET OWN ACCOUNT
    // ============================================================

    @Test @Order(10)
    void INT005a_success() {
        User u = createUser("own@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        ResponseEntity<String> res =
                rest.exchange(url("/account"), HttpMethod.GET,
                        new HttpEntity<>(authHeader(token)), String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(11)
    void INT005b_missingToken() {
        ResponseEntity<String> res = rest.getForEntity(url("/account"), String.class);
        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(12)
    void INT005c_tokenExpired() {
        String token = generateExpiredToken("abc@gmail.com");
        ResponseEntity<String> res =
                rest.exchange(url("/account"), HttpMethod.GET,
                        new HttpEntity<>(authHeader(token)), String.class);

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(13)
    void INT005d_inactiveUser() {
        User u = createUser("inactive2@gmail.com");
        u.setActive(false);
        userRepository.save(u);

        ResponseEntity<String> res =
                rest.exchange(url("/account"), HttpMethod.GET,
                        new HttpEntity<>(authHeader(jwtUtils.generateToken(u.getEmail()))),
                        String.class);

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }


    // ============================================================
    // INT006 – UPDATE ACCOUNT
    // ============================================================

    @Test @Order(14)
    void INT006a_update_success() {
        User u = createUser("upd@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        HttpHeaders h = authHeader(token);
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("name", "NewName");

        ResponseEntity<String> res =
                rest.exchange(url("/update"), HttpMethod.PUT,
                        new HttpEntity<>(body, h), String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(15)
    void INT006b_emailExists() {
        createUser("exists@gmail.com");
        User me = createUser("me@gmail.com");

        String token = jwtUtils.generateToken(me.getEmail());

        HttpHeaders h = authHeader(token);
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("email", "exists@gmail.com");

        ResponseEntity<String> res =
                rest.exchange(url("/update"), HttpMethod.PUT,
                        new HttpEntity<>(body, h), String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(16)
    void INT006c_badEmailFormat() {
        User me = createUser("badfmt@gmail.com");

        String token = jwtUtils.generateToken(me.getEmail());

        HttpHeaders h = authHeader(token);
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("email", "abc@");

        ResponseEntity<String> res =
                rest.exchange(url("/update"), HttpMethod.PUT,
                        new HttpEntity<>(body, h), String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(17)
    void INT006d_phoneNot10Digits() {
        User me = createUser("phone@gmail.com");
        String token = jwtUtils.generateToken(me.getEmail());

        HttpHeaders h = authHeader(token);
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("phoneNumber", "12345");

        ResponseEntity<String> res =
                rest.exchange(url("/update"), HttpMethod.PUT,
                        new HttpEntity<>(body, h), String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(18)
    void INT006e_passwordShort() {
        User me = createUser("pwd@gmail.com");
        String token = jwtUtils.generateToken(me.getEmail());

        HttpHeaders h = authHeader(token);
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("password", "123");

        ResponseEntity<String> res =
                rest.exchange(url("/update"), HttpMethod.PUT,
                        new HttpEntity<>(body, h), String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(19)
    void INT006f_missingToken() {
        ResponseEntity<String> res =
                rest.exchange(url("/update"), HttpMethod.PUT,
                        new HttpEntity<>(new HttpHeaders()), String.class);

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }


    // ============================================================
    // INT007 – DEACTIVATE
    // ============================================================

    @Test @Order(20)
    void INT007a_deactivate_success() {
        User u = createUser("dec@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        ResponseEntity<String> res =
                rest.exchange(url("/deactivate"), HttpMethod.DELETE,
                        new HttpEntity<>(authHeader(token)), String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(21)
    void INT007b_missingToken() {
        ResponseEntity<String> res =
                rest.exchange(url("/deactivate"), HttpMethod.DELETE,
                        new HttpEntity<>(new HttpHeaders()), String.class);

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(22)
    void INT007c_expiredToken() {
        String expired = generateExpiredToken("user@gmail.com");

        ResponseEntity<String> res =
                rest.exchange(url("/deactivate"), HttpMethod.DELETE,
                        new HttpEntity<>(authHeader(expired)), String.class);

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(23)
    void INT007d_userNotFound() {
        User u = createUser("gone@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        userRepository.delete(u);

        ResponseEntity<String> res =
                rest.exchange(url("/deactivate"), HttpMethod.DELETE,
                        new HttpEntity<>(authHeader(token)), String.class);

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(24)
    void INT007e_alreadyInactive() {
        User u = createUser("already@gmail.com");
        u.setActive(false);
        userRepository.save(u);

        ResponseEntity<String> res =
                rest.exchange(url("/deactivate"), HttpMethod.DELETE,
                        new HttpEntity<>(authHeader(jwtUtils.generateToken(u.getEmail()))),
                        String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }
}
