package com.phegon.FoodApp.cart.service;

import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.auth_users.services.UserService;
import com.phegon.FoodApp.cart.dtos.CartDTO;
import com.phegon.FoodApp.cart.entity.Cart;
import com.phegon.FoodApp.cart.entity.CartItem;
import com.phegon.FoodApp.cart.repository.CartItemRepository;
import com.phegon.FoodApp.cart.repository.CartRepository;
import com.phegon.FoodApp.cart.services.CartServiceImpl;
import com.phegon.FoodApp.exceptions.NotFoundException;
import com.phegon.FoodApp.menu.entity.Menu;
import com.phegon.FoodApp.menu.repository.MenuRepository;
import com.phegon.FoodApp.response.Response;
import com.phegon.FoodApp.review.entity.Review;
import com.phegon.FoodApp.cart.dtos.CartItemDTO;
import com.phegon.FoodApp.menu.dtos.MenuDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private MenuRepository menuRepository;
    @Mock private UserService userService;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private CartServiceImpl cartService;

    private User mockUser() {
        User u = new User();
        u.setId(1L);
        u.setEmail("test@example.com");
        u.setActive(true);
        return u;
    }

    private Menu mockMenu(Long id, BigDecimal price) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setName("Menu " + id);
        menu.setDescription("Description");
        menu.setPrice(price);
        menu.setImageUrl("image.jpg");
        return menu;
    }

    private CartItem mockCartItem(Long id, Menu menu, int qty, BigDecimal subtotal) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setMenu(menu);
        item.setQuantity(qty);
        item.setPricePerUnit(new BigDecimal("10000"));
        item.setSubtotal(subtotal);
        return item;
    }

    private Cart mockCart(User user, List<CartItem> items) {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);
        cart.setCartItems(items);
        return cart;
    }

        @BeforeEach
        void init() {
            MockitoAnnotations.openMocks(this);
        }

    // ====================addItemToCart==================
    @Test
    void testAddItem_NewCartCreated() {

        User user = mockUser();
        when(userService.getCurrentLoggedInUser()).thenReturn(user);

        Menu menu = mockMenu(1L, BigDecimal.valueOf(10));
        when(menuRepository.findById(1L)).thenReturn(Optional.of(menu));

        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);
        cart.setCartItems(new ArrayList<>());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartDTO dto = new CartDTO();
        dto.setMenuId(1L);
        dto.setQuantity(2);

        Response<?> response = cartService.addItemToCart(dto);

        assertEquals(200, response.getStatusCode());
        verify(cartRepository).save(any(Cart.class));
        verify(cartItemRepository).save(any(CartItem.class));
    }



    @Test
    void testAddItem_AddToExistingCart() {
        User user = mockUser();
        Menu menu = mockMenu(2L, BigDecimal.valueOf(100));

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setCartItems(new ArrayList<>());

        CartDTO dto = new CartDTO();
        dto.setMenuId(2L);
        dto.setQuantity(1);

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(menuRepository.findById(2L)).thenReturn(Optional.of(menu));
        when(cartRepository.findByUser_Id(1L)).thenReturn(Optional.of(cart));

        Response<?> res = cartService.addItemToCart(dto);

        assertEquals(200, res.getStatusCode());
        verify(cartItemRepository).save(any());
    }

    @Test
    void testAddItem_IncrementExistingItem() {
        User user = mockUser();

        Menu menu = new Menu();
        menu.setId(1L);
        menu.setPrice(BigDecimal.valueOf(10));

        CartItem existingItem = new CartItem();
        existingItem.setMenu(menu);
        existingItem.setQuantity(2);
        existingItem.setPricePerUnit(BigDecimal.valueOf(10));
        existingItem.setSubtotal(BigDecimal.valueOf(20));

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setCartItems(new ArrayList<>(List.of(existingItem)));

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(menuRepository.findById(1L)).thenReturn(Optional.of(menu));
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));

        CartDTO dto = new CartDTO();
        dto.setMenuId(1L);
        dto.setQuantity(3);

        Response<?> response = cartService.addItemToCart(dto);

        assertEquals(200, response.getStatusCode());
        assertEquals(5, existingItem.getQuantity());  
        assertEquals(BigDecimal.valueOf(50), existingItem.getSubtotal());
    }


    @Test
    void testAddItem_MenuNotFound() {
        CartDTO dto = new CartDTO();
        dto.setMenuId(99L);
        dto.setQuantity(1);

        when(menuRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> cartService.addItemToCart(dto));
    }

    @Test
    void testAddItem_UserNotLoggedIn() {
        CartDTO dto = new CartDTO();
        dto.setMenuId(1L);
        dto.setQuantity(1);

        when(userService.getCurrentLoggedInUser())
                .thenThrow(new NotFoundException("User not found"));

        assertThrows(NotFoundException.class,
                () -> cartService.addItemToCart(dto));
    }

    // ====================incrementItem======================
    @Test
    void testIncrementItem_Success() {
        User user = mockUser();

        Menu menu = new Menu();
        menu.setId(1L);
        menu.setPrice(BigDecimal.valueOf(10));

        CartItem item = new CartItem();
        item.setMenu(menu);
        item.setQuantity(2);
        item.setPricePerUnit(BigDecimal.valueOf(10));
        item.setSubtotal(BigDecimal.valueOf(20));

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setCartItems(List.of(item));

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));

        Response<?> response = cartService.incrementItem(1L);

        assertEquals(200, response.getStatusCode());
        assertEquals(3, item.getQuantity());
        assertEquals(BigDecimal.valueOf(30), item.getSubtotal());
    }

    @Test
    void testIncrementItem_CartNotFound() {
        User user = mockUser();

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> cartService.incrementItem(1L));
    }

    @Test
    void testIncrementItem_ItemNotFound() {
        User user = mockUser();
        Cart cart = mockCart(user, new ArrayList<>());

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));

        assertThrows(NotFoundException.class,
                () -> cartService.incrementItem(99L));
    }

    //=================decrementItem===============
    @Test
    void testDecrementItem_Success() {
        User user = mockUser();

        Menu menu = new Menu();
        menu.setId(1L);
        menu.setPrice(BigDecimal.valueOf(10));

        CartItem item = new CartItem();
        item.setMenu(menu);
        item.setQuantity(3);
        item.setPricePerUnit(BigDecimal.valueOf(10));
        item.setSubtotal(BigDecimal.valueOf(30));

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setCartItems(List.of(item));

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));

        Response<?> response = cartService.decrementItem(1L);

        assertEquals(200, response.getStatusCode());
        assertEquals(2, item.getQuantity());
        assertEquals(BigDecimal.valueOf(20), item.getSubtotal());
    }

    @Test
    void testDecrementItem_RemoveItemWhenZero() {
        User user = mockUser();
        Menu menu = mockMenu(1L, new BigDecimal("10.00"));

        CartItem item = mockCartItem(1L, menu, 1, new BigDecimal("10.00"));
        Cart cart = mockCart(user, new ArrayList<>(List.of(item)));

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));

        Response<?> res = cartService.decrementItem(1L);

        assertEquals(200, res.getStatusCode());
        assertTrue(cart.getCartItems().isEmpty());
    }

    @Test
    void testDecrementItem_CartNotFound() {
        User user = mockUser();

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> cartService.decrementItem(1L));
    }

    @Test
    void testDecrementItem_ItemNotFound() {
        User user = mockUser();
        Cart cart = mockCart(user, new ArrayList<>());

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));

        assertThrows(NotFoundException.class,
                () -> cartService.decrementItem(99L));
    }

    // =================removeItem==============
    @Test
    void testRemoveItem_Success() {
        User user = mockUser();
        Menu menu = mockMenu(1L, new BigDecimal("10.00"));
        CartItem item = mockCartItem(1L, menu, 2, new BigDecimal("20.00"));

        Cart cart = mockCart(user, new ArrayList<>(List.of(item)));

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

        Response<?> res = cartService.removeItem(1L);

        assertEquals(200, res.getStatusCode());
        verify(cartItemRepository).delete(item);
    }

    @Test
    void testRemoveItem_CartNotFound() {
        User user = mockUser();

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> cartService.removeItem(1L));
    }

    @Test
    void testRemoveItem_CartItemNotFound() {
        User user = mockUser();
        Cart cart = mockCart(user, new ArrayList<>());

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> cartService.removeItem(1L));
    }

    @Test
    void testRemoveItem_ItemNotBelongToUserCart() {
        User user = mockUser();
        Menu menu = mockMenu(1L, new BigDecimal("10.00"));

        // item tồn tại nhưng không nằm trong cart
        CartItem otherItem = mockCartItem(1L, menu, 2, new BigDecimal("20.00"));
        Cart cart = mockCart(user, new ArrayList<>()); // cart trống

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(otherItem));

        assertThrows(NotFoundException.class,
                () -> cartService.removeItem(1L));
    }
    // =========getShoppingCart============
    @Test
    void testGetShoppingCart_Success() {
        User user = mockUser();

        Menu menu = mockMenu(1L, new BigDecimal("10.00"));
        CartItem item = mockCartItem(1L, menu, 2, new BigDecimal("20.00"));
        Cart cart = mockCart(user, List.of(item));

        CartDTO cartDTO = new CartDTO();
        cartDTO.setTotalAmount(new BigDecimal("20.00"));

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));
        when(modelMapper.map(cart, CartDTO.class)).thenReturn(cartDTO);

        Response<CartDTO> res = cartService.getShoppingCart();

        assertEquals(200, res.getStatusCode());
        assertEquals(new BigDecimal("20.00"), res.getData().getTotalAmount());
    }

    @Test
    void testGetShoppingCart_CartNotFound() {
        User user = mockUser();

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> cartService.getShoppingCart());
    }

    @Test
    void testGetShoppingCart_NullCartItemsHandled() {
        User user = mockUser();
        Cart cart = mockCart(user, null); // cartItems = null

        CartDTO dto = new CartDTO();
        dto.setTotalAmount(BigDecimal.ZERO);

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));
        when(modelMapper.map(cart, CartDTO.class)).thenReturn(dto);

        Response<CartDTO> res = cartService.getShoppingCart();

        assertEquals(200, res.getStatusCode());
        assertEquals(BigDecimal.ZERO, res.getData().getTotalAmount());
    }

    @Test
    void testGetShoppingCart_TotalAmountCalculatedCorrectly() {
        User user = mockUser();

        Menu m1 = mockMenu(1L, new BigDecimal("10.00"));
        Menu m2 = mockMenu(2L, new BigDecimal("5.00"));

        CartItem i1 = mockCartItem(1L, m1, 2, new BigDecimal("20.00"));
        CartItem i2 = mockCartItem(2L, m2, 3, new BigDecimal("15.00"));

        Cart cart = mockCart(user, List.of(i1, i2));

        CartDTO dto = new CartDTO();
        dto.setTotalAmount(new BigDecimal("35.00"));

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));
        when(modelMapper.map(cart, CartDTO.class)).thenReturn(dto);

        Response<CartDTO> res = cartService.getShoppingCart();

        assertEquals(new BigDecimal("35.00"), res.getData().getTotalAmount());
    }

    @Test
    void testGetShoppingCart_RemoveReviewsInResponse() {
        User user = mockUser();

        Menu menu = new Menu();
        menu.setId(1L);
        menu.setReviews(List.of(new Review())); // input có reviews

        CartItem item = new CartItem();
        item.setMenu(menu);
        item.setSubtotal(BigDecimal.valueOf(10));

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setCartItems(List.of(item));

        CartDTO dto = new CartDTO();

        CartItemDTO itemDTO = new CartItemDTO();
        MenuDTO menuDTO = new MenuDTO();
        menuDTO.setReviews(null); // output phải null
        itemDTO.setMenu(menuDTO);
        dto.setCartItems(List.of(itemDTO));

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));
        when(modelMapper.map(any(Cart.class), eq(CartDTO.class))).thenReturn(dto);

        Response<CartDTO> response = cartService.getShoppingCart();

        assertNull(response.getData().getCartItems().get(0).getMenu().getReviews());
    }


    //============clearShoppingCart=============
    @Test
    void testClearShoppingCart_Success() {
        User user = mockUser();

        Menu menu = mockMenu(1L, new BigDecimal("10.00"));
        CartItem item1 = mockCartItem(1L, menu, 1, new BigDecimal("10.00"));
        CartItem item2 = mockCartItem(2L, menu, 2, new BigDecimal("20.00"));

        List<CartItem> items = new ArrayList<>(List.of(item1, item2));
        Cart cart = mockCart(user, items);

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));

        Response<?> res = cartService.clearShoppingCart();

        verify(cartItemRepository).deleteAll(items);
        verify(cartRepository).save(cart);

        assertEquals(200, res.getStatusCode());
        assertTrue(cart.getCartItems().isEmpty());
    }

    @Test
    void testClearShoppingCart_CartNotFound() {
        User user = mockUser();

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> cartService.clearShoppingCart());
    }

    @Test
    void testClearShoppingCart_EmptyCart_NoError() {
        User user = mockUser();

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setCartItems(new ArrayList<>());

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));

        Response<?> response = cartService.clearShoppingCart();

        assertEquals(200, response.getStatusCode());
        verify(cartItemRepository).deleteAll(anyList()); // CHO PHÉP deleteAll([])
    }

}
