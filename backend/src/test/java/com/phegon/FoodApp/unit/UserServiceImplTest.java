// ====== IMPORT & SETUP GIỮ NGUYÊN ======

package com.phegon.FoodApp.unit;

import com.phegon.FoodApp.auth_users.dtos.UserDTO;
import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.auth_users.repository.UserRepository;
import com.phegon.FoodApp.auth_users.services.UserServiceImpl;
import com.phegon.FoodApp.aws.AWSS3Service;
import com.phegon.FoodApp.email_notification.dtos.NotificationDTO;
import com.phegon.FoodApp.email_notification.services.NotificationService;
import com.phegon.FoodApp.exceptions.BadRequestException;
import com.phegon.FoodApp.exceptions.NotFoundException;
import com.phegon.FoodApp.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ModelMapper modelMapper;
    @Mock private NotificationService notificationService;
    @Mock private AWSS3Service awss3Service;

    @InjectMocks private UserServiceImpl userService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockSecurity("test@example.com");
    }

    private void mockSecurity(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private User mockUser() {
        User u = new User();
        u.setId(1L);
        u.setName("User");
        u.setEmail("test@example.com");
        u.setActive(true);
        u.setPhoneNumber("0909999999");
        u.setAddress("City");
        u.setProfileUrl("http://old-image.com/123.jpg");
        return u;
    }

    // 1) TEST getCurrentLoggedInUser

    @Test
    void testGetCurrentLoggedInUser_Success() {
        User user = mockUser();

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        User result = userService.getCurrentLoggedInUser();

        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testGetCurrentLoggedInUser_NotFound() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.getCurrentLoggedInUser());
    }

    @Test
    void testGetCurrentLoggedInUser_Inactive() {
        User user = mockUser();
        user.setActive(false);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(NotFoundException.class,
                () -> userService.getCurrentLoggedInUser());
    }

    // 2) getAllUsers

    @Test
    void testGetAllUsers() {
        User u = mockUser();
        List<User> users = List.of(u);
        List<UserDTO> userDTOs = List.of(new UserDTO());

        when(userRepository.findAll(Sort.by(Sort.Direction.DESC, "id")))
                .thenReturn(users);

        when(modelMapper.map(anyList(), any(java.lang.reflect.Type.class)))
                .thenReturn(userDTOs);

        Response<List<UserDTO>> res = userService.getAllUsers();

        assertEquals(200, res.getStatusCode());
        assertEquals(1, res.getData().size());
    }

    // 3) getOwnAccountDetails

    @Test
    void testGetOwnAccountDetails() {
        User u = mockUser();
        UserDTO dto = new UserDTO();
        dto.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        when(modelMapper.map(u, UserDTO.class)).thenReturn(dto);

        Response<UserDTO> res = userService.getOwnAccountDetails();

        assertEquals(200, res.getStatusCode());
        assertEquals("test@example.com", res.getData().getEmail());
    }

    // 4) updateOwnAccount

    @Test
    void testUpdateOwnAccount_UpdateEmailExists() {
        User u = mockUser();
        UserDTO dto = new UserDTO();
        dto.setEmail("new@example.com");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        when(userRepository.existsByEmail("new@example.com"))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> userService.updateOwnAccount(dto));
    }

    @Test
    void testUpdateOwnAccount_InvalidEmailFormat() {
        User u = mockUser();
        UserDTO dto = new UserDTO();
        dto.setEmail("INVALID");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        assertThrows(BadRequestException.class,
                () -> userService.updateOwnAccount(dto));
    }

    @Test
    void testUpdateOwnAccount_InvalidPhoneNumber_TooShort() {
        User u = mockUser();
        UserDTO dto = new UserDTO();
        dto.setPhoneNumber("09123"); // 5 digits

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        assertThrows(BadRequestException.class,
                () -> userService.updateOwnAccount(dto));
    }

    @Test
    void testUpdateOwnAccount_InvalidPhoneNumber_NotNumeric() {
        User u = mockUser();
        UserDTO dto = new UserDTO();
        dto.setPhoneNumber("09AB123456");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        assertThrows(BadRequestException.class,
                () -> userService.updateOwnAccount(dto));
    }

    @Test
    void testUpdateOwnAccount_PasswordTooShort() {
        User u = mockUser();
        UserDTO dto = new UserDTO();
        dto.setPassword("123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        assertThrows(BadRequestException.class,
                () -> userService.updateOwnAccount(dto));
    }

    @Test
    void testUpdateOwnAccount_EmptyImageUpload() {
        User u = mockUser();
        MultipartFile emptyFile = mock(MultipartFile.class);

        when(emptyFile.isEmpty()).thenReturn(true);

        UserDTO dto = new UserDTO();
        dto.setImageFile(emptyFile);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        assertThrows(BadRequestException.class,
                () -> userService.updateOwnAccount(dto));
    }

        @Test
    void testUpdateOwnAccount_UploadFileNull() {
        User u = mockUser();
        UserDTO dto = new UserDTO();  // imageFile = null → mặc định

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        Response<?> res = userService.updateOwnAccount(dto);

        assertEquals(200, res.getStatusCode());
        verify(awss3Service, never()).uploadFile(anyString(), any());
        verify(awss3Service, never()).deleteFile(anyString());
        verify(userRepository).save(u);
    }

    @Test
    void testUpdateOwnAccount_UpdateProfileImage() throws Exception {
        User u = mockUser();

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("avatar.png");

        UserDTO dto = new UserDTO();
        dto.setImageFile(mockFile);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        URL newURL = new URL("http://new-image.com/new.png");
        when(awss3Service.uploadFile(anyString(), eq(mockFile)))
                .thenReturn(newURL);

        Response<?> res = userService.updateOwnAccount(dto);

        verify(awss3Service).deleteFile(anyString());
        assertEquals(200, res.getStatusCode());
    }

    @Test
    void testUpdateOwnAccount_NoOldImage() throws Exception {
        User u = mockUser();
        u.setProfileUrl(null);

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("avatar.png");

        UserDTO dto = new UserDTO();
        dto.setImageFile(mockFile);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        when(awss3Service.uploadFile(anyString(), eq(mockFile)))
                .thenReturn(new URL("http://new-image.com/img.png"));

        Response<?> res = userService.updateOwnAccount(dto);

        verify(awss3Service, never()).deleteFile(anyString());
        assertEquals(200, res.getStatusCode());
    }

    @Test
    void testUpdateOwnAccount_UpdateNothing() {
        User u = mockUser();
        UserDTO dto = new UserDTO(); // no changes

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        Response<?> res = userService.updateOwnAccount(dto);

        assertEquals(200, res.getStatusCode());
        verify(userRepository).save(u);
    }

    @Test
    void testUpdateOwnAccount_UpdateAllFields() {
        User u = mockUser();
        UserDTO dto = new UserDTO();

        dto.setName("New");
        dto.setAddress("New Address");
        dto.setPhoneNumber("0901111111");
        dto.setEmail("new2@example.com");
        dto.setPassword("NewPassword");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        when(userRepository.existsByEmail("new2@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("NewPassword")).thenReturn("EncodedPWD");

        Response<?> res = userService.updateOwnAccount(dto);

        assertEquals("New", u.getName());
        assertEquals("New Address", u.getAddress());
        assertEquals("0901111111", u.getPhoneNumber());
        assertEquals("new2@example.com", u.getEmail());
        assertEquals("EncodedPWD", u.getPassword());
        assertEquals(200, res.getStatusCode());
    }

    // 5) deactivateOwnAccount

    @Test
    void testDeactivateOwnAccount_Success() {
        User u = mockUser();

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        Response<?> res = userService.deactivateOwnAccount();

        assertEquals(200, res.getStatusCode());
        assertFalse(u.isActive());
        verify(userRepository).save(u);
        verify(notificationService).sendEmail(any(NotificationDTO.class));
    }

    @Test
    void testDeactivateOwnAccount_UserNotFound() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.deactivateOwnAccount());

        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendEmail(any());
    }

    @Test
    void testDeactivateOwnAccount_UserAlreadyInactive() {
        User u = mockUser();
        u.setActive(false);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(u));

        assertThrows(NotFoundException.class,
                () -> userService.deactivateOwnAccount());

        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendEmail(any());
    }
}
