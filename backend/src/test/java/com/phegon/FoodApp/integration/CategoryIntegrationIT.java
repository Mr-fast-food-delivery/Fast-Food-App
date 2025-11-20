package com.phegon.FoodApp.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phegon.FoodApp.FoodAppApplication;
import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.auth_users.repository.UserRepository;
import com.phegon.FoodApp.category.dtos.CategoryDTO;
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
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest(
        classes = FoodAppApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CategoryIntegrationIT {

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
        return "http://localhost:" + port + "/api/categories" + path;
    }

    @BeforeEach
    void setup() {
        // Đảm bảo sạch dữ liệu giữa các test
        menuRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.save(new Role(null, "ADMIN"));
        roleRepository.save(new Role(null, "CUSTOMER"));
    }

    private User createAdmin(String email) {
        Role admin = roleRepository.findByName("ADMIN").orElseThrow();
        User u = User.builder()
                .email(email)
                .name("Admin")
                .password("$2a$10$fake") // không dùng để login nên không sao
                .address("HCM")
                .phoneNumber("0123456789")
                .roles(List.of(admin))
                .isActive(true)
                .build();
        return userRepository.save(u);
    }

    private User createCustomer(String email) {
        Role c = roleRepository.findByName("CUSTOMER").orElseThrow();
        User u = User.builder()
                .email(email)
                .name("Cust")
                .password("$2a$10$fake")
                .address("HCM")
                .phoneNumber("0123456789")
                .roles(List.of(c))
                .isActive(true)
                .build();
        return userRepository.save(u);
    }

    private HttpHeaders auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private Response<?> parse(String json) {
        try {
            return mapper.readValue(json, new TypeReference<Response<?>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================================
    // INT008 – CREATE CATEGORY
    // =========================================================================

    @Test
    @Order(1)
    void INT008_01_createCategory_success() {
        User admin = createAdmin("admin@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        CategoryDTO dto = new CategoryDTO();
        dto.setName("Pizza");
        dto.setDescription("Italian");

        ResponseEntity<String> res = rest.exchange(
                url(""),
                HttpMethod.POST,
                new HttpEntity<>(dto, auth(token)),
                String.class
        );

        Assertions.assertEquals(200, res.getStatusCodeValue());

        Response<?> body = parse(res.getBody());
        Assertions.assertEquals(200, body.getStatusCode());
        Assertions.assertEquals("Category added successfully", body.getMessage());
    }

    @Test
    @Order(2)
    void INT008_02_noToken() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Pizza");

        ResponseEntity<String> res = rest.exchange(
                url(""),
                HttpMethod.POST,
                new HttpEntity<>(dto, new HttpHeaders()),
                String.class
        );

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test
    @Order(3)
    void INT008_03_forbidden_customer() {
        User user = createCustomer("c@gmail.com");
        String token = jwtUtils.generateToken(user.getEmail());

        CategoryDTO dto = new CategoryDTO();
        dto.setName("Pizza");

        ResponseEntity<String> res = rest.exchange(
                url(""),
                HttpMethod.POST,
                new HttpEntity<>(dto, auth(token)),
                String.class
        );

        Assertions.assertEquals(403, res.getStatusCodeValue());
    }

    @Test
    @Order(4)
    void INT008_04_nameNull() {
        User admin = createAdmin("a2@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        CategoryDTO dto = new CategoryDTO();
        dto.setName(null);

        ResponseEntity<String> res = rest.exchange(
                url(""),
                HttpMethod.POST,
                new HttpEntity<>(dto, auth(token)),
                String.class
        );

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test
    @Order(5)
    void INT008_05_nameEmpty() {
        User admin = createAdmin("a3@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        CategoryDTO dto = new CategoryDTO();
        dto.setName("");

        ResponseEntity<String> res = rest.exchange(
                url(""),
                HttpMethod.POST,
                new HttpEntity<>(dto, auth(token)),
                String.class
        );

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test
    @Order(6)
    void INT008_06_nameSpacesOnly() {
        User admin = createAdmin("a4@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        CategoryDTO dto = new CategoryDTO();
        dto.setName("   ");

        ResponseEntity<String> res = rest.exchange(
                url(""),
                HttpMethod.POST,
                new HttpEntity<>(dto, auth(token)),
                String.class
        );

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test
    @Order(7)
    void INT008_07_duplicateName() {
        categoryRepository.save(
                Category.builder()
                        .name("Pizza")
                        .description("desc")
                        .build()
        );

        User admin = createAdmin("a5@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        CategoryDTO dto = new CategoryDTO();
        dto.setName("Pizza");

        ResponseEntity<String> res = rest.exchange(
                url(""),
                HttpMethod.POST,
                new HttpEntity<>(dto, auth(token)),
                String.class
        );

        // Tùy logic: nếu bạn đã thêm check existsByName → 400.
        // Nếu để DB constraint ném lỗi → ExceptionHandler cũng nên map về 400.
        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    // =========================================================================
    // INT009 – UPDATE CATEGORY
    // =========================================================================

    @Test
    @Order(8)
    void INT009_01_update_success() {
        Category c = categoryRepository.save(
                Category.builder()
                        .name("Cake")
                        .description("Sweet")
                        .build()
        );

        User admin = createAdmin("up@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        CategoryDTO dto = new CategoryDTO();
        dto.setId(c.getId());
        dto.setName("NewCake");
        dto.setDescription("newDesc");

        ResponseEntity<String> res = rest.exchange(
                url(""),
                HttpMethod.PUT,
                new HttpEntity<>(dto, auth(token)),
                String.class
        );

        Assertions.assertEquals(200, res.getStatusCodeValue());

        Response<?> body = parse(res.getBody());
        Assertions.assertEquals(200, body.getStatusCode());
        Assertions.assertEquals("Category updated successfully", body.getMessage());
    }

    @Test
    @Order(9)
    void INT009_02_notFound() {
        User admin = createAdmin("adminX@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        CategoryDTO dto = new CategoryDTO();
        dto.setId(999L);
        dto.setName("X");

        ResponseEntity<String> res = rest.exchange(
                url(""),
                HttpMethod.PUT,
                new HttpEntity<>(dto, auth(token)),
                String.class
        );

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test
    @Order(10)
    void INT009_03_missingToken() {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(1L);
        dto.setName("ABC");

        ResponseEntity<String> res = rest.exchange(
                url(""),
                HttpMethod.PUT,
                new HttpEntity<>(dto, new HttpHeaders()),
                String.class
        );

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test
    @Order(11)
    void INT009_04_customerForbidden() {
        Category c = categoryRepository.save(
                Category.builder()
                        .name("Burger")
                        .description("d1")
                        .build()
        );

        User user = createCustomer("cust@gmail.com");
        String token = jwtUtils.generateToken(user.getEmail());

        CategoryDTO dto = new CategoryDTO();
        dto.setId(c.getId());
        dto.setName("NewBurger");

        ResponseEntity<String> res = rest.exchange(
                url(""),
                HttpMethod.PUT,
                new HttpEntity<>(dto, auth(token)),
                String.class
        );

        Assertions.assertEquals(403, res.getStatusCodeValue());
    }

    @Test
    @Order(12)
    void INT009_05_nameEmpty() {
        Category c = categoryRepository.save(
                Category.builder()
                        .name("Tea")
                        .description("d1")
                        .build()
        );

        User admin = createAdmin("a7@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        CategoryDTO dto = new CategoryDTO();
        dto.setId(c.getId());
        dto.setName("");

        ResponseEntity<String> res = rest.exchange(
                url(""),
                HttpMethod.PUT,
                new HttpEntity<>(dto, auth(token)),
                String.class
        );

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test
    @Order(13)
    void INT009_06_duplicateName() {
        categoryRepository.save(
                Category.builder()
                        .name("Sushi")
                        .description("d1")
                        .build()
        );

        Category c2 = categoryRepository.save(
                Category.builder()
                        .name("Tempura")
                        .description("d2")
                        .build()
        );

        User admin = createAdmin("adminD@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        CategoryDTO dto = new CategoryDTO();
        dto.setId(c2.getId());
        dto.setName("Sushi");

        ResponseEntity<String> res = rest.exchange(
                url(""),
                HttpMethod.PUT,
                new HttpEntity<>(dto, auth(token)),
                String.class
        );

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    // =========================================================================
    // INT010 – GET ALL CATEGORIES
    // =========================================================================

    @Test
    @Order(14)
    void INT010_01_getAll_success() {
        categoryRepository.save(
                Category.builder().name("Pizza").description("d1").build()
        );
        categoryRepository.save(
                Category.builder().name("Burger").description("d2").build()
        );

        ResponseEntity<String> res = rest.getForEntity(url("/all"), String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    // =========================================================================
    // INT011 – GET CATEGORY BY ID
    // =========================================================================

    @Test
    @Order(15)
    void INT011_01_success() {
        Category c = categoryRepository.save(
                Category.builder()
                        .name("Pho")
                        .description("d1")
                        .build()
        );

        ResponseEntity<String> res = rest.getForEntity(url("/" + c.getId()), String.class);

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test
    @Order(16)
    void INT011_02_notFound() {
        ResponseEntity<String> res = rest.getForEntity(url("/999"), String.class);

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    // =========================================================================
    // INT012 – DELETE CATEGORY
    // =========================================================================

    @Test
    @Order(17)
    void INT012_01_delete_success() {
        Category c = categoryRepository.save(
                Category.builder()
                        .name("Del")
                        .description("d1")
                        .build()
        );

        User admin = createAdmin("del@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        ResponseEntity<String> res = rest.exchange(
                url("/" + c.getId()),
                HttpMethod.DELETE,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(200, res.getStatusCodeValue());
        Assertions.assertFalse(categoryRepository.existsById(c.getId()));
    }

    @Test
    @Order(18)
    void INT012_02_noToken() {
        Category c = categoryRepository.save(
                Category.builder()
                        .name("A")
                        .description("d1")
                        .build()
        );

        ResponseEntity<String> res = rest.exchange(
                url("/" + c.getId()),
                HttpMethod.DELETE,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );

        Assertions.assertEquals(401, res.getStatusCodeValue());
        Assertions.assertTrue(categoryRepository.existsById(c.getId()));
    }

    @Test
    @Order(19)
    void INT012_03_customerForbidden() {
        Category c = categoryRepository.save(
                Category.builder()
                        .name("X")
                        .description("d1")
                        .build()
        );

        User user = createCustomer("cust@gmail.com");
        String token = jwtUtils.generateToken(user.getEmail());

        ResponseEntity<String> res = rest.exchange(
                url("/" + c.getId()),
                HttpMethod.DELETE,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(403, res.getStatusCodeValue());
        Assertions.assertTrue(categoryRepository.existsById(c.getId()));
    }

    @Test
    @Order(20)
    void INT012_04_notFound() {
        User admin = createAdmin("aa@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        ResponseEntity<String> res = rest.exchange(
                url("/999"),
                HttpMethod.DELETE,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test
    @Order(21)
    void INT012_05_categoryLinkedToMenu_cannotDelete() {

        // 1. Tạo Category
        Category category = categoryRepository.save(
                Category.builder()
                        .name("LinkedCat")
                        .description("desc")
                        .build()
        );

        // 2. Tạo Menu liên kết Category này
        Menu menu = Menu.builder()
                .name("Beef Steak")
                .description("Premium Beef")
                .price(new BigDecimal("199000"))
                .category(category)
                .imageUrl("fake.jpg")
                .build();
        menuRepository.save(menu);

        // 3. Tạo admin token
        User admin = createAdmin("admin_linked@gmail.com");
        String token = jwtUtils.generateToken(admin.getEmail());

        // 4. Gửi DELETE category
        ResponseEntity<String> res = rest.exchange(
            url("/" + category.getId()),
            HttpMethod.DELETE,
            new HttpEntity<>(auth(token)),
            String.class
        );

        int status = res.getStatusCodeValue();

        // Kỳ vọng: service dùng MenuRepository.existsByCategory... và ném 400/409
        Assertions.assertTrue(
                status == 400 || status == 409,
                "Expected HTTP 400 or 409 when deleting category linked to menus, but got = " + status
        );

        // Đảm bảo category vẫn còn trong DB
        Assertions.assertTrue(categoryRepository.existsById(category.getId()));
    }
}
