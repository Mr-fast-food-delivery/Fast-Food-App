package com.phegon.FoodApp.payment.services;

import com.phegon.FoodApp.email_notification.dtos.NotificationDTO;
import com.phegon.FoodApp.email_notification.services.NotificationService;
import com.phegon.FoodApp.enums.OrderStatus;
import com.phegon.FoodApp.enums.PaymentStatus;
import com.phegon.FoodApp.exceptions.BadRequestException;
import com.phegon.FoodApp.exceptions.NotFoundException;
import com.phegon.FoodApp.order.entity.Order;
import com.phegon.FoodApp.order.entity.OrderItem;
import com.phegon.FoodApp.order.repository.OrderRepository;
import com.phegon.FoodApp.payment.dtos.PaymentDTO;
import com.phegon.FoodApp.payment.entity.Payment;
import com.phegon.FoodApp.payment.repository.PaymentRepository;
import com.phegon.FoodApp.response.Response;
import com.phegon.FoodApp.menu.entity.Menu;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        paymentService = new PaymentServiceImpl(
                paymentRepository,
                notificationService,
                orderRepository,
                templateEngine,
                modelMapper
        );

        // Set Stripe Key để tránh null
        TestUtils.setField(paymentService, "secreteKey", "sk_test_123");
        TestUtils.setField(paymentService, "frontendBaseUrl", "http://localhost:3000");

        mockOrder = new Order();
        mockOrder.setId(1L);
        mockOrder.setTotalAmount(BigDecimal.valueOf(100));
        mockOrder.setPaymentStatus(PaymentStatus.PENDING);
    }

    // =================INITIALIZE PAYMENT=======================
    @Test
    void testInitializePayment_Success() throws Exception {

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setAmount(BigDecimal.valueOf(100));

        Order order = new Order();
        order.setId(1L);
        order.setTotalAmount(BigDecimal.valueOf(100));
        order.setPaymentStatus(PaymentStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        try (MockedStatic<PaymentIntent> mockedPaymentIntent = Mockito.mockStatic(PaymentIntent.class)) {

            PaymentIntent fakeIntent = mock(PaymentIntent.class);
            when(fakeIntent.getClientSecret()).thenReturn("secret_123");

            mockedPaymentIntent.when(
                    () -> PaymentIntent.create(any(PaymentIntentCreateParams.class))
            ).thenReturn(fakeIntent);

            Response<?> res = paymentService.initializePayment(dto);

            assertEquals(200, res.getStatusCode());
            assertEquals("secret_123", res.getData());
        }
    }


    @Test
    void testInitializePayment_OrderNotFound() {
        PaymentDTO req = new PaymentDTO();
        req.setOrderId(100L);

        when(orderRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> paymentService.initializePayment(req));
    }   

    @Test
    void testInitializePayment_AlreadyCompleted() {
        mockOrder.setPaymentStatus(PaymentStatus.COMPLETED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(100));

        assertThrows(BadRequestException.class,
                () -> paymentService.initializePayment(req));
    }

    @Test
    void testInitializePayment_AmountNull() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(null);

        assertThrows(BadRequestException.class,
                () -> paymentService.initializePayment(req));
    }

    @Test
    void testInitializePayment_AmountNotTally() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(50));

        assertThrows(BadRequestException.class,
                () -> paymentService.initializePayment(req));
    }

    @Test
    void testInitializePayment_StripeThrowsException() {

        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(100));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        try (MockedStatic<PaymentIntent> mocked = Mockito.mockStatic(PaymentIntent.class)) {

            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(new RuntimeException("Stripe error"));

            assertThrows(RuntimeException.class,
                    () -> paymentService.initializePayment(req));
        }
    }

    @Test
    void testInitializePayment_StripeKeyNull() {
        TestUtils.setField(paymentService, "secreteKey", null);

        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(100));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(Exception.class,
                () -> paymentService.initializePayment(req));
    }

    @Test
    void testInitializePayment_StripeKeyEmpty() {
        TestUtils.setField(paymentService, "secreteKey", "");

        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(100));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(Exception.class,
                () -> paymentService.initializePayment(req));
    }

    @Test
    void testInitializePayment_AmountZero() {
        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.ZERO);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(RuntimeException.class,
                () -> paymentService.initializePayment(req));
    }

    @Test
    void testInitializePayment_TotalAmountZero() {
        mockOrder.setTotalAmount(BigDecimal.ZERO);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.ZERO);

        assertThrows(BadRequestException.class,
                () -> paymentService.initializePayment(req));
    }

    @Test
    void testInitializePayment_MissingMetadata() {

        PaymentDTO req = new PaymentDTO();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.valueOf(100));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        try (MockedStatic<PaymentIntent> mocked = Mockito.mockStatic(PaymentIntent.class)) {

            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(new IllegalArgumentException("Missing metadata"));

            assertThrows(RuntimeException.class,
                    () -> paymentService.initializePayment(req));
        }
    }
}
