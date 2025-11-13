package com.phegon.FoodApp.auth_users.services;


import com.phegon.FoodApp.auth_users.dtos.LoginRequest;
import com.phegon.FoodApp.auth_users.dtos.LoginResponse;
import com.phegon.FoodApp.auth_users.dtos.RegistrationRequest;
import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.auth_users.repository.UserRepository;
import com.phegon.FoodApp.exceptions.BadRequestException;
import com.phegon.FoodApp.exceptions.NotFoundException;
import com.phegon.FoodApp.response.Response;
import com.phegon.FoodApp.role.entity.Role;
import com.phegon.FoodApp.role.repository.RoleRepository;
import com.phegon.FoodApp.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RoleRepository roleRepository;


    @Override
    public Response<?> register(RegistrationRequest registrationRequest) {

        log.info("INSIDE register()");
        
        validateRegistrationRequest(registrationRequest);
        
        // Validate the registration request
        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        // collect all roles from the request
        List<Role> userRoles;
        if (registrationRequest.getRoles() != null && !registrationRequest.getRoles().isEmpty()) {
            userRoles = registrationRequest.getRoles().stream()
                    .map(roleName -> roleRepository.findByName(roleName.toUpperCase())
                            .orElseThrow(() -> new NotFoundException("Role '" + roleName + "' Not Found")))
                    .toList();
        } else {
            // If no roles provided, default to CUSTOMER
            Role defaultRole = roleRepository.findByName("CUSTOMER")
                    .orElseThrow(() -> new NotFoundException("Default CUSTOMER role Not Found"));
            userRoles = List.of(defaultRole);
        }
        // Build the user object
        User userToSave = User.builder()
                .name(registrationRequest.getName())
                .email(registrationRequest.getEmail())
                .phoneNumber(registrationRequest.getPhoneNumber())
                .address(registrationRequest.getAddress())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .roles(userRoles)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        // Save the user
        userRepository.save(userToSave);

        log.info("User registered successfully");

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("User Registered Successfully")
                .build();


    }

    @Override
    public Response<LoginResponse> login(LoginRequest loginRequest) {

        log.info("INSIDE login()");
        validateLoginRequest(loginRequest); // ✅ thêm dòng này

        // Find the user by email
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid Email"));

        if (!user.isActive()) {
            throw new NotFoundException("Account not active, Please contact CUSTOMER support");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid Password");
        }

        String token = jwtUtils.generateToken(user.getEmail());
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(token);
        loginResponse.setRoles(roleNames);

        return Response.<LoginResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Login Successful")
                .data(loginResponse)
                .build();
    }

    private void validateRegistrationRequest(RegistrationRequest req) {
        if (!req.getEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            throw new BadRequestException("Invalid email format");
        if (req.getPassword().length() < 6)
            throw new BadRequestException("Password must be at least 6 characters long");
        if (!req.getPhoneNumber().matches("\\d+"))
            throw new BadRequestException("Invalid phone number format");
        if (req.getPhoneNumber().length() < 10)
            throw new BadRequestException("Phone number must not exceed 10 digits");
    }

    private void validateLoginRequest(LoginRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank())
            throw new BadRequestException("Email cannot be empty");
        if (!req.getEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            throw new BadRequestException("Invalid email format");
        if (req.getPassword() == null || req.getPassword().isBlank())
            throw new BadRequestException("Password cannot be empty");
        if (req.getPassword().length() < 6)
            throw new BadRequestException("Password must be at least 6 characters long");
    }
}
