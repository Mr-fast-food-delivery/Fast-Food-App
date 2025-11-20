package com.phegon.FoodApp.unit;

import com.phegon.FoodApp.category.dtos.CategoryDTO;
import com.phegon.FoodApp.category.entity.Category;
import com.phegon.FoodApp.category.repository.CategoryRepository;
import com.phegon.FoodApp.category.services.CategoryServiceImpl;
import com.phegon.FoodApp.exceptions.NotFoundException;
import com.phegon.FoodApp.response.Response;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock CategoryRepository categoryRepository;
    @Mock org.modelmapper.ModelMapper modelMapper;

    @InjectMocks CategoryServiceImpl categoryService;

    // Helper entity
    Category mockEntity() {
        Category c = new Category();
        c.setId(1L);
        c.setName("Food");
        c.setDescription("Good food");
        return c;
    }

    // Helper DTO
    CategoryDTO mockDTO() {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(1L);
        dto.setName("Food");
        dto.setDescription("Good food");
        return dto;
    }

    // ================================
    // A. addCategory()
    // ================================
    @Nested
    class AddCategoryTests {

        @Test
        void addCategory_Success() {
            CategoryDTO dto = mockDTO();
            Category mapped = mockEntity();

            when(modelMapper.map(dto, Category.class)).thenReturn(mapped);
            when(categoryRepository.save(mapped)).thenReturn(mapped);

            Response<CategoryDTO> res = categoryService.addCategory(dto);
            assertEquals(200, res.getStatusCode());
        }

        @Test
        void addCategory_ModelMapperFails() {
            CategoryDTO dto = mockDTO();
            when(modelMapper.map(any(), eq(Category.class)))
                    .thenThrow(new RuntimeException("map error"));

            assertThrows(RuntimeException.class, () -> categoryService.addCategory(dto));
        }

        @Test
        void addCategory_SaveFails() {
            CategoryDTO dto = mockDTO();
            Category mapped = mockEntity();

            when(modelMapper.map(dto, Category.class)).thenReturn(mapped);
            when(categoryRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThrows(RuntimeException.class, () -> categoryService.addCategory(dto));
        }

        @Test
        void addCategory_FieldMappingCorrect() {
            CategoryDTO dto = mockDTO();
            Category capt = new Category();

            when(modelMapper.map(any(CategoryDTO.class), eq(Category.class)))
                    .thenAnswer(inv -> {
                        CategoryDTO d = inv.getArgument(0);
                        capt.setName(d.getName());
                        capt.setDescription(d.getDescription());
                        return capt;
                    });

            when(categoryRepository.save(any())).thenReturn(capt);

            categoryService.addCategory(dto);

            assertEquals("Food", capt.getName());
            assertEquals("Good food", capt.getDescription());
        }
    }

    // ================================
    // B. updateCategory()
    // ================================
    @Nested
    class UpdateCategoryTests {

        @Test
        void updateCategory_Success() {
            Category entity = mockEntity();
            CategoryDTO dto = mockDTO();

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(categoryRepository.save(entity)).thenReturn(entity);

            Response<CategoryDTO> res = categoryService.updateCategory(dto);
            assertEquals(200, res.getStatusCode());
        }

        @Test
        void updateCategory_NotFound() {
            CategoryDTO dto = mockDTO();
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> categoryService.updateCategory(dto));
        }

        @Test
        void updateCategory_SaveFails() {
            Category entity = mockEntity();
            CategoryDTO dto = mockDTO();

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(categoryRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThrows(RuntimeException.class, () -> categoryService.updateCategory(dto));
        }

        @Test
        void updateCategory_UpdateNameField() {
            Category entity = mockEntity();

            CategoryDTO dto = new CategoryDTO();
            dto.setId(1L);
            dto.setName("NewName");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(categoryRepository.save(any())).thenReturn(entity);

            categoryService.updateCategory(dto);

            assertEquals("NewName", entity.getName());
        }

        @Test
        void updateCategory_UpdateDescriptionField() {
            Category entity = mockEntity();

            CategoryDTO dto = new CategoryDTO();
            dto.setId(1L);
            dto.setDescription("NewDesc");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(categoryRepository.save(any())).thenReturn(entity);

            categoryService.updateCategory(dto);

            assertEquals("NewDesc", entity.getDescription());
        }
    }

    // ================================
    // C. getCategoryById()
    // ================================
    @Nested
    class GetCategoryByIdTests {

        @Test
        void getCategoryById_Success() {
            Category entity = mockEntity();
            CategoryDTO dto = mockDTO();

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(modelMapper.map(entity, CategoryDTO.class)).thenReturn(dto);

            Response<CategoryDTO> res = categoryService.getCategoryById(1L);

            assertEquals(200, res.getStatusCode());
            assertEquals("Food", res.getData().getName());
        }

        @Test
        void getCategoryById_NotFound() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> categoryService.getCategoryById(1L));
        }

        @Test
        void getCategoryById_ModelMapperFails() {
            Category entity = mockEntity();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(modelMapper.map(any(), eq(CategoryDTO.class))).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> categoryService.getCategoryById(1L));
        }

        @Test
        void getCategoryById_FieldMappingCorrect() {
            Category entity = mockEntity();
            CategoryDTO dto = mockDTO();

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(modelMapper.map(entity, CategoryDTO.class)).thenReturn(dto);

            Response<CategoryDTO> res = categoryService.getCategoryById(1L);

            assertEquals("Food", res.getData().getName());
            assertEquals("Good food", res.getData().getDescription());
        }
    }

    // ================================
    // D. getAllCategories()
    // ================================
    @Nested
    class GetAllCategoriesTests {

        @Test
        void getAllCategories_Success() {
            Category c = mockEntity();
            CategoryDTO dto = mockDTO();

            when(categoryRepository.findAll()).thenReturn(List.of(c));
            when(modelMapper.map(c, CategoryDTO.class)).thenReturn(dto);

            Response<List<CategoryDTO>> res = categoryService.getAllCategories();
            assertEquals(200, res.getStatusCode());
            assertEquals(1, res.getData().size());
        }

        @Test
        void getAllCategories_EmptyList() {
            when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

            Response<List<CategoryDTO>> res = categoryService.getAllCategories();
            assertTrue(res.getData().isEmpty());
        }

        @Test
        void getAllCategories_ModelMapperFails() {
            Category c = mockEntity();

            when(categoryRepository.findAll()).thenReturn(List.of(c));
            when(modelMapper.map(any(), eq(CategoryDTO.class))).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> categoryService.getAllCategories());
        }
    }

    // ================================
    // E. deleteCategory()
    // ================================
    @Nested
    class DeleteCategoryTests {

        @Test
        void deleteCategory_Success() {
            when(categoryRepository.existsById(1L)).thenReturn(true);
            doNothing().when(categoryRepository).deleteById(1L);

            Response<?> res = categoryService.deleteCategory(1L);
            assertEquals(200, res.getStatusCode());
        }

        @Test
        void deleteCategory_NotFound() {
            when(categoryRepository.existsById(1L)).thenReturn(false);
            assertThrows(NotFoundException.class, () -> categoryService.deleteCategory(1L));
        }
    }
}
