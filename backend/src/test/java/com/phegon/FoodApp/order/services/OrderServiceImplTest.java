package com.phegon.FoodApp.order.services;

import java.lang.reflect.Method;
import com.phegon.FoodApp.auth_users.dtos.UserDTO;
import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.auth_users.services.UserService;
import com.phegon.FoodApp.cart.entity.Cart;
import com.phegon.FoodApp.cart.entity.CartItem;
import com.phegon.FoodApp.cart.repository.CartRepository;
import com.phegon.FoodApp.cart.services.CartService;
import com.phegon.FoodApp.email_notification.dtos.NotificationDTO;
import com.phegon.FoodApp.email_notification.services.NotificationService;
import com.phegon.FoodApp.enums.OrderStatus;
import com.phegon.FoodApp.enums.PaymentStatus;
import com.phegon.FoodApp.exceptions.BadRequestException;
import com.phegon.FoodApp.exceptions.NotFoundException;
import com.phegon.FoodApp.menu.dtos.MenuDTO;
import com.phegon.FoodApp.menu.entity.Menu;
import com.phegon.FoodApp.order.dtos.OrderDTO;
import com.phegon.FoodApp.order.dtos.OrderItemDTO;
import com.phegon.FoodApp.order.entity.Order;
import com.phegon.FoodApp.order.entity.OrderItem;
import com.phegon.FoodApp.order.repository.OrderItemRepository;
import com.phegon.FoodApp.order.repository.OrderRepository;
import com.phegon.FoodApp.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private CartService cartService;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User mockUser;
    private Cart mockCart;

    @BeforeEach
    void init() throws Exception {
        mockUser = buildUser(1L, "John Doe", "john@example.com", "123 Street");
        mockCart = mockCartWithItemsWithItems(mockUser);

        // set basePaymentLink via reflection
        Field f = OrderServiceImpl.class.getDeclaredField("basePaymentLink");
        f.setAccessible(true);
        f.set(orderService, "https://pay.test?orderId=");
    }

    // ===== Helper methods =====

    private User buildUser(Long id, String name, String email, String address) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setEmail(email);
        u.setAddress(address);
        return u;
    }

    private Cart mockCartWithItemsWithItems(User user) {
        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(user);

        Menu menu1 = new Menu();
        menu1.setId(1L);
        menu1.setName("Burger");
        menu1.setPrice(BigDecimal.valueOf(10));

        Menu menu2 = new Menu();
        menu2.setId(2L);
        menu2.setName("Pizza");
        menu2.setPrice(BigDecimal.valueOf(20));

        CartItem item1 = new CartItem();
        item1.setId(100L);
        item1.setMenu(menu1);
        item1.setQuantity(1);
        item1.setPricePerUnit(BigDecimal.valueOf(10));
        item1.setSubtotal(BigDecimal.valueOf(10));

        CartItem item2 = new CartItem();
        item2.setId(200L);
        item2.setMenu(menu2);
        item2.setQuantity(2);
        item2.setPricePerUnit(BigDecimal.valueOf(20));
        item2.setSubtotal(BigDecimal.valueOf(40));

        cart.setCartItems(Arrays.asList(item1, item2));
        return cart;
    }

    private OrderDTO buildOrderDTOFromOrder(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setPaymentStatus(order.getPaymentStatus());

        UserDTO userDTO = new UserDTO();
        userDTO.setId(order.getUser().getId());
        userDTO.setName(order.getUser().getName());
        userDTO.setEmail(order.getUser().getEmail());
        userDTO.setAddress(order.getUser().getAddress());
        dto.setUser(userDTO);

        List<OrderItemDTO> itemDTOs = new ArrayList<>();
        for (OrderItem oi : order.getOrderItems()) {
            OrderItemDTO oidto = new OrderItemDTO();
            oidto.setId(oi.getId());
            oidto.setQuantity(oi.getQuantity());
            oidto.setPricePerUnit(oi.getPricePerUnit());
            oidto.setSubtotal(oi.getSubtotal());

            MenuDTO m = new MenuDTO();
            m.setId(oi.getMenu().getId());
            m.setName(oi.getMenu().getName());
            oidto.setMenu(m);

            itemDTOs.add(oidto);
        }
        dto.setOrderItems(itemDTOs);
        return dto;
    }

    // ==== TEST HELPERS ==================================================== //

    private User mockUserWithAddress() {
        User user = new User();
        user.setId(1L);
        user.setName("Huy");
        user.setEmail("huy@test.com");
        user.setAddress("Ho Chi Minh");
        return user;
    }

    private Cart mockCartWithItems() {
        Cart cart = new Cart();

        Menu menu = new Menu();
        menu.setId(10L);
        menu.setName("Pizza");

        CartItem item = new CartItem();
        item.setMenu(menu);
        item.setQuantity(2);
        item.setPricePerUnit(BigDecimal.valueOf(50));
        item.setSubtotal(BigDecimal.valueOf(100));

        cart.setCartItems(List.of(item));
        return cart;
    }

    private Order mockSaveOrder() {
        Order order = new Order();
        order.setId(999L);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(BigDecimal.valueOf(100));
        return order;
    }

    private OrderDTO mockFullOrderDTO() {
        OrderDTO dto = new OrderDTO();

        dto.setId(1L);
        dto.setOrderDate(LocalDateTime.now());
        dto.setTotalAmount(BigDecimal.valueOf(100));

        // User
        UserDTO userDTO = new UserDTO();
        userDTO.setAddress("HCM");
        dto.setUser(userDTO);

        // Items
        MenuDTO menu = new MenuDTO();
        menu.setName("Pizza");

        OrderItemDTO item = new OrderItemDTO();
        item.setQuantity(2);
        item.setSubtotal(BigDecimal.valueOf(50));
        item.setMenu(menu);

        dto.setOrderItems(List.of(item));

        return dto;
    }
    // =========================================================
    // A. placeOrderFromCart()
    // =========================================================
    @Nested
    class PlaceOrderFromCartTests {
        
        @Test
        void testPlaceOrder_Success() {

            mockUser.setAddress("HN");

            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId())).thenReturn(Optional.of(mockCartWithItems()));

            lenient().when(orderRepository.save(any())).thenReturn(mockSaveOrder());
            lenient().when(orderItemRepository.saveAll(any())).thenReturn(List.of());

            when(modelMapper.map(any(Order.class), eq(OrderDTO.class)))
                    .thenReturn(mockFullOrderDTO());

            when(templateEngine.process(eq("order-confirmation"), any()))
                    .thenReturn("<html/>");

            Response<?> res = orderService.placeOrderFromCart();

            assertEquals(200, res.getStatusCode());
            verify(cartService, times(1)).clearShoppingCart();
            verify(notificationService, times(1)).sendEmail(any());
        }

        @Test
        void testPlaceOrder_UserHasNoAddress() {
            User u = buildUser(2L, "NoAddr", "noaddr@example.com", null);
            when(userService.getCurrentLoggedInUser()).thenReturn(u);

            assertThrows(NotFoundException.class,
                    () -> orderService.placeOrderFromCart());

            verifyNoInteractions(cartRepository);
        }

        @Test
        void testPlaceOrder_CartNotFound() {
            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId()))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> orderService.placeOrderFromCart());
        }

        @Test
        void testPlaceOrder_CartItemsNull() {
            Cart cart = new Cart();
            cart.setUser(mockUser);
            cart.setCartItems(null);

            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId()))
                    .thenReturn(Optional.of(cart));

            assertThrows(BadRequestException.class,
                    () -> orderService.placeOrderFromCart());
        }

        @Test
        void testPlaceOrder_CartItemsEmpty() {
            Cart cart = new Cart();
            cart.setUser(mockUser);
            cart.setCartItems(Collections.emptyList());

            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId()))
                    .thenReturn(Optional.of(cart));

            assertThrows(BadRequestException.class,
                    () -> orderService.placeOrderFromCart());
        }

        @Test
        void testPlaceOrder_OrderItemsBuildCorrectly() {
            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId())).thenReturn(Optional.of(mockCart));

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(orderCaptor.capture()))
                    .thenAnswer(inv -> {
                        Order o = inv.getArgument(0);
                        o.setId(100L);
                        return o;
                    });

            when(orderItemRepository.saveAll(anyList()))
                    .thenAnswer(inv -> inv.getArgument(0));

            when(modelMapper.map(any(Order.class), eq(OrderDTO.class)))
                    .thenAnswer(inv -> buildOrderDTOFromOrder((Order) inv.getArgument(0)));
            when(templateEngine.process(eq("order-confirmation"), any(Context.class)))
                    .thenReturn("<html></html>");

            orderService.placeOrderFromCart();

            Order captured = orderCaptor.getValue();
            assertNotNull(captured);
            assertEquals(2, captured.getOrderItems().size());
            assertEquals(BigDecimal.valueOf(50), captured.getTotalAmount());
        }

        @Test
        void testPlaceOrder_OrderSavedOnce() {

            mockUser.setAddress("HN");

            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId())).thenReturn(Optional.of(mockCartWithItems()));

            lenient().when(orderRepository.save(any())).thenReturn(mockSaveOrder());
            lenient().when(orderItemRepository.saveAll(any())).thenReturn(List.of());

            when(modelMapper.map(any(Order.class), eq(OrderDTO.class)))
                    .thenReturn(mockFullOrderDTO());

            when(templateEngine.process(anyString(), any())).thenReturn("<html/>");

            orderService.placeOrderFromCart();

            verify(orderRepository, times(1)).save(any());
        }

        @Test
        void testPlaceOrder_OrderItemsSavedCorrectly() {
            mockUser.setAddress("HN");

            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId())).thenReturn(Optional.of(mockCartWithItems()));

            lenient().when(orderRepository.save(any())).thenReturn(mockSaveOrder());
            lenient().when(orderItemRepository.saveAll(any())).thenReturn(List.of());

            when(modelMapper.map(any(Order.class), eq(OrderDTO.class)))
                    .thenReturn(mockFullOrderDTO());

            when(templateEngine.process(anyString(), any())).thenReturn("<html/>");

            orderService.placeOrderFromCart();

            verify(orderItemRepository, times(1)).saveAll(any());
        }

        @Test
        void testPlaceOrder_ClearCartCalled() {
            mockUser.setAddress("HN");

            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId())).thenReturn(Optional.of(mockCartWithItems()));

            lenient().when(orderRepository.save(any())).thenReturn(mockSaveOrder());
            lenient().when(orderItemRepository.saveAll(any())).thenReturn(List.of());

            when(modelMapper.map(any(Order.class), eq(OrderDTO.class)))
                    .thenReturn(mockFullOrderDTO());

            when(templateEngine.process(anyString(), any())).thenReturn("<html/>");

            orderService.placeOrderFromCart();

            verify(cartService, times(1)).clearShoppingCart();
        }

        @Test
        void testPlaceOrder_ModelMapperError() {
            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId())).thenReturn(Optional.of(mockCart));

            when(orderRepository.save(any(Order.class))).thenReturn(new Order());
            when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(modelMapper.map(any(Order.class), eq(OrderDTO.class)))
                    .thenThrow(new RuntimeException("Mapper error"));

            assertThrows(RuntimeException.class,
                    () -> orderService.placeOrderFromCart());
        }

        @Test
        void testPlaceOrder_OrderRepositorySaveThrows() {
            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId())).thenReturn(Optional.of(mockCart));

            when(orderRepository.save(any(Order.class)))
                    .thenThrow(new RuntimeException("DB error"));

            assertThrows(RuntimeException.class,
                    () -> orderService.placeOrderFromCart());
        }

        @Test
        void testPlaceOrder_OrderItemsSaveThrows() {
            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId())).thenReturn(Optional.of(mockCart));

            when(orderRepository.save(any(Order.class))).thenReturn(new Order());
            when(orderItemRepository.saveAll(anyList()))
                    .thenThrow(new RuntimeException("SaveAll error"));

            assertThrows(RuntimeException.class,
                    () -> orderService.placeOrderFromCart());
        }

        @Test
        void testPlaceOrder_ClearCartThrows() {
            User user = mockUserWithAddress();
            Cart cart = mockCartWithItems();
            Order mockSaveOrder = mockSaveOrder();

            when(userService.getCurrentLoggedInUser()).thenReturn(user);
            when(cartRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(cart));

            // Only necessary stub before clearShoppingCart() is called
            when(orderRepository.save(any())).thenReturn(mockSaveOrder);

            // This is where the test expects error
            doThrow(new RuntimeException("clear error"))
                    .when(cartService).clearShoppingCart();

            assertThrows(RuntimeException.class, () -> orderService.placeOrderFromCart());

            verify(cartService, times(1)).clearShoppingCart();
        }


        @Test
        void testPlaceOrder_SendEmailCalled() {
            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId())).thenReturn(Optional.of(mockCart));

            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(inv -> {
                        Order o = inv.getArgument(0);
                        o.setId(123L);
                        return o;
                    });
            when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(modelMapper.map(any(Order.class), eq(OrderDTO.class)))
                    .thenAnswer(inv -> buildOrderDTOFromOrder((Order) inv.getArgument(0)));
            when(templateEngine.process(eq("order-confirmation"), any(Context.class)))
                    .thenReturn("<html>Email</html>");

            orderService.placeOrderFromCart();

            ArgumentCaptor<NotificationDTO> captor = ArgumentCaptor.forClass(NotificationDTO.class);
            verify(notificationService, times(1)).sendEmail(captor.capture());

            NotificationDTO sent = captor.getValue();
            assertEquals(mockUser.getEmail(), sent.getRecipient());
            assertTrue(sent.getSubject().contains("Order #123"));
        }

       @Test
        void testPlaceOrder_SendEmailThrows() {

            mockUser.setAddress("HN");

            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId())).thenReturn(Optional.of(mockCartWithItems()));

            lenient().when(orderRepository.save(any())).thenReturn(mockSaveOrder());
            lenient().when(orderItemRepository.saveAll(any())).thenReturn(List.of());

            when(modelMapper.map(any(Order.class), eq(OrderDTO.class)))
                    .thenReturn(mockFullOrderDTO());

            when(templateEngine.process(anyString(), any()))
                    .thenReturn("<html/>");

            doThrow(new RuntimeException("Email failed"))
                    .when(notificationService).sendEmail(any());

            assertThrows(RuntimeException.class,
                    () -> orderService.placeOrderFromCart());
        }

        @Test
        void testPlaceOrder_PaymentLinkCorrectInEmail() {
            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(cartRepository.findByUser_Id(mockUser.getId())).thenReturn(Optional.of(mockCart));

            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(inv -> {
                        Order o = inv.getArgument(0);
                        o.setId(777L);
                        return o;
                    });
            when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            when(modelMapper.map(any(Order.class), eq(OrderDTO.class)))
                    .thenAnswer(inv -> buildOrderDTOFromOrder((Order) inv.getArgument(0)));

            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("order-confirmation"), contextCaptor.capture()))
                    .thenReturn("<html></html>");

            orderService.placeOrderFromCart();

            Context ctx = contextCaptor.getValue();
            String paymentLink = (String) ctx.getVariable("paymentLink");
            assertNotNull(paymentLink);
            assertTrue(paymentLink.startsWith("https://pay.test?orderId="));
            assertTrue(paymentLink.contains("777"));
        }
    }

    // =========================================================
    // B. getOrderById()
    // =========================================================
    @Nested
    class GetOrderByIdTests {

        @Test
        void testGetOrderById_Success() {
            Order order = new Order();
            order.setId(1L);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            OrderDTO dto = new OrderDTO();
            dto.setId(1L);
            when(modelMapper.map(order, OrderDTO.class)).thenReturn(dto);

            Response<OrderDTO> res = orderService.getOrderById(1L);

            assertEquals(200, res.getStatusCode());
            assertEquals(1L, res.getData().getId());
        }

        @Test
        void testGetOrderById_NotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> orderService.getOrderById(999L));
        }

        @Test
        void testGetOrderById_ModelMapperError() {
            Order order = new Order();
            order.setId(2L);
            when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
            when(modelMapper.map(order, OrderDTO.class))
                    .thenThrow(new RuntimeException("mapper error"));

            assertThrows(RuntimeException.class,
                    () -> orderService.getOrderById(2L));
        }
    }

    // =========================================================
    // C. getAllOrders()
    // =========================================================
    @Nested
    class GetAllOrdersTests {

        @Test
        void testGetAllOrders_NoStatus() {
            Order o = new Order();
            o.setOrderItems(new ArrayList<>());

            Page<Order> page = new PageImpl<>(List.of(o));

            when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);

            OrderDTO mapped = new OrderDTO();
            mapped.setOrderItems(List.of(new OrderItemDTO(){{
                setMenu(new MenuDTO());
            }}));

            when(modelMapper.map(any(Order.class), eq(OrderDTO.class)))
                    .thenReturn(mapped);

            Response<Page<OrderDTO>> res = orderService.getAllOrders(null, 0, 10);
            assertEquals(200, res.getStatusCode());
        }

        @Test
        void testGetAllOrders_WithStatus() {
            Order o = new Order();
            o.setOrderItems(new ArrayList<>());

            Page<Order> page = new PageImpl<>(List.of(o));

            when(orderRepository.findByOrderStatus(eq(OrderStatus.INITIALIZED), any(Pageable.class)))
                    .thenReturn(page);

            OrderDTO mapped = new OrderDTO();
            mapped.setOrderItems(List.of(new OrderItemDTO(){{
                setMenu(new MenuDTO());
            }}));

            when(modelMapper.map(any(Order.class), eq(OrderDTO.class)))
                    .thenReturn(mapped);

            Response<Page<OrderDTO>> res = orderService.getAllOrders(OrderStatus.INITIALIZED, 0, 10);
            assertEquals(200, res.getStatusCode());
        }

        @Test
        void testGetAllOrders_OrderItemsMenuReviewsSetNull() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
            Order order = new Order();
            order.setId(3L);

            Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.findAll(pageable)).thenReturn(page);

            OrderDTO dto = new OrderDTO();
            dto.setId(3L);

            MenuDTO menuDTO = new MenuDTO();
            menuDTO.setReviews(new ArrayList<>());

            OrderItemDTO itemDTO = new OrderItemDTO();
            itemDTO.setMenu(menuDTO);
            dto.setOrderItems(List.of(itemDTO));

            when(modelMapper.map(order, OrderDTO.class)).thenReturn(dto);

            Response<Page<OrderDTO>> res = orderService.getAllOrders(null, 0, 10);

            List<OrderDTO> content = res.getData().getContent();
            assertEquals(1, content.size());
            assertNull(content.get(0).getOrderItems().get(0).getMenu().getReviews());
        }

         @Test
        void testGetAllOrders_ModelMapperError() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

            Order order = new Order();
            order.setId(10L);

            Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.findAll(pageable)).thenReturn(page);

            when(modelMapper.map(order, OrderDTO.class))
                    .thenThrow(new RuntimeException("Mapping failed"));

            assertThrows(RuntimeException.class,
                    () -> orderService.getAllOrders(null, 0, 10));
        }
    }

    // =========================================================
    // D. getOrdersOfUser()
    // =========================================================
    @Nested
    class GetOrdersOfUserTests {

        @Test
        void testGetOrdersOfUser_Success() {

            Order o = new Order();
            o.setOrderItems(new ArrayList<>());

            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(orderRepository.findByUserOrderByOrderDateDesc(mockUser))
                    .thenReturn(List.of(o));

            OrderDTO mapped = new OrderDTO();
            mapped.setOrderItems(List.of(new OrderItemDTO(){{
                setMenu(new MenuDTO());
            }}));

            when(modelMapper.map(any(Order.class), eq(OrderDTO.class)))
                    .thenReturn(mapped);

            Response<List<OrderDTO>> res = orderService.getOrdersOfUser();

            assertEquals(200, res.getStatusCode());
            assertEquals(1, res.getData().size());
            assertNull(res.getData().get(0).getUser());
        }

        @Test
        void testGetOrdersOfUser_EmptyList() {

            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);
            when(orderRepository.findByUserOrderByOrderDateDesc(mockUser))
                    .thenReturn(Collections.emptyList());

            Response<List<OrderDTO>> res = orderService.getOrdersOfUser();

            assertEquals(200, res.getStatusCode());
            assertNotNull(res.getData());
            assertTrue(res.getData().isEmpty());
        }

        @Test
        void testGetOrdersOfUser_ModelMapperError() {

            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);

            Order order = new Order();
            order.setId(2L);

            when(orderRepository.findByUserOrderByOrderDateDesc(mockUser))
                    .thenReturn(List.of(order));

            when(modelMapper.map(order, OrderDTO.class))
                    .thenThrow(new RuntimeException("ModelMapper error"));

            assertThrows(RuntimeException.class,
                    () -> orderService.getOrdersOfUser());
        }

        @Test
        void testGetOrdersOfUser_RemoveUserAndReviews() {

            when(userService.getCurrentLoggedInUser()).thenReturn(mockUser);

            // Fake order
            Order order = new Order();
            order.setId(3L);
            order.setUser(mockUser);

            Menu menu = new Menu();
            menu.setReviews(List.of()); // raw entity

            OrderItem orderItem = new OrderItem();
            orderItem.setMenu(menu);
            order.setOrderItems(List.of(orderItem));

            when(orderRepository.findByUserOrderByOrderDateDesc(mockUser))
                    .thenReturn(List.of(order));

            // DTO
            OrderDTO dto = new OrderDTO();
            dto.setId(3L);
            dto.setUser(new UserDTO());

            OrderItemDTO itemDTO = new OrderItemDTO();
            MenuDTO menuDTO = new MenuDTO();
            menuDTO.setReviews(List.of(new com.phegon.FoodApp.review.dtos.ReviewDTO()));
            itemDTO.setMenu(menuDTO);

            dto.setOrderItems(List.of(itemDTO));

            when(modelMapper.map(order, OrderDTO.class)).thenReturn(dto);

            Response<List<OrderDTO>> res = orderService.getOrdersOfUser();

            OrderDTO returned = res.getData().get(0);

            assertNull(returned.getUser());
            assertNull(returned.getOrderItems().get(0).getMenu().getReviews());
        }
    }

        // =========================================================
    // E. getOrderItemById()
    // =========================================================
    @Nested
    class GetOrderItemByIdTests {

        @Test
        void testGetOrderItemById_Success() {

            Menu menu = new Menu();
            menu.setId(1L);

            OrderItem item = new OrderItem();
            item.setId(10L);
            item.setMenu(menu);

            when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

            OrderItemDTO dto = new OrderItemDTO();
            dto.setId(10L);
            dto.setMenu(new MenuDTO());

            when(modelMapper.map(item, OrderItemDTO.class)).thenReturn(dto);
            when(modelMapper.map(menu, MenuDTO.class)).thenReturn(new MenuDTO());

            Response<OrderItemDTO> res = orderService.getOrderItemById(10L);

            assertEquals(200, res.getStatusCode());
            assertEquals(10L, res.getData().getId());
            assertNotNull(res.getData().getMenu());
        }

        @Test
        void testGetOrderItemById_NotFound() {

            when(orderItemRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> orderService.getOrderItemById(99L));
        }

        @Test
        void testGetOrderItemById_ModelMapperError() {

            Menu menu = new Menu();
            OrderItem item = new OrderItem();
            item.setMenu(menu);

            when(orderItemRepository.findById(5L)).thenReturn(Optional.of(item));

            when(modelMapper.map(item, OrderItemDTO.class))
                    .thenThrow(new RuntimeException("Mapping error"));

            assertThrows(RuntimeException.class,
                    () -> orderService.getOrderItemById(5L));
        }

        @Test
        void testGetOrderItemById_MenuMappingCorrect() {

            Menu menu = new Menu();
            menu.setId(1L);

            OrderItem item = new OrderItem();
            item.setMenu(menu);

            when(orderItemRepository.findById(1L)).thenReturn(Optional.of(item));

            OrderItemDTO dto = new OrderItemDTO();
            dto.setMenu(new MenuDTO());

            when(modelMapper.map(item, OrderItemDTO.class)).thenReturn(dto);
            when(modelMapper.map(menu, MenuDTO.class)).thenReturn(new MenuDTO());

            Response<OrderItemDTO> res = orderService.getOrderItemById(1L);

            assertNotNull(res.getData().getMenu());
        }
    }


    // =========================================================
    // F. updateOrderStatus()
    // =========================================================
    @Nested
    class UpdateOrderStatusTests {

        @Test
        void testUpdateOrderStatus_Success() {

            OrderDTO dto = new OrderDTO();
            dto.setId(1L);
            dto.setOrderStatus(OrderStatus.ON_THE_WAY);

            Order order = new Order();
            order.setId(1L);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            Response<OrderDTO> res = orderService.updateOrderStatus(dto);

            verify(orderRepository, times(1)).save(order);
            assertEquals(200, res.getStatusCode());
        }

        @Test
        void testUpdateOrderStatus_OrderNotFound() {

            OrderDTO dto = new OrderDTO();
            dto.setId(999L);

            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> orderService.updateOrderStatus(dto));
        }

        @Test
        void testUpdateOrderStatus_SaveThrows() {

            OrderDTO dto = new OrderDTO();
            dto.setId(1L);
            dto.setOrderStatus(OrderStatus.CANCELLED);

            Order order = new Order();

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any()))
                    .thenThrow(new RuntimeException("DB Error"));

            assertThrows(RuntimeException.class,
                    () -> orderService.updateOrderStatus(dto));
        }
    }


    // =========================================================
    // G. countUniqueCustomers()
    // =========================================================
    @Nested
    class CountUniqueCustomersTests {

        @Test
        void testCountUniqueCustomers_Success() {

            when(orderRepository.countDistinctUsers()).thenReturn(7L);

            Response<Long> res = orderService.countUniqueCustomers();

            assertEquals(200, res.getStatusCode());
            assertEquals(7L, res.getData());
        }

        @Test
        void testCountUniqueCustomers_DBError() {

            when(orderRepository.countDistinctUsers())
                    .thenThrow(new RuntimeException("DB Failure"));

            assertThrows(RuntimeException.class,
                    () -> orderService.countUniqueCustomers());
        }
    }


    // =========================================================
    // H. Order Email Tests (indirect through placeOrderFromCart)
    // =========================================================
    @Nested
    class OrderEmailTests {

        @Test
        void testOrderEmail_TemplateError() throws Exception {
            User user = mockUserWithAddress();
            OrderDTO dto = mockFullOrderDTO();

            when(templateEngine.process(eq("order-confirmation"), any()))
                    .thenThrow(new RuntimeException("template error"));

            Method m = OrderServiceImpl.class
                    .getDeclaredMethod("sendOrderConfirmationEmail", User.class, OrderDTO.class);
            m.setAccessible(true);

            InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                    () -> m.invoke(orderService, user, dto));

            assertTrue(ex.getCause() instanceof RuntimeException);
            assertEquals("template error", ex.getCause().getMessage());
        }

        @Test
        void testOrderEmail_Success() throws Exception {
            User user = mockUserWithAddress();
            OrderDTO dto = mockFullOrderDTO();

            // template return hợp lệ
            when(templateEngine.process(eq("order-confirmation"), any()))
                    .thenReturn("<html>Email OK</html>");

            doNothing().when(notificationService).sendEmail(any());

            Method m = OrderServiceImpl.class
                    .getDeclaredMethod("sendOrderConfirmationEmail", User.class, OrderDTO.class);

            m.setAccessible(true);

            assertDoesNotThrow(() -> m.invoke(orderService, user, dto));
        }
    }
}
