package com.phegon.FoodApp.menu.service;

import com.phegon.FoodApp.category.entity.Category;
import com.phegon.FoodApp.menu.dtos.MenuDTO;
import com.phegon.FoodApp.menu.entity.Menu;
import com.phegon.FoodApp.menu.repository.MenuRepository;
import com.phegon.FoodApp.menu.services.MenuServiceImpl;
import com.phegon.FoodApp.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private MenuServiceImpl menuService;

    private MenuDTO menuDTO;
    private Menu menu;
    private MultipartFile file;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(2L);

        menuDTO = new MenuDTO();
        menuDTO.setId(1L);
        menuDTO.setName("Pizza");
        menuDTO.setPrice(BigDecimal.valueOf(100));
        menuDTO.setCategoryId(2L);

        menu = new Menu();
        menu.setId(1L);
        menu.setName("Pizza");
        menu.setPrice(BigDecimal.valueOf(100));
        menu.setCategory(category);

        file = mock(MultipartFile.class);
    }

    @Test
    void testGetMenuById_Found() {
        when(menuRepository.findById(1L)).thenReturn(Optional.of(menu));
        when(modelMapper.map(menu, MenuDTO.class)).thenReturn(menuDTO);

        Response<MenuDTO> response = menuService.getMenuById(1L);

        assertEquals(200, response.getStatusCode());
        assertEquals("Pizza", response.getData().getName());
    }

    @Test
    void testGetMenuById_NotFound() {
        when(menuRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> menuService.getMenuById(1L));
    }
}
