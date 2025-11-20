package com.phegon.FoodApp.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phegon.FoodApp.FoodAppApplication;
import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.auth_users.repository.UserRepository;
import com.phegon.FoodApp.category.entity.Category;
import com.phegon.FoodApp.category.repository.CategoryRepository;
import com.phegon.FoodApp.menu.entity.Menu;
import com.phegon.FoodApp.menu.repository.MenuRepository;
import com.phegon.FoodApp.response.Response;
import com.phegon.FoodApp.role.entity.Role;
import com.phegon.FoodApp.role.repository.RoleRepository;
import com.phegon.FoodApp.security.JwtUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@SpringBootTest(
    classes = {
        FoodAppApplication.class,
        FakeS3Config.class   // Cho chắc chắn Spring chỉ load fake
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)

@ActiveProfiles("test")
@Import(FakeS3Config.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MenuIntegrationIT {

    @LocalServerPort
    private int port;

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper mapper;

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtils jwtUtils;

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MenuRepository menuRepository;

    private String url(String path) {
        return "http://localhost:" + port + "/api/menu" + path;
    }

    @BeforeEach
    void setup() {
        menuRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.save(new Role(null, "ADMIN"));
        roleRepository.save(new Role(null, "CUSTOMER"));
    }

    // ===========================
    // Helpers
    // ===========================
    private User createAdmin(String email) {
        Role r = roleRepository.findByName("ADMIN").orElseThrow();
        User u = User.builder()
                .email(email)
                .name("Admin")
                .password("$2a$10$faked")
                .isActive(true)
                .phoneNumber("0123456789")
                .address("HCM")
                .roles(List.of(r))
                .build();
        return userRepository.save(u);
    }

    private User createCustomer(String email) {
        Role r = roleRepository.findByName("CUSTOMER").orElseThrow();
        User u = User.builder()
                .email(email)
                .name("Customer")
                .password("$2a$10$faked")
                .isActive(true)
                .phoneNumber("0123456789")
                .address("HCM")
                .roles(List.of(r))
                .build();
        return userRepository.save(u);
    }

    private HttpHeaders authMultipart(String token) {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + token);
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        return h;
    }

    private HttpHeaders auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + token);
        return h;
    }

    private Resource fakeImage() {
        byte[] bytes = "fake image".getBytes(StandardCharsets.UTF_8);
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "image.png";
            }
        };
    }

    private Resource emptyImage() {
        byte[] bytes = new byte[0];
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "empty.png";
            }
        };
    }

    private Response<?> parse(String json) {
        try {
            return mapper.readValue(json, new TypeReference<Response<?>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ============================================================
    // INT013 – CREATE MENU
    // ============================================================

    @Test @Order(1)
    void INT013_01_createMenu_success() {
        User admin = createAdmin("admin@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        Category cat = categoryRepository.save(
                Category.builder().name("Pizza").description("desc").build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "Margherita");
        body.add("description", "Cheese");
        body.add("price", "120.5");
        body.add("categoryId", cat.getId().toString());
        body.add("imageFile", fakeImage());

        HttpEntity<MultiValueMap<String, Object>> req =
                new HttpEntity<>(body, authMultipart(token));

        ResponseEntity<String> res =
                rest.postForEntity(url(""), req, String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(2)
    void INT013_02_missingToken() {
        Category cat = categoryRepository.save(
                Category.builder().name("C1").description("d").build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "A");
        body.add("price", "12");
        body.add("categoryId", cat.getId().toString());
        body.add("imageFile", fakeImage());

        ResponseEntity<String> res =
                rest.postForEntity(url(""), new HttpEntity<>(body), String.class);

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(3)
    void INT013_03_forbidden_customer() {
        User u = createCustomer("c@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        Category cat = categoryRepository.save(
                Category.builder().name("C2").description("d").build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "A");
        body.add("price", "12");
        body.add("categoryId", cat.getId().toString());
        body.add("imageFile", fakeImage());

        HttpEntity<MultiValueMap<String, Object>> req =
                new HttpEntity<>(body, authMultipart(token));

        ResponseEntity<String> res =
                rest.postForEntity(url(""), req, String.class);

        Assertions.assertEquals(403, res.getStatusCodeValue());
    }

    @Test @Order(4)
    void INT013_04_categoryNotFound() {
        User admin = createAdmin("a@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "A");
        body.add("price", "12");
        body.add("categoryId", "999");
        body.add("imageFile", fakeImage());

        HttpEntity<MultiValueMap<String, Object>> req =
                new HttpEntity<>(body, authMultipart(token));

        ResponseEntity<String> res = rest.postForEntity(url(""), req, String.class);

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(5)
    void INT013_05_nameEmpty() {
        User admin = createAdmin("n@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        Category cat = categoryRepository.save(
                Category.builder().name("C3").description("d").build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "");
        body.add("price", "12");
        body.add("categoryId", cat.getId().toString());
        body.add("imageFile", fakeImage());

        ResponseEntity<String> res =
                rest.postForEntity(url(""), new HttpEntity<>(body, authMultipart(token)), String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(6)
    void INT013_06_invalidPrice() {
        User admin = createAdmin("p@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        Category cat = categoryRepository.save(
                Category.builder().name("C4").description("d").build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "A");
        body.add("price", "-5");
        body.add("categoryId", cat.getId().toString());
        body.add("imageFile", fakeImage());

        ResponseEntity<String> res =
                rest.postForEntity(url(""), new HttpEntity<>(body, authMultipart(token)), String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(7)
    void INT013_07_imageEmpty() {
        User admin = createAdmin("img@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        Category cat = categoryRepository.save(
                Category.builder().name("C5").description("d").build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "Test");
        body.add("price", "12");
        body.add("categoryId", cat.getId().toString());
        // thiếu imageFile

        ResponseEntity<String> res =
                rest.postForEntity(url(""), new HttpEntity<>(body, authMultipart(token)), String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    // ============================================================
    // INT014 – UPDATE MENU
    // ============================================================

    @Test @Order(8)
    void INT014_01_updateSuccess() {
        User admin = createAdmin("u1@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        Category cat = categoryRepository.save(
                Category.builder().name("CT1").description("d").build()
        );

        Menu menu = menuRepository.save(
                Menu.builder()
                        .name("Old")
                        .price(BigDecimal.valueOf(10))
                        .category(cat)
                        .imageUrl("old.png")
                        .build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", menu.getId().toString());
        body.add("name", "NewName");
        body.add("price", "50");
        body.add("categoryId", cat.getId().toString());

        ResponseEntity<String> res =
                rest.exchange(url(""),
                        HttpMethod.PUT,
                        new HttpEntity<>(body, authMultipart(token)),
                        String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(9)
    void INT014_02_menuNotFound() {
        User admin = createAdmin("u2@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", "999");
        body.add("name", "X");
        body.add("price", "10");
        body.add("categoryId", "1");

        ResponseEntity<String> res =
                rest.exchange(url(""), HttpMethod.PUT,
                        new HttpEntity<>(body, authMultipart(token)),
                        String.class);

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(10)
    void INT014_03_missingToken() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", "1");

        ResponseEntity<String> res =
                rest.exchange(url(""), HttpMethod.PUT,
                        new HttpEntity<>(body),
                        String.class);

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(11)
    void INT014_04_customerForbidden() {
        User cust = createCustomer("cu@gmail.com");
        String token = jwtUtils.generateToken(cust.getEmail());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", "1");

        ResponseEntity<String> res =
                rest.exchange(url(""), HttpMethod.PUT,
                        new HttpEntity<>(body, authMultipart(token)),
                        String.class);

        Assertions.assertEquals(403, res.getStatusCodeValue());
    }

    @Test @Order(12)
    void INT014_05_categoryNotFound() {
        User admin = createAdmin("u5@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        Menu m = menuRepository.save(
                Menu.builder()
                        .name("A")
                        .price(BigDecimal.TEN)
                        .build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", m.getId().toString());
        body.add("name", "X");
        body.add("price", "10");
        body.add("categoryId", "999");

        ResponseEntity<String> res =
                rest.exchange(url(""), HttpMethod.PUT,
                        new HttpEntity<>(body, authMultipart(token)),
                        String.class);

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(13)
    void INT014_06_nameEmpty() {
        User admin = createAdmin("u6@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        Category c = categoryRepository.save(
                Category.builder().name("xxx").description("d").build()
        );

        Menu m = menuRepository.save(
                Menu.builder().name("A").price(BigDecimal.ONE).category(c).build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", m.getId().toString());
        body.add("name", "");
        body.add("price", "12");
        body.add("categoryId", c.getId().toString());

        ResponseEntity<String> res =
                rest.exchange(url(""), HttpMethod.PUT,
                        new HttpEntity<>(body, authMultipart(token)),
                        String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(14)
    void INT014_07_invalidPriceUpdate() {
        User admin = createAdmin("u7@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        Category cat = categoryRepository.save(
                Category.builder().name("cc").description("d").build()
        );

        Menu m = menuRepository.save(
                Menu.builder().name("A").price(BigDecimal.ONE).category(cat).build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", m.getId().toString());
        body.add("name", "X");
        body.add("price", "-1");
        body.add("categoryId", cat.getId().toString());

        ResponseEntity<String> res =
                rest.exchange(url(""), HttpMethod.PUT,
                        new HttpEntity<>(body, authMultipart(token)),
                        String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(15)
    void INT014_08_emptyImage() {
        User admin = createAdmin("u8@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        Category cat = categoryRepository.save(
                Category.builder().name("ca").description("d").build()
        );

        Menu m = menuRepository.save(
                Menu.builder().name("A").price(BigDecimal.ONE).category(cat).build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", m.getId().toString());
        body.add("name", "Aaa");
        body.add("price", "10");
        body.add("categoryId", cat.getId().toString());
        body.add("imageFile", emptyImage());

        ResponseEntity<String> res =
                rest.exchange(url(""), HttpMethod.PUT,
                        new HttpEntity<>(body, authMultipart(token)),
                        String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    // ============================================================
    // INT015 – DELETE MENU
    // ============================================================

    @Test @Order(16)
    void INT015_01_deleteSuccess() {
        User admin = createAdmin("d1@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        Category c = categoryRepository.save(
                Category.builder().name("Del").description("d").build()
        );

        Menu m = menuRepository.save(
                Menu.builder().name("A").price(BigDecimal.ONE).category(c).build()
        );

        ResponseEntity<String> res =
                rest.exchange(url("/" + m.getId()),
                        HttpMethod.DELETE,
                        new HttpEntity<>(auth(token)),
                        String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(17)
    void INT015_02_missingToken() {
        ResponseEntity<String> res =
                rest.exchange(url("/1"), HttpMethod.DELETE,
                        new HttpEntity<>(new HttpHeaders()),
                        String.class);

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(18)
    void INT015_03_forbiddenCustomer() {
        User cust = createCustomer("xx@gmail.com");
        String token = jwtUtils.generateToken(cust.getEmail());

        ResponseEntity<String> res =
                rest.exchange(url("/1"), HttpMethod.DELETE,
                        new HttpEntity<>(auth(token)),
                        String.class);

        Assertions.assertEquals(403, res.getStatusCodeValue());
    }

    @Test @Order(19)
    void INT015_04_notFound() {
        User admin = createAdmin("d3@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        ResponseEntity<String> res =
                rest.exchange(url("/999"), HttpMethod.DELETE,
                        new HttpEntity<>(auth(token)),
                        String.class);

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(20)
    void INT015_05_menuInCart() {
        // Hiện chưa có ràng buộc Cart → EXPECT 200
        User admin = createAdmin("d4@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        Category c = categoryRepository.save(
                Category.builder().name("Cart").description("d").build()
        );

        Menu m = menuRepository.save(
                Menu.builder().name("X").price(BigDecimal.ONE).category(c).build()
        );

        ResponseEntity<String> res =
                rest.exchange(url("/" + m.getId()),
                        HttpMethod.DELETE,
                        new HttpEntity<>(auth(token)),
                        String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(21)
    void INT015_06_menuInOrder() {
        // Hiện chưa ràng buộc OrderItems → EXPECT 200
        User admin = createAdmin("d5@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        Category c = categoryRepository.save(
                Category.builder().name("Order").description("d").build()
        );

        Menu m = menuRepository.save(
                Menu.builder().name("X2").price(BigDecimal.ONE).category(c).build()
        );

        ResponseEntity<String> res =
                rest.exchange(url("/" + m.getId()),
                        HttpMethod.DELETE,
                        new HttpEntity<>(auth(token)),
                        String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    // ============================================================
    // INT016 – GET ALL MENUS
    // ============================================================

    @Test @Order(22)
    void INT016_01_getAll_success() {
        Category c = categoryRepository.save(
                Category.builder().name("c1").description("d").build()
        );

        menuRepository.save(
                Menu.builder().name("M1").price(BigDecimal.TEN).category(c).build()
        );
        menuRepository.save(
                Menu.builder().name("M2").price(BigDecimal.ONE).category(c).build()
        );

        ResponseEntity<String> res = rest.getForEntity(url(""), String.class);
        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    // ============================================================
    // INT017 – GET MENU BY ID
    // ============================================================

    @Test @Order(23)
    void INT017_01_success() {
        Category c = categoryRepository.save(
                Category.builder().name("ddd").description("d").build()
        );
        Menu m = menuRepository.save(
                Menu.builder().name("A").price(BigDecimal.ONE).category(c).build()
        );

        ResponseEntity<String> res = rest.getForEntity(url("/" + m.getId()), String.class);
        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(24)
    void INT017_02_notFound() {
        ResponseEntity<String> res = rest.getForEntity(url("/999"), String.class);
        Assertions.assertEquals(404, res.getStatusCodeValue());
    }
}
