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

    //===========UPDATE PAYMENT FOR ORDER==========//
    @Test
    void testUpdatePayment_Success() {

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setAmount(BigDecimal.valueOf(100));
        dto.setSuccess(true);
        dto.setTransactionId("tx123");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(mockOrder);
        when(templateEngine.process(eq("payment-success"), any())).thenReturn("<html>OK</html>");

        assertDoesNotThrow(() -> paymentService.updatePaymentForOrder(dto));

        assertEquals(PaymentStatus.COMPLETED, mockOrder.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, mockOrder.getOrderStatus());
    }

    @Test
    void testUpdatePayment_Failed() {

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(false);
        dto.setFailureReason("Card declined");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(mockOrder);
        when(templateEngine.process(eq("payment-failed"), any())).thenReturn("<html>FAIL</html>");

        assertDoesNotThrow(() -> paymentService.updatePaymentForOrder(dto));

        assertEquals(PaymentStatus.FAILED, mockOrder.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, mockOrder.getOrderStatus());
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

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.save(any())).thenThrow(new RuntimeException("DB Error"));

        assertThrows(RuntimeException.class,
                () -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_SaveOrderThrows() {

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenThrow(new RuntimeException("Save error"));

        assertThrows(RuntimeException.class,
                () -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_TemplateError_Success() {

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(mockOrder);

        when(templateEngine.process(eq("payment-success"), any()))
                .thenThrow(new RuntimeException("Template error"));

        assertThrows(RuntimeException.class,
                () -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_EmailFailed_Success() {

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(mockOrder);
        when(templateEngine.process(eq("payment-success"), any())).thenReturn("<html>Ok</html>");

        doThrow(new RuntimeException("Email failed"))
                .when(notificationService).sendEmail(any());

        assertThrows(RuntimeException.class,
                () -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_EmailFailed_Failed() {

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(false);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(mockOrder);
        when(templateEngine.process(eq("payment-failed"), any())).thenReturn("<html>Fail</html>");

        doThrow(new RuntimeException("Email error"))
                .when(notificationService).sendEmail(any());

        assertThrows(RuntimeException.class,
                () -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_Failed_NoFailureReason() {

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(false);
        dto.setFailureReason(null);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(mockOrder);
        when(templateEngine.process(eq("payment-failed"), any())).thenReturn("<html></html>");

        assertDoesNotThrow(() -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_AmountNull() {

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setAmount(null);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(mockOrder);
        when(templateEngine.process(eq("payment-success"), any())).thenReturn("<html></html>");

        assertDoesNotThrow(() -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_TransactionIdNull() {

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setTransactionId(null);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.save(any())).thenReturn(new Payment());
        when(orderRepository.save(any())).thenReturn(mockOrder);
        when(templateEngine.process(eq("payment-success"), any())).thenReturn("<html></html>");

        assertDoesNotThrow(() -> paymentService.updatePaymentForOrder(dto));
    }

    @Test
    void testUpdatePayment_PaymentDateSet() {

        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setSuccess(true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);

        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            assertNotNull(p.getPaymentDate());
            return p;
        });

        when(orderRepository.save(any())).thenReturn(mockOrder);
        when(templateEngine.process(anyString(), any())).thenReturn("<html></html>");

        assertDoesNotThrow(() -> paymentService.updatePaymentForOrder(dto));
    }

}
