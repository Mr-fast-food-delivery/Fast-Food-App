package com.phegon.FoodApp.menu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phegon.FoodApp.menu.dtos.MenuDTO;
import com.phegon.FoodApp.menu.services.MenuService;
import com.phegon.FoodApp.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MenuController.class)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuService menuService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetMenuById() throws Exception {

        MenuDTO menuDTO = new MenuDTO();
        menuDTO.setId(1L);
        menuDTO.setName("Pizza");
        menuDTO.setPrice(BigDecimal.valueOf(100));

        Response<MenuDTO> response = Response.<MenuDTO>builder()
                .statusCode(200)
                .message("Success")
                .data(menuDTO)
                .build();

        when(menuService.getMenuById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/menu/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Pizza"));
    }

    @Test
    void testGetMenus() throws Exception {

        Response<?> response = Response.<java.util.List<MenuDTO>>builder()
                .statusCode(200)
                .message("Success")
                .data(Collections.emptyList())
                .build();

        when(menuService.getMenus(null, null)).thenReturn(response);

        mockMvc.perform(get("/api/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"));
    }
}
