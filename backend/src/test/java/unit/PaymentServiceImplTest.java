package unit;

import com.phegon.FoodApp.order.dtos.OrderDTO;
import com.phegon.FoodApp.order.dtos.OrderItemDTO;
import com.phegon.FoodApp.menu.dtos.MenuDTO;
import com.phegon.FoodApp.review.dtos.ReviewDTO;
import com.phegon.FoodApp.auth_users.dtos.UserDTO;
import com.phegon.FoodApp.payment.dtos.PaymentDTO;
import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.email_notification.dtos.NotificationDTO;
import com.phegon.FoodApp.email_notification.services.NotificationService;
import com.phegon.FoodApp.enums.OrderStatus;
import com.phegon.FoodApp.enums.PaymentGateway;
import com.phegon.FoodApp.enums.PaymentStatus;
import com.phegon.FoodApp.exceptions.BadRequestException;
import com.phegon.FoodApp.exceptions.NotFoundException;
import com.phegon.FoodApp.order.entity.Order;
import com.phegon.FoodApp.order.entity.OrderItem;
import com.phegon.FoodApp.order.repository.OrderRepository;
import com.phegon.FoodApp.payment.entity.Payment;
import com.phegon.FoodApp.payment.repository.PaymentRepository;
import com.phegon.FoodApp.payment.services.PaymentServiceImpl;
import com.phegon.FoodApp.response.Response;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.modelmapper.ModelMapper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.data.domain.Sort;
import java.lang.reflect.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.lang.reflect.Type;
import org.modelmapper.TypeToken;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Order mockOrder;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = openMocks(this);
        mockOrder = mockOrderWithUser();
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    // Helper: Always return Order with User (avoid NPE)
    private Order mockOrderWithUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");

        Order order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setTotalAmount(BigDecimal.valueOf(100));
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setOrderStatus(OrderStatus.INITIALIZED);
        order.setOrderItems(new ArrayList<>());

        return order;
    }

    // A. INITIALIZE PAYMENT — 11 TEST

    @Test
    void testInitializePayment_MissingMetadata() {
        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(100));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {

            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(new IllegalArgumentException("Missing metadata"));

            assertThrows(RuntimeException.class,
                    () -> paymentService.initializePayment(req));
        }
    }

    @Test
    void testInitializePayment_StripeThrows() {
        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(100));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {

            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(new RuntimeException("Stripe error"));

            assertThrows(RuntimeException.class,
                    () -> paymentService.initializePayment(req));
        }
    }

    @Test
    void testInitializePayment_Success() {
        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(100));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {

            PaymentIntent intent = mock(PaymentIntent.class);
            when(intent.getClientSecret()).thenReturn("secret123");

            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(intent);

            Response<?> res = paymentService.initializePayment(req);

            assertEquals(200, res.getStatusCode());
            assertEquals("secret123", res.getData());
        }
    }


    @Test
    void testInitializePayment_OrderNotFound() {
        PaymentDTO req = new PaymentDTO();
        req.setOrderId(99L);

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> paymentService.initializePayment(req));
    }

    @Test
    void testInitializePayment_AlreadyCompleted() {
        mockOrder.setPaymentStatus(PaymentStatus.COMPLETED);

        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(100));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(BadRequestException.class, () -> paymentService.initializePayment(req));
    }

    @Test
    void testInitializePayment_AmountNull() {
        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(null);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(BadRequestException.class, () -> paymentService.initializePayment(req));
    }

    @Test
    void testInitializePayment_AmountNotTally() {
        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(50));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(BadRequestException.class, () -> paymentService.initializePayment(req));
    }

    @Test
    void testInitializePayment_TotalAmountZero() {
        mockOrder.setTotalAmount(BigDecimal.ZERO);

        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.ZERO);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(BadRequestException.class, () -> paymentService.initializePayment(req));
    }



    @Test
    void testInitializePayment_StripeKeyNull() throws Exception {

        // set giá trị secreteKey = null
        Field f = PaymentServiceImpl.class.getDeclaredField("secreteKey");
        f.setAccessible(true);
        f.set(paymentService, null);

        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(100));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {

            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(new RuntimeException("Stripe key missing"));

            assertThrows(RuntimeException.class,
                    () -> paymentService.initializePayment(req));
        }
    }



    @Test
    void testInitializePayment_AmountZero() {
        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.ZERO);

        when(orderRepository.findById(1L)) 
                .thenReturn(Optional.of(mockOrderWithUser()));

        assertThrows(RuntimeException.class, () -> paymentService.initializePayment(req));
    }

    @Test
    void testInitializePayment_InvalidMetadata() throws Exception {

        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(100));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {

            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(new IllegalArgumentException("Invalid metadata"));

            assertThrows(RuntimeException.class,
                    () -> paymentService.initializePayment(req));
        }
    }
    
    // B. UPDATE PAYMENT — 12 TEST

    @Test
    void testUpdatePayment_Success() {
        Order order = mockOrderWithUser();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(order);
        when(templateEngine.process(eq("payment-success"), any())).thenReturn("<html/>");

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(true);
        dto.setAmount(BigDecimal.valueOf(100));

        assertDoesNotThrow(() -> paymentService.updatePaymentForOrder(dto));

        assertEquals(PaymentStatus.COMPLETED, order.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
    }

    @Test
    void testUpdatePayment_Failed() {
        Order order = mockOrderWithUser();

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(false);
        dto.setFailureReason("Card declined");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(order);
        when(templateEngine.process(eq("payment-failed"), any())).thenReturn("<html>FAIL</html>");

        assertDoesNotThrow(() -> paymentService.updatePaymentForOrder(dto));

        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
    }

    @Test
    void testUpdatePayment_OrderNotFound() {
        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(999L);

        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_SavePaymentThrows() {
        Order order = mockOrderWithUser();

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenThrow(new RuntimeException("DB Error"));

        assertThrows(RuntimeException.class,
                () -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_SaveOrderThrows() {
        Order order = mockOrderWithUser();

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenThrow(new RuntimeException("Save error"));

        assertThrows(RuntimeException.class,
                () -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_TemplateError_Success() {
        Order order = mockOrderWithUser();

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(order);

        when(templateEngine.process(eq("payment-success"), any()))
                .thenThrow(new RuntimeException("Template error"));

        assertThrows(RuntimeException.class,
                () -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_EmailFailed_Success() {
        Order order = mockOrderWithUser();

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(order);
        when(templateEngine.process(eq("payment-success"), any())).thenReturn("<html>Ok</html>");

        doThrow(new RuntimeException("Email failed"))
                .when(notificationService).sendEmail(any());

        assertThrows(RuntimeException.class,
                () -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_EmailFailed_Failed() {
        Order order = mockOrderWithUser();

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(false);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(order);
        when(templateEngine.process(eq("payment-failed"), any())).thenReturn("<html>Fail</html>");

        doThrow(new RuntimeException("Email error"))
                .when(notificationService).sendEmail(any());

        assertThrows(RuntimeException.class,
                () -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_Failed_NoFailureReason() {
        Order order = mockOrderWithUser();

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(false);
        dto.setFailureReason(null);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(order);
        when(templateEngine.process(eq("payment-failed"), any())).thenReturn("<html></html>");

        assertDoesNotThrow(() -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_AmountNull() {
        Order order = mockOrderWithUser();

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setAmount(null);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(order);
        when(templateEngine.process(eq("payment-success"), any())).thenReturn("<html></html>");

        assertDoesNotThrow(() -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_TransactionIdNull() {
        Order order = mockOrderWithUser();

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setTransactionId(null);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(order);
        when(templateEngine.process(eq("payment-success"), any())).thenReturn("<html></html>");

        assertDoesNotThrow(() -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_PaymentDateSet() {
        Order order = mockOrderWithUser();

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            assertNotNull(p.getPaymentDate());
            return p;
        });

        when(orderRepository.save(any())).thenReturn(order);
        when(templateEngine.process(anyString(), any())).thenReturn("<html></html>");

        assertDoesNotThrow(() -> paymentService.updatePaymentForOrder(dto));
    }


    // C. GET ALL PAYMENTS — 2 TEST
    @Test
    void testGetAllPayments_Success() {

        // Mock payment list
        Payment p = new Payment();
        List<Payment> list = List.of(p);

        when(paymentRepository.findAll(any(Sort.class))).thenReturn(list);

        // Mock DTO output
        PaymentDTO dto = new PaymentDTO();
        dto.setOrder(new OrderDTO());
        dto.setUser(new UserDTO());

        // ModelMapper mock
        Type expectedType = new TypeToken<List<PaymentDTO>>() {}.getType();

        when(modelMapper.map(eq(list), eq(expectedType)))
                .thenReturn(List.of(dto));

        // Call service
        Response<List<PaymentDTO>> res = paymentService.getAllPayments();

        // Assertions
        assertNotNull(res.getData());
        assertEquals(1, res.getData().size());
        assertNull(res.getData().get(0).getOrder());
        assertNull(res.getData().get(0).getUser());
    }


    @Test
    void testGetAllPayments_Empty() {
        when(paymentRepository.findAll(any(Sort.class))).thenReturn(List.of());

        when(modelMapper.map(any(), any(Type.class)))
                .thenReturn(List.of());

        Response<List<PaymentDTO>> res = paymentService.getAllPayments();

        assertNotNull(res.getData());
        assertEquals(0, res.getData().size());
    }

    // D. GET PAYMENT BY ID — 6 TEST

    @Test
    void testGetPaymentById_Success() {
        Payment p = new Payment();
        p.setOrder(new Order());
        p.setUser(new User());

        OrderItemDTO itemDTO = new OrderItemDTO();
        MenuDTO menuDTO = new MenuDTO();
        menuDTO.setReviews(List.of(new ReviewDTO()));
        itemDTO.setMenu(menuDTO);

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderItems(List.of(itemDTO));

        PaymentDTO dto = new PaymentDTO();
        dto.setOrder(orderDTO);
        dto.setUser(new UserDTO());

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));
        when(modelMapper.map(any(Payment.class), eq(PaymentDTO.class)))
                .thenReturn(dto);

        Response<PaymentDTO> res = paymentService.getPaymentById(1L);

        assertNull(res.getData().getUser().getRoles());
        assertNull(res.getData().getOrder().getUser());
        assertNull(res.getData().getOrder().getOrderItems().get(0).getMenu().getReviews());
    }


    @Test
    void testGetPaymentById_NotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> paymentService.getPaymentById(1L));
    }

    @Test
    void testGetPaymentById_ModelMapperError() {
        Payment payment = new Payment();
        payment.setId(1L);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        when(modelMapper.map(any(), eq(PaymentDTO.class)))
                .thenThrow(new RuntimeException("Mapper error"));

        assertThrows(RuntimeException.class,
                () -> paymentService.getPaymentById(1L));
    }

    @Test
    void testGetPaymentById_UserNull() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setUser(null);
        payment.setOrder(mockOrderWithUser());

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(modelMapper.map(any(), eq(PaymentDTO.class)))
                .thenReturn(new PaymentDTO());

        assertThrows(NullPointerException.class,
                () -> paymentService.getPaymentById(1L));
    }

    @Test
    void testGetPaymentById_OrderNull() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setUser(mockOrderWithUser().getUser());
        payment.setOrder(null);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(modelMapper.map(any(), eq(PaymentDTO.class)))
                .thenReturn(new PaymentDTO());

        assertThrows(NullPointerException.class,
                () -> paymentService.getPaymentById(1L));
    }

    @Test
    void testGetPaymentById_OrderItemsMenuNull() {
        Order order = mockOrderWithUser();

        OrderItem item = new OrderItem();
        item.setMenu(null);

        order.setOrderItems(List.of(item));

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setUser(order.getUser());
        payment.setOrder(order);

        PaymentDTO dto = new PaymentDTO();
        dto.setUser(new com.phegon.FoodApp.auth_users.dtos.UserDTO());
        dto.setOrder(new com.phegon.FoodApp.order.dtos.OrderDTO());

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(modelMapper.map(any(), eq(PaymentDTO.class))).thenReturn(dto);

        assertThrows(NullPointerException.class,
                () -> paymentService.getPaymentById(1L));
    }

}
