package com.phegon.FoodApp.integration;

import com.phegon.FoodApp.FoodAppApplication;
import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.auth_users.repository.UserRepository;
import com.phegon.FoodApp.category.entity.Category;
import com.phegon.FoodApp.category.repository.CategoryRepository;
import com.phegon.FoodApp.config.FakeS3Config;
import com.phegon.FoodApp.menu.entity.Menu;
import com.phegon.FoodApp.menu.repository.MenuRepository;
import com.phegon.FoodApp.role.entity.Role;
import com.phegon.FoodApp.role.repository.RoleRepository;
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


@ActiveProfiles("test")
@SpringBootTest(
    classes = {FoodAppApplication.class, FakeS3Config.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MenuIntegrationIT {

    @LocalServerPort
    private int port;

    @Autowired private TestRestTemplate rest;

    @Autowired private RoleRepository roleRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private CategoryRepository catRepo;
    @Autowired private MenuRepository menuRepo;

    private String url(String path) {
        return "http://localhost:" + port + "/api/menu" + path;
    }

    // ========= AUTH HEADERS ==========
    private HttpHeaders adminMultipart() {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer admin-token");
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        return h;
    }

    private HttpHeaders admin() {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer admin-token");
        return h;
    }

    private HttpHeaders customer() {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer customer-token");
        return h;
    }

    // ========= HELPERS ==========
    @BeforeEach
    void setup() {
        menuRepo.deleteAll();
        catRepo.deleteAll();
        userRepo.deleteAll();
        roleRepo.deleteAll();

        roleRepo.save(new Role(null, "ADMIN"));
        roleRepo.save(new Role(null, "CUSTOMER"));
    }

    private User createAdmin(String email) {
        Role r = roleRepo.findByName("ADMIN").orElseThrow();
        User u = User.builder()
                .email(email)
                .name("Admin")
                .password("123")
                .isActive(true)
                .roles(List.of(r))
                .build();
        return userRepo.save(u);
    }

    private User createCustomer(String email) {
        Role r = roleRepo.findByName("CUSTOMER").orElseThrow();
        User u = User.builder()
                .email(email)
                .name("Customer")
                .password("123")
                .isActive(true)
                .roles(List.of(r))
                .build();
        return userRepo.save(u);
    }

    private Resource fakeImg() {
        byte[] bytes = "fake image".getBytes(StandardCharsets.UTF_8);
        return new ByteArrayResource(bytes) {
            @Override public String getFilename() { return "image.png"; }
        };
    }

    private Resource emptyImg() {
        byte[] bytes = new byte[0];
        return new ByteArrayResource(bytes) {
            @Override public String getFilename() { return "empty.png"; }
        };
    }

    // ====================================================
    // INT013 – CREATE MENU
    // ====================================================

    @Test @Order(1)
    void INT013_01_createMenu_success() {
        createAdmin("admin@gmail.com");

        Category c = catRepo.save(
                Category.builder().name("Pizza").description("desc").build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "Margherita");
        body.add("description", "Cheese");
        body.add("price", "120.5");
        body.add("categoryId", c.getId().toString());
        body.add("imageFile", fakeImg());

        ResponseEntity<String> res =
                rest.postForEntity(url(""), new HttpEntity<>(body, adminMultipart()), String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(2)
    void INT013_02_missingToken() {
        Category c = catRepo.save(
                Category.builder().name("C1").description("d").build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "A");
        body.add("price", "12");
        body.add("categoryId", c.getId().toString());
        body.add("imageFile", fakeImg());

        ResponseEntity<String> res =
                rest.postForEntity(url(""), new HttpEntity<>(body), String.class);

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(3)
    void INT013_03_forbidden_customer() {
        createCustomer("c@gmail.com");

        Category c = catRepo.save(
                Category.builder().name("C2").description("d").build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "A");
        body.add("price", "12");
        body.add("categoryId", c.getId().toString());
        body.add("imageFile", fakeImg());

        ResponseEntity<String> res =
                rest.postForEntity(url(""), new HttpEntity<>(body, customer()), String.class);

        Assertions.assertEquals(403, res.getStatusCodeValue());
    }

    @Test @Order(4)
    void INT013_04_categoryNotFound() {
        createAdmin("a@gmail.com");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "A");
        body.add("price", "12");
        body.add("categoryId", "999");
        body.add("imageFile", fakeImg());

        ResponseEntity<String> res =
                rest.postForEntity(url(""), new HttpEntity<>(body, adminMultipart()), String.class);

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(5)
    void INT013_05_nameEmpty() {
        createAdmin("n@gmail.com");

        Category c = catRepo.save(
                Category.builder().name("C3").description("d").build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "");
        body.add("price", "12");
        body.add("categoryId", c.getId().toString());
        body.add("imageFile", fakeImg());

        ResponseEntity<String> res =
                rest.postForEntity(url(""), new HttpEntity<>(body, adminMultipart()), String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(6)
    void INT013_06_invalidPrice() {
        createAdmin("p@gmail.com");

        Category c = catRepo.save(
                Category.builder().name("C4").description("d").build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "A");
        body.add("price", "-5");
        body.add("categoryId", c.getId().toString());
        body.add("imageFile", fakeImg());

        ResponseEntity<String> res =
                rest.postForEntity(url(""), new HttpEntity<>(body, adminMultipart()), String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(7)
    void INT013_07_imageEmpty() {
        createAdmin("img@gmail.com");

        Category c = catRepo.save(
                Category.builder().name("C5").description("d").build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "Test");
        body.add("price", "12");
        body.add("categoryId", c.getId().toString());
        // không add imageFile

        ResponseEntity<String> res =
                rest.postForEntity(url(""), new HttpEntity<>(body, adminMultipart()), String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    // ====================================================
    // INT014 – UPDATE MENU
    // ====================================================

    @Test @Order(8)
    void INT014_01_updateSuccess() {
        createAdmin("u1@gmail.com");

        Category c = catRepo.save(
                Category.builder().name("CT1").description("d").build()
        );

        Menu m = menuRepo.save(
                Menu.builder()
                        .name("Old")
                        .price(BigDecimal.valueOf(10))
                        .category(c)
                        .imageUrl("old.png")
                        .build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", m.getId().toString());
        body.add("name", "NewName");
        body.add("price", "50");
        body.add("categoryId", c.getId().toString());

        ResponseEntity<String> res =
                rest.exchange(url(""), HttpMethod.PUT,
                        new HttpEntity<>(body, adminMultipart()),
                        String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(9)
    void INT014_02_menuNotFound() {
        createAdmin("u2@gmail.com");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", "999");
        body.add("name", "X");
        body.add("price", "10");
        body.add("categoryId", "1");

        ResponseEntity<String> res =
                rest.exchange(url(""), HttpMethod.PUT,
                        new HttpEntity<>(body, adminMultipart()),
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
        createCustomer("cu@gmail.com");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", "1");

        ResponseEntity<String> res =
                rest.exchange(url(""), HttpMethod.PUT,
                        new HttpEntity<>(body, customer()),
                        String.class);

        Assertions.assertEquals(403, res.getStatusCodeValue());
    }

    @Test @Order(12)
    void INT014_05_categoryNotFound() {
        createAdmin("u5@gmail.com");

        Menu m = menuRepo.save(
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
                        new HttpEntity<>(body, adminMultipart()),
                        String.class);

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(13)
    void INT014_06_nameEmpty() {
        createAdmin("u6@gmail.com");

        Category c = catRepo.save(
                Category.builder().name("xxx").description("d").build()
        );

        Menu m = menuRepo.save(
                Menu.builder().name("A").price(BigDecimal.ONE).category(c).build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", m.getId().toString());
        body.add("name", "");
        body.add("price", "12");
        body.add("categoryId", c.getId().toString());

        ResponseEntity<String> res =
                rest.exchange(url(""), HttpMethod.PUT,
                        new HttpEntity<>(body, adminMultipart()),
                        String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(14)
    void INT014_07_invalidPriceUpdate() {
        createAdmin("u7@gmail.com");

        Category c = catRepo.save(
                Category.builder().name("cc").description("d").build()
        );

        Menu m = menuRepo.save(
                Menu.builder().name("A").price(BigDecimal.ONE).category(c).build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", m.getId().toString());
        body.add("name", "X");
        body.add("price", "-1");
        body.add("categoryId", c.getId().toString());

        ResponseEntity<String> res =
                rest.exchange(url(""), HttpMethod.PUT,
                        new HttpEntity<>(body, adminMultipart()),
                        String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(15)
    void INT014_08_emptyImage() {
        createAdmin("u8@gmail.com");

        Category c = catRepo.save(
                Category.builder().name("ca").description("d").build()
        );

        Menu m = menuRepo.save(
                Menu.builder().name("A").price(BigDecimal.ONE).category(c).build()
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", m.getId().toString());
        body.add("name", "Aaa");
        body.add("price", "10");
        body.add("categoryId", c.getId().toString());
        body.add("imageFile", emptyImg());

        ResponseEntity<String> res =
                rest.exchange(url(""), HttpMethod.PUT,
                        new HttpEntity<>(body, adminMultipart()),
                        String.class);

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    // ====================================================
    // INT015 – DELETE MENU
    // ====================================================

    @Test @Order(16)
    void INT015_01_deleteSuccess() {
        createAdmin("d1@gmail.com");

        Category c = catRepo.save(
                Category.builder().name("Del").description("d").build()
        );

        Menu m = menuRepo.save(
                Menu.builder().name("A").price(BigDecimal.ONE).category(c).build()
        );

        ResponseEntity<String> res =
                rest.exchange(url("/" + m.getId()),
                        HttpMethod.DELETE,
                        new HttpEntity<>(admin()),
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
        createCustomer("xx@gmail.com");

        ResponseEntity<String> res =
                rest.exchange(url("/1"), HttpMethod.DELETE,
                        new HttpEntity<>(customer()),
                        String.class);

        Assertions.assertEquals(403, res.getStatusCodeValue());
    }

    @Test @Order(19)
    void INT015_04_notFound() {
        createAdmin("d3@gmail.com");

        ResponseEntity<String> res =
                rest.exchange(url("/999"), HttpMethod.DELETE,
                        new HttpEntity<>(admin()),
                        String.class);

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(20)
    void INT015_05_menuInCart() {
        createAdmin("d4@gmail.com");

        Category c = catRepo.save(
                Category.builder().name("Cart").description("d").build()
        );

        Menu m = menuRepo.save(
                Menu.builder().name("X").price(BigDecimal.ONE).category(c).build()
        );

        ResponseEntity<String> res =
                rest.exchange(url("/" + m.getId()),
                        HttpMethod.DELETE,
                        new HttpEntity<>(admin()),
                        String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(21)
    void INT015_06_menuInOrder() {
        createAdmin("d5@gmail.com");

        Category c = catRepo.save(
                Category.builder().name("Order").description("d").build()
        );

        Menu m = menuRepo.save(
                Menu.builder().name("X2").price(BigDecimal.ONE).category(c).build()
        );

        ResponseEntity<String> res =
                rest.exchange(url("/" + m.getId()),
                        HttpMethod.DELETE,
                        new HttpEntity<>(admin()),
                        String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    // ====================================================
    // INT016 – GET ALL MENUS
    // ====================================================

    @Test @Order(22)
    void INT016_01_getAll_success() {
        Category c = catRepo.save(
                Category.builder().name("c1").description("d").build()
        );

        menuRepo.save(
                Menu.builder().name("M1").price(BigDecimal.TEN).category(c).build()
        );
        menuRepo.save(
                Menu.builder().name("M2").price(BigDecimal.ONE).category(c).build()
        );

        ResponseEntity<String> res = rest.getForEntity(url(""), String.class);
        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    // ====================================================
    // INT017 – GET MENU BY ID
    // ====================================================

    @Test @Order(23)
    void INT017_01_success() {
        Category c = catRepo.save(
                Category.builder().name("ddd").description("d").build()
        );
        Menu m = menuRepo.save(
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
