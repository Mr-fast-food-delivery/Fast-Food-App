package com.phegon.FoodApp.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phegon.FoodApp.FoodAppApplication;
import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.auth_users.repository.UserRepository;
import com.phegon.FoodApp.cart.dtos.CartDTO;
import com.phegon.FoodApp.cart.entity.Cart;
import com.phegon.FoodApp.cart.entity.CartItem;
import com.phegon.FoodApp.cart.repository.CartItemRepository;
import com.phegon.FoodApp.cart.repository.CartRepository;
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
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;

@SpringBootTest(
        classes = FoodAppApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import(FakeS3Config.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CartIntegrationIT {

    @LocalServerPort
    private int port;

    @Autowired private TestRestTemplate rest;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MenuRepository menuRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;

    @Autowired private JwtUtils jwtUtils;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper mapper;

    private String url(String path) {
        return "http://localhost:" + port + "/api/cart" + path;
    }

    @BeforeEach
    void setup() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        menuRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.save(new Role(null, "CUSTOMER"));
        roleRepository.save(new Role(null, "ADMIN"));
    }

    // ============================= HELPERS ===================================

    private User createUser(String email) {
        Role r = roleRepository.findByName("CUSTOMER").orElseThrow();

        User u = User.builder()
                .email(email)
                .name("UserTest")
                .password(passwordEncoder.encode("123456"))
                .isActive(true)
                .phoneNumber("0123456789")
                .address("HCM")
                .roles(List.of(r))
                .build();
        return userRepository.save(u);
    }

    private Menu createMenu(String name) {
        return menuRepository.save(
                Menu.builder()
                        .name(name)
                        .price(BigDecimal.valueOf(10))
                        .description("d")
                        .build()
        );
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
    // INT018 – ADD ITEM
    // =========================================================================

    @Test @Order(1)
    void INT018_01_add_success() {
        User u = createUser("u1@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());
        Menu m = createMenu("Pizza");

        CartDTO req = new CartDTO();
        req.setMenuId(m.getId());
        req.setQuantity(2);

        ResponseEntity<String> res = rest.exchange(
                url("/items"), HttpMethod.POST,
                new HttpEntity<>(req, auth(token)),
                String.class
        );

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(2)
    void INT018_02_unauthorized() {
        CartDTO req = new CartDTO();
        req.setMenuId(1L);
        req.setQuantity(1);

        ResponseEntity<String> res = rest.exchange(
                url("/items"), HttpMethod.POST,
                new HttpEntity<>(req, new HttpHeaders()),
                String.class
        );

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(3)
    void INT018_03_menuNotFound() {
        User u = createUser("u2@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        CartDTO req = new CartDTO();
        req.setMenuId(999L);
        req.setQuantity(1);

        ResponseEntity<String> res = rest.exchange(
                url("/items"), HttpMethod.POST,
                new HttpEntity<>(req, auth(token)),
                String.class
        );

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(4)
    void INT018_04_missingMenuId() {
        User u = createUser("u3@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        CartDTO req = new CartDTO();
        req.setQuantity(2);

        ResponseEntity<String> res = rest.exchange(
                url("/items"), HttpMethod.POST,
                new HttpEntity<>(req, auth(token)),
                String.class
        );

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(5)
    void INT018_05_quantityInvalid() {
        User u = createUser("u4@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());
        Menu m = createMenu("BanhMi");

        CartDTO req = new CartDTO();
        req.setMenuId(m.getId());
        req.setQuantity(0);

        ResponseEntity<String> res = rest.exchange(
                url("/items"), HttpMethod.POST,
                new HttpEntity<>(req, auth(token)),
                String.class
        );

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    @Test @Order(6)
    void INT018_06_cartAutoCreate() {
        User u = createUser("u5@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());
        Menu m = createMenu("Coke");

        CartDTO req = new CartDTO();
        req.setMenuId(m.getId());
        req.setQuantity(1);

        ResponseEntity<String> res = rest.exchange(
                url("/items"), HttpMethod.POST,
                new HttpEntity<>(req, auth(token)),
                String.class
        );

        Assertions.assertEquals(200, res.getStatusCodeValue());
        Assertions.assertTrue(cartRepository.findByUser_Id(u.getId()).isPresent());
    }

    // =========================================================================
    // INT019 – INCREMENT ITEM
    // =========================================================================

    @Test @Order(7)
    void INT019_01_increment_success() {
        User u = createUser("inc@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());
        Menu m = createMenu("Tea");

        Cart cart = Cart.builder()
                .user(u)
                .cartItems(new ArrayList<>())
                .build();
        cart = cartRepository.save(cart);

        CartItem ci = CartItem.builder()
                .cart(cart)
                .menu(m)
                .quantity(1)
                .pricePerUnit(m.getPrice())
                .subtotal(m.getPrice())
                .build();
        cartItemRepository.save(ci);

        ResponseEntity<String> res = rest.exchange(
                url("/items/increment/" + m.getId()),
                HttpMethod.PUT,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(8)
    void INT019_02_unauthorized() {
        ResponseEntity<String> res = rest.exchange(
                url("/items/increment/1"), HttpMethod.PUT,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );
        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(9)
    void INT019_03_cartNotFound() {
        User u = createUser("xx@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        Menu m = createMenu("Pho");

        ResponseEntity<String> res = rest.exchange(
                url("/items/increment/" + m.getId()),
                HttpMethod.PUT,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(10)
    void INT019_04_itemNotInCart() {
        User u = createUser("notincart@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        Menu m = createMenu("Rice");

        Cart cart = Cart.builder()
                .user(u)
                .cartItems(new ArrayList<>())
                .build();
        cartRepository.save(cart);

        ResponseEntity<String> res = rest.exchange(
                url("/items/increment/" + m.getId()),
                HttpMethod.PUT,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(11)
    void INT019_05_menuIdNull() {
        User u = createUser("idnull@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        ResponseEntity<String> res = rest.exchange(
                url("/items/increment/null"),
                HttpMethod.PUT,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    // =========================================================================
    // INT020 – DECREMENT ITEM
    // =========================================================================

    @Test @Order(12)
    void INT020_01_decrement_success() {
        User u = createUser("dec1@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());
        Menu m = createMenu("MiTom");

        Cart cart = Cart.builder()
                .user(u)
                .cartItems(new ArrayList<>())
                .build();
        cartRepository.save(cart);

        CartItem ci = CartItem.builder()
                .cart(cart)
                .menu(m)
                .quantity(2)
                .pricePerUnit(m.getPrice())
                .subtotal(m.getPrice().multiply(BigDecimal.valueOf(2)))
                .build();
        cartItemRepository.save(ci);

        ResponseEntity<String> res = rest.exchange(
                url("/items/decrement/" + m.getId()), HttpMethod.PUT,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(13)
    void INT020_02_removeItemWhenQty1() {
        User u = createUser("dec2@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());
        Menu m = createMenu("Sprite");

        Cart cart = Cart.builder()
                .user(u)
                .cartItems(new ArrayList<>())
                .build();
        cartRepository.save(cart);

        CartItem ci = CartItem.builder()
                .cart(cart)
                .menu(m)
                .quantity(1)
                .pricePerUnit(m.getPrice())
                .subtotal(m.getPrice())
                .build();
        cartItemRepository.save(ci);

        rest.exchange(
                url("/items/decrement/" + m.getId()),
                HttpMethod.PUT,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(0,
                cartRepository.findByUser_Id(u.getId()).get().getCartItems().size());
    }

    @Test @Order(14)
    void INT020_03_unauthorized() {
        ResponseEntity<String> res = rest.exchange(
                url("/items/decrement/1"), HttpMethod.PUT,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );
        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(15)
    void INT020_04_cartNotFound() {
        User u = createUser("noCart@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        Menu m = createMenu("Pepsi");

        ResponseEntity<String> res = rest.exchange(
                url("/items/decrement/" + m.getId()), HttpMethod.PUT,
                new HttpEntity<>(auth(token)), String.class
        );

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(16)
    void INT020_05_itemNotInCart() {
        User u = createUser("emptycart@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());
        Menu m = createMenu("Soda");

        Cart cart = Cart.builder()
                .user(u)
                .cartItems(new ArrayList<>())
                .build();
        cartRepository.save(cart);

        ResponseEntity<String> res = rest.exchange(
                url("/items/decrement/" + m.getId()), HttpMethod.PUT,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(17)
    void INT020_06_menuIdNull() {
        User u = createUser("mnull@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        ResponseEntity<String> res = rest.exchange(
                url("/items/decrement/null"), HttpMethod.PUT,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    // =========================================================================
    // INT021 – REMOVE ITEM
    // =========================================================================

    @Test @Order(18)
    void INT021_01_remove_success() {
        User u = createUser("rm@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        Menu m = createMenu("BBQ");

        Cart cart = Cart.builder()
                .user(u)
                .cartItems(new ArrayList<>())
                .build();
        cartRepository.save(cart);

        CartItem ci = cartItemRepository.save(
                     CartItem.builder()
                        .cart(cart)
                        .menu(m)
                        .quantity(1)
                        .pricePerUnit(m.getPrice())
                        .subtotal(m.getPrice())
                        .build()
        );

        ResponseEntity<String> res = rest.exchange(
                url("/items/" + ci.getId()),
                HttpMethod.DELETE,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(19)
    void INT021_02_unauthorized() {
        ResponseEntity<String> res = rest.exchange(
                url("/items/1"), HttpMethod.DELETE,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );
        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(20)
    void INT021_03_cartNotFound() {
        User u = createUser("nocart2@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        ResponseEntity<String> res = rest.exchange(
                url("/items/1"), HttpMethod.DELETE,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(21)
    void INT021_04_cartItemNotFound() {
        User u = createUser("ci@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        Cart cart = Cart.builder()
                .user(u)
                .cartItems(new ArrayList<>())
                .build();
        cartRepository.save(cart);

        ResponseEntity<String> res = rest.exchange(
                url("/items/999"), HttpMethod.DELETE,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(22)
    void INT021_05_itemNotBelongToCart() {
        User u = createUser("owner@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        User other = createUser("other@gmail.com");

        Cart c1 = Cart.builder().user(u).cartItems(new ArrayList<>()).build();
        c1 = cartRepository.save(c1);

        Cart c2 = Cart.builder().user(other).cartItems(new ArrayList<>()).build();
        c2 = cartRepository.save(c2);

        Menu m = createMenu("X");

        CartItem ci = cartItemRepository.save(
                CartItem.builder()
                        .cart(c2)
                        .menu(m)
                        .quantity(1)
                        .pricePerUnit(m.getPrice())
                        .subtotal(m.getPrice())
                        .build()
        );

        ResponseEntity<String> res = rest.exchange(
                url("/items/" + ci.getId()),
                HttpMethod.DELETE,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    @Test @Order(23)
    void INT021_06_invalidCartItemId() {
        User u = createUser("inv@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        ResponseEntity<String> res = rest.exchange(
                url("/items/null"), HttpMethod.DELETE,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(400, res.getStatusCodeValue());
    }

    // =========================================================================
    // INT022 – GET CART
    // =========================================================================

    @Test @Order(24)
    void INT022_01_getCart_success() {
        User u = createUser("get@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        Cart c = Cart.builder()
                .user(u)
                .cartItems(new ArrayList<>())
                .build();
        cartRepository.save(c);

        ResponseEntity<String> res = rest.exchange(
                url(""), HttpMethod.GET,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(25)
    void INT022_02_getCart_empty() {
        User u = createUser("empty@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        Cart c = Cart.builder()
                .user(u)
                .cartItems(new ArrayList<>())
                .build();
        cartRepository.save(c);

        ResponseEntity<String> res = rest.exchange(
                url(""), HttpMethod.GET,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(26)
    void INT022_03_unauthorized() {
        ResponseEntity<String> res = rest.exchange(
                url(""), HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(27)
    void INT022_04_cartNotFound() {
        User u = createUser("nocart@email.com");
        String token = jwtUtils.generateToken(u.getEmail());

        ResponseEntity<String> res = rest.exchange(
                url(""), HttpMethod.GET,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }

    // =========================================================================
    // INT023 – CLEAR CART
    // =========================================================================

    @Test @Order(28)
    void INT023_01_clear_success() {
        User u = createUser("clr@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());
        Menu m = createMenu("Kebab");

        Cart cart = Cart.builder()
                .user(u)
                .cartItems(new ArrayList<>())
                .build();
        cartRepository.save(cart);

        CartItem ci = CartItem.builder()
                .cart(cart)
                .menu(m)
                .quantity(1)
                .pricePerUnit(m.getPrice())
                .subtotal(m.getPrice())
                .build();
        cartItemRepository.save(ci);

        ResponseEntity<String> res = rest.exchange(
                url(""), HttpMethod.DELETE,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(200, res.getStatusCodeValue());
    }

    @Test @Order(29)
    void INT023_02_unauthorized() {
        ResponseEntity<String> res = rest.exchange(
                url(""), HttpMethod.DELETE,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );

        Assertions.assertEquals(401, res.getStatusCodeValue());
    }

    @Test @Order(30)
    void INT023_03_cartNotFound() {
        User u = createUser("noclf@gmail.com");
        String token = jwtUtils.generateToken(u.getEmail());

        ResponseEntity<String> res = rest.exchange(
                url(""), HttpMethod.DELETE,
                new HttpEntity<>(auth(token)),
                String.class
        );

        Assertions.assertEquals(404, res.getStatusCodeValue());
    }
}
