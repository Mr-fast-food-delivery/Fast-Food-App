package com.phegon.FoodApp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phegon.FoodApp.FoodAppApplication;
import com.phegon.FoodApp.auth_users.entity.User;
import com.phegon.FoodApp.auth_users.repository.UserRepository;
import com.phegon.FoodApp.config.FakeS3Config;
import com.phegon.FoodApp.config.TestSecurityConfig;
import com.phegon.FoodApp.role.entity.Role;
import com.phegon.FoodApp.role.repository.RoleRepository;
import com.phegon.FoodApp.security.AuthFilter;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.List;

@SpringBootTest(
    classes = FoodAppApplication.class,
    properties = {
        "spring.main.allow-bean-definition-overriding=true"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({FakeS3Config.class, TestSecurityConfig.class})
class UserIntegrationIT {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    ObjectMapper mapper;


    private Role createRole(String name) {
    return roleRepository.findByName(name).orElseGet(() -> {
        Role r = new Role();
        r.setName(name);
        return roleRepository.save(r);
    });
}

    private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor token(User user) {
        String roleName = user.getRoles().get(0).getName();
        return user(user.getEmail())
        .password("123456")
        .authorities(new SimpleGrantedAuthority("ROLE_" + roleName));

    }


    private User createUser(String email, String roleName, boolean active) {
        Role role = createRole(roleName);

        User u = new User();
        u.setEmail(email);
        u.setPassword("123456");
        u.setActive(active);
        u.setRoles(List.of(role));
        return userRepository.save(u);
    }


    // ============================================================
    // INT003 — Get Current User /account
    // ============================================================

    @Test
    void INT003a_getCurrentUser_success() throws Exception {
        User user = createUser("a@gmail.com", "CUSTOMER", true);

        mockMvc.perform(get("/api/users/account")
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})
)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("a@gmail.com"));
    }

    @Test
    void INT003b_missingToken() throws Exception {
        mockMvc.perform(get("/api/users/account"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void INT003c_tokenInvalid() throws Exception {
        mockMvc.perform(get("/api/users/account")
                        .header("Authorization", "Bearer abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void INT003d_userDeletedButTokenStillValid() throws Exception {
        User user = createUser("b@gmail.com", "CUSTOMER", true);
        userRepository.delete(user);

        mockMvc.perform(get("/api/users/account")
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})
)
                .andExpect(status().isNotFound());
    }

    @Test
    void INT003e_userInactive() throws Exception {
        User user = createUser("c@gmail.com", "CUSTOMER", false);

        mockMvc.perform(get("/api/users/account")
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})
)
                .andExpect(status().isNotFound());
    }


    // ============================================================
    // INT004 — Get All Users /all (ADMIN only)
    // ============================================================

    @Test
    void INT004a_adminGetAll_success() throws Exception {
        User admin = createUser("admin@gmail.com", "ADMIN", true);

        mockMvc.perform(get("/api/users/all")
                        .with(token(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void INT004b_customerForbidden() throws Exception {
        User user = createUser("u@gmail.com", "CUSTOMER", true);

        mockMvc.perform(get("/api/users/all")
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})
)
                .andExpect(status().isForbidden());
    }

    @Test
    void INT004c_missingToken() throws Exception {
        mockMvc.perform(get("/api/users/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void INT004d_tokenInvalid() throws Exception {
        mockMvc.perform(get("/api/users/all")
                        .header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized());
    }


    // ============================================================
    // INT005 — /own (same logic như /account)
    // ============================================================

    @Test
    void INT005a_getOwnAccount_success() throws Exception {
        User user = createUser("z@gmail.com", "CUSTOMER", true);

        mockMvc.perform(get("/api/users/account")
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})
)
                .andExpect(status().isOk());
    }

    @Test
    void INT005b_missingToken() throws Exception {
        mockMvc.perform(get("/api/users/account"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void INT005c_tokenExpired_invalid() throws Exception {
        mockMvc.perform(get("/api/users/account")
                        .header("Authorization", "Bearer expiredToken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void INT005d_userInactive() throws Exception {
        User user = createUser("e@gmail.com", "CUSTOMER", false);

        mockMvc.perform(get("/api/users/account")
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})
)
                .andExpect(status().isNotFound());
    }


    // ============================================================
    // INT006 — Update user
    // ============================================================

    @Test
    void INT006a_update_success() throws Exception {
        User user = createUser("x@gmail.com", "CUSTOMER", true);

        mockMvc.perform(multipart("/api/users/update")
                    .with(request -> { request.setMethod("PUT"); return request; })
                    .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})

                    .param("name", "NewName"))

                .andExpect(status().isOk());
    }

    @Test
    void INT006b_emailDuplicate() throws Exception {
        createUser("exist@gmail.com", "CUSTOMER", true);
        User user = createUser("my@gmail.com", "CUSTOMER", true);

        mockMvc.perform(multipart("/api/users/update")
                    .with(request -> { request.setMethod("PUT"); return request; })
                    .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})

                    .param("email", "exist@gmail.com"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void INT006c_emailFormatInvalid() throws Exception {
        User user = createUser("t@gmail.com", "CUSTOMER", true);

        mockMvc.perform(multipart("/api/users/update")
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})

                        .param("email", "abc@"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void INT006d_phoneNotTenDigits() throws Exception {
        User user = createUser("p@gmail.com", "CUSTOMER", true);

        mockMvc.perform(multipart("/api/users/update")
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})

                        .param("phoneNumber", "12345"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void INT006e_passwordTooShort() throws Exception {
        User user = createUser("q@gmail.com", "CUSTOMER", true);

        mockMvc.perform(multipart("/api/users/update")
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})

                        .param("password", "123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void INT006f_imageEmpty() throws Exception {
        User user = createUser("r@gmail.com", "CUSTOMER", true);

        mockMvc.perform(multipart("/api/users/update")
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})

                        .param("imageFile", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void INT006g_nameEmpty() throws Exception {
        User user = createUser("s@gmail.com", "CUSTOMER", true);

        mockMvc.perform(multipart("/api/users/update")
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})

                        .param("name", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void INT006h_missingToken() throws Exception {
        mockMvc.perform(multipart("/api/users/update")
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isUnauthorized());
    }


    // ============================================================
    // INT007 — Deactivate account
    // ============================================================

    @Test
    void INT007a_deactivate_success() throws Exception {
        User user = createUser("aa@gmail.com", "CUSTOMER", true);

        mockMvc.perform(delete("/api/users/deactivate")
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})
)
                .andExpect(status().isOk());
    }

    @Test
    void INT007b_missingToken() throws Exception {
        mockMvc.perform(delete("/api/users/deactivate"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void INT007c_tokenExpired() throws Exception {
        mockMvc.perform(delete("/api/users/deactivate")
                        .header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void INT007d_userNotFound() throws Exception {
        User user = createUser("deleted@gmail.com", "CUSTOMER", true);
        userRepository.delete(user);

        mockMvc.perform(delete("/api/users/deactivate")
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})
)
                .andExpect(status().isNotFound());
    }

    @Test
    void INT007e_userAlreadyInactive() throws Exception {
        User user = createUser("inactive@gmail.com", "CUSTOMER", false);

        mockMvc.perform(delete("/api/users/deactivate")
                        .with(request -> {
    request.addHeader("Authorization", "Bearer " + user.getId());
    return request;
})
)
                .andExpect(status().isOk());
    }
}
