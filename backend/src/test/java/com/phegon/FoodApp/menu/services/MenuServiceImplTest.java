package com.phegon.FoodApp.menu.services;

import com.phegon.FoodApp.aws.AWSS3Service;
import com.phegon.FoodApp.category.entity.Category;
import com.phegon.FoodApp.category.repository.CategoryRepository;
import com.phegon.FoodApp.exceptions.BadRequestException;
import com.phegon.FoodApp.exceptions.NotFoundException;
import com.phegon.FoodApp.menu.dtos.MenuDTO;
import com.phegon.FoodApp.menu.entity.Menu;
import com.phegon.FoodApp.menu.repository.MenuRepository;
import com.phegon.FoodApp.response.Response;
import com.phegon.FoodApp.review.dtos.ReviewDTO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock MenuRepository menuRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock AWSS3Service awss3Service;
    @Mock org.modelmapper.ModelMapper modelMapper;

    @InjectMocks MenuServiceImpl menuService;

    // helper
    MenuDTO mockMenuDTO() {
        MenuDTO dto = new MenuDTO();
        dto.setName("Pizza");
        dto.setDescription("Delicious");
        dto.setPrice(BigDecimal.valueOf(99));
        dto.setCategoryId(1L);
        return dto;
    }

    Category mockCategory() {
        Category c = new Category();
        c.setId(1L);
        return c;
    }

    Menu mockMenu() {
        return Menu.builder()
                .id(10L)
                .name("Pizza")
                .description("Yummy")
                .price(BigDecimal.valueOf(99))
                .imageUrl("https://s3.com/image.png")
                .category(mockCategory())
                .build();
    }

    MultipartFile mockFile() {
        MultipartFile f = mock(MultipartFile.class);
        when(f.isEmpty()).thenReturn(false);
        when(f.getOriginalFilename()).thenReturn("photo.png");
        return f;
    }
    @Nested
    class CreateMenuTests {

        @Test
        void createMenu_Success() {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(mockFile());

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));
            try {
                when(awss3Service.uploadFile(any(), any()))
                        .thenReturn(new URL("https://s3.com/new.png"));
            } catch (Exception ignored) {}

            Menu saved = mockMenu();
            when(menuRepository.save(any())).thenReturn(saved);

            when(modelMapper.map(saved, MenuDTO.class)).thenReturn(dto);

            Response<MenuDTO> res = menuService.createMenu(dto);
            assertEquals(200, res.getStatusCode());
        }

        @Test
        void createMenu_CategoryNotFound() {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(mockFile());

            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> menuService.createMenu(dto));
        }

        @Test
        void createMenu_ImageNull() {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(null);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));
            assertThrows(BadRequestException.class, () -> menuService.createMenu(dto));
        }

        @Test
        void createMenu_ImageEmpty() {
            MenuDTO dto = mockMenuDTO();
            MultipartFile f = mock(MultipartFile.class);
            when(f.isEmpty()).thenReturn(true);
            dto.setImageFile(f);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));
            assertThrows(BadRequestException.class, () -> menuService.createMenu(dto));
        }

        @Test
        void createMenu_UploadFails() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(mockFile());

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));
            when(awss3Service.uploadFile(any(), any())).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> menuService.createMenu(dto));
        }

        @Test
        void createMenu_SaveFails() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(mockFile());

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));
            when(awss3Service.uploadFile(any(), any())).thenReturn(new URL("https://s3.com/x.png"));
            when(menuRepository.save(any())).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> menuService.createMenu(dto));
        }

        @Test
        void createMenu_ModelMapperFails() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(mockFile());

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));
            when(awss3Service.uploadFile(any(), any())).thenReturn(new URL("https://s3.com/x.png"));
            when(menuRepository.save(any())).thenReturn(mockMenu());
            when(modelMapper.map(any(), eq(MenuDTO.class))).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> menuService.createMenu(dto));
        }

        @Test
        void createMenu_ImageNamePattern() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(mockFile());

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));
            when(awss3Service.uploadFile(any(), any()))
                    .thenReturn(new URL("https://s3.com/UUID_photo.png"));

            when(menuRepository.save(any())).thenReturn(mockMenu());
            when(modelMapper.map(any(), eq(MenuDTO.class))).thenReturn(dto);

            Response<MenuDTO> res = menuService.createMenu(dto);
            assertEquals(200, res.getStatusCode());
        }
    }

        @Nested
    class UpdateMenuTests {

        Menu existingWithImage() {
            Menu m = mockMenu();
            m.setImageUrl("https://s3.com/old.png");
            return m;
        }

        @Test
        void updateMenu_Success_NoImageChange() {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setImageFile(null);

            when(menuRepository.findById(10L)).thenReturn(Optional.of(existingWithImage()));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));

            Menu updated = existingWithImage();
            when(menuRepository.save(any())).thenReturn(updated);
            when(modelMapper.map(updated, MenuDTO.class)).thenReturn(dto);

            Response<MenuDTO> res = menuService.updateMenu(dto);

            assertEquals(200, res.getStatusCode());
            verify(awss3Service, never()).deleteFile(anyString());
            verify(awss3Service, never()).uploadFile(anyString(), any());
        }

        @Test
        void updateMenu_Success_WithImageChange() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setImageFile(mockFile());

            when(menuRepository.findById(10L)).thenReturn(Optional.of(existingWithImage()));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/new.png"));

            Menu updated = existingWithImage();
            updated.setImageUrl("https://s3.com/new.png");
            when(menuRepository.save(any())).thenReturn(updated);
            when(modelMapper.map(updated, MenuDTO.class)).thenReturn(dto);

            Response<MenuDTO> res = menuService.updateMenu(dto);

            assertEquals(200, res.getStatusCode());
            // old image bị xóa
            verify(awss3Service).deleteFile("menus/old.png");
            // ảnh mới upload
            verify(awss3Service).uploadFile(startsWith("menus/"), any());
        }

        @Test
        void updateMenu_MenuNotFound() {
            MenuDTO dto = mockMenuDTO();
            dto.setId(99L);

            when(menuRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> menuService.updateMenu(dto));
        }

        @Test
        void updateMenu_CategoryNotFound() {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);

            when(menuRepository.findById(10L)).thenReturn(Optional.of(existingWithImage()));
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> menuService.updateMenu(dto));
        }

        @Test
        void updateMenu_OldImageNull_NoDelete() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setImageFile(mockFile());

            Menu menuNoImage = mockMenu();
            menuNoImage.setImageUrl(null);

            when(menuRepository.findById(10L)).thenReturn(Optional.of(menuNoImage));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));
            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/new.png"));

            when(menuRepository.save(any())).thenReturn(menuNoImage);
            when(modelMapper.map(any(), eq(MenuDTO.class))).thenReturn(dto);

            menuService.updateMenu(dto);

            verify(awss3Service, never()).deleteFile(anyString());
            verify(awss3Service).uploadFile(anyString(), any());
        }

        @Test
        void updateMenu_UploadNewImageFails() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setImageFile(mockFile());

            when(menuRepository.findById(10L)).thenReturn(Optional.of(existingWithImage()));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenThrow(new RuntimeException("upload error"));

            assertThrows(RuntimeException.class, () -> menuService.updateMenu(dto));
        }

        @Test
        void updateMenu_SaveFails() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setImageFile(mockFile());

            when(menuRepository.findById(10L)).thenReturn(Optional.of(existingWithImage()));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));
            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/new.png"));
            when(menuRepository.save(any())).thenThrow(new RuntimeException("db error"));

            assertThrows(RuntimeException.class, () -> menuService.updateMenu(dto));
        }

        @Test
        void updateMenu_ModelMapperFails() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setImageFile(mockFile());

            when(menuRepository.findById(10L)).thenReturn(Optional.of(existingWithImage()));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));
            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/new.png"));
            when(menuRepository.save(any())).thenReturn(existingWithImage());
            when(modelMapper.map(any(), eq(MenuDTO.class))).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> menuService.updateMenu(dto));
        }

        @Test
        void updateMenu_FieldMappingCorrect() {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setName("New name");
            dto.setDescription("New desc");
            dto.setPrice(BigDecimal.valueOf(123));
            dto.setImageFile(null);

            Menu existing = existingWithImage();

            when(menuRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));

            ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
            when(menuRepository.save(captor.capture())).thenReturn(existing);
            when(modelMapper.map(any(), eq(MenuDTO.class))).thenReturn(dto);

            menuService.updateMenu(dto);

            Menu saved = captor.getValue();
            assertEquals("New name", saved.getName());
            assertEquals("New desc", saved.getDescription());
            assertEquals(BigDecimal.valueOf(123), saved.getPrice());
        }

        @Test
        void updateMenu_ImageNamePattern() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setImageFile(mockFile());

            when(menuRepository.findById(10L)).thenReturn(Optional.of(existingWithImage()));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/new.png"));
            when(menuRepository.save(any())).thenReturn(existingWithImage());
            when(modelMapper.map(any(), eq(MenuDTO.class))).thenReturn(dto);

            menuService.updateMenu(dto);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(awss3Service).uploadFile(keyCaptor.capture(), any());
            String key = keyCaptor.getValue();
            assertTrue(key.startsWith("menus/"));
            assertTrue(key.contains("_"));
        }
    }
    @Nested
    class GetMenuByIdTests {

        @Test
        void getMenuById_Success() {
            Menu menu = mockMenu();
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);

            when(menuRepository.findById(10L)).thenReturn(Optional.of(menu));
            when(modelMapper.map(menu, MenuDTO.class)).thenReturn(dto);

            Response<MenuDTO> res = menuService.getMenuById(10L);

            assertEquals(200, res.getStatusCode());
            assertEquals(10L, res.getData().getId());
        }

        @Test
        void getMenuById_NotFound() {
            when(menuRepository.findById(10L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> menuService.getMenuById(10L));
        }

        @Test
        void getMenuById_ModelMapperFails() {
            Menu menu = mockMenu();

            when(menuRepository.findById(10L)).thenReturn(Optional.of(menu));
            when(modelMapper.map(any(), eq(MenuDTO.class))).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> menuService.getMenuById(10L));
        }

        @Test
        void getMenuById_ReviewSortDesc() {
            Menu menu = mockMenu();

            ReviewDTO r1 = new ReviewDTO(); r1.setId(1L);
            ReviewDTO r2 = new ReviewDTO(); r2.setId(3L);
            ReviewDTO r3 = new ReviewDTO(); r3.setId(2L);
            List<ReviewDTO> reviews = new ArrayList<>(List.of(r1, r2, r3));

            MenuDTO dto = mockMenuDTO();
            dto.setReviews(reviews);

            when(menuRepository.findById(10L)).thenReturn(Optional.of(menu));
            when(modelMapper.map(menu, MenuDTO.class)).thenReturn(dto);

            Response<MenuDTO> res = menuService.getMenuById(10L);

            List<ReviewDTO> sorted = res.getData().getReviews();
            assertEquals(3L, sorted.get(0).getId());
            assertEquals(2L, sorted.get(1).getId());
            assertEquals(1L, sorted.get(2).getId());
        }

        @Test
        void getMenuById_ReviewNullList() {
            Menu menu = mockMenu();
            MenuDTO dto = mockMenuDTO();
            dto.setReviews(null);

            when(menuRepository.findById(10L)).thenReturn(Optional.of(menu));
            when(modelMapper.map(menu, MenuDTO.class)).thenReturn(dto);

            assertDoesNotThrow(() -> menuService.getMenuById(10L));
        }
    }
    @Nested
    class DeleteMenuTests {

        @Test
        void deleteMenu_Success_WithImage() {
            Menu menu = mockMenu();
            menu.setId(10L);
            menu.setImageUrl("https://s3.com/a.png");

            when(menuRepository.findById(10L)).thenReturn(Optional.of(menu));

            Response<?> res = menuService.deleteMenu(10L);

            assertEquals(200, res.getStatusCode());
            verify(awss3Service).deleteFile("menus/a.png");
            verify(menuRepository).deleteById(10L);
        }

        @Test
        void deleteMenu_Success_NoImage() {
            Menu menu = mockMenu();
            menu.setId(10L);
            menu.setImageUrl(null);

            when(menuRepository.findById(10L)).thenReturn(Optional.of(menu));

            menuService.deleteMenu(10L);

            verify(awss3Service, never()).deleteFile(anyString());
            verify(menuRepository).deleteById(10L);
        }

        @Test
        void deleteMenu_NotFound() {
            when(menuRepository.findById(10L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> menuService.deleteMenu(10L));
        }

        @Test
        void deleteMenu_DeleteFileFails() {
            Menu menu = mockMenu();
            menu.setId(10L);
            menu.setImageUrl("https://s3.com/a.png");

            when(menuRepository.findById(10L)).thenReturn(Optional.of(menu));
            doThrow(new RuntimeException("s3 error"))
                    .when(awss3Service).deleteFile(anyString());

            assertThrows(RuntimeException.class, () -> menuService.deleteMenu(10L));
        }

        @Test
        void deleteMenu_DeleteFails() {
            Menu menu = mockMenu();
            menu.setId(10L);
            menu.setImageUrl("https://s3.com/a.png");

            when(menuRepository.findById(10L)).thenReturn(Optional.of(menu));
            // deleteFile ok
            doNothing().when(awss3Service).deleteFile(anyString());
            doThrow(new RuntimeException("db error")).when(menuRepository).deleteById(10L);

            assertThrows(RuntimeException.class, () -> menuService.deleteMenu(10L));
        }
    }
    @Nested
    class GetMenusTests {

        Menu menu1() {
            Menu m = mockMenu();
            m.setId(1L);
            return m;
        }

        @Test
        void getMenus_NoFilter() {
            List<Menu> menus = List.of(menu1());

            when(menuRepository.findAll(
                    ArgumentMatchers.<Specification<Menu>>any(),
                    Mockito.<Sort>any()
            )).thenReturn(menus);

            MenuDTO dto = mockMenuDTO();
            when(modelMapper.map(any(Menu.class), eq(MenuDTO.class))).thenReturn(dto);

            Response<List<MenuDTO>> res = menuService.getMenus(null, null);

            assertEquals(200, res.getStatusCode());
            assertEquals(1, res.getData().size());
        }

        @Test
        void getMenus_WithCategoryAndSearch() {
            List<Menu> menus = List.of(menu1());

            when(menuRepository.findAll(
                    ArgumentMatchers.<Specification<Menu>>any(),
                    Mockito.<Sort>any()
            )).thenReturn(menus);

            MenuDTO dto = mockMenuDTO();
            when(modelMapper.map(any(Menu.class), eq(MenuDTO.class))).thenReturn(dto);

            Response<List<MenuDTO>> res = menuService.getMenus(1L, "pizza");

            assertEquals(200, res.getStatusCode());

            verify(menuRepository).findAll(
                    ArgumentMatchers.<Specification<Menu>>any(),
                    Mockito.<Sort>any()
            );
        }

        @Test
        void getMenus_EmptyList() {

            when(menuRepository.findAll(
                    ArgumentMatchers.<Specification<Menu>>any(),
                    Mockito.<Sort>any()
            )).thenReturn(Collections.emptyList());

            Response<List<MenuDTO>> res = menuService.getMenus(null, null);

            assertNotNull(res.getData());
            assertTrue(res.getData().isEmpty());
        }

        @Test
        void getMenus_ModelMapperFails() {
            List<Menu> menus = List.of(menu1());

            when(menuRepository.findAll(
                    ArgumentMatchers.<Specification<Menu>>any(),
                    Mockito.<Sort>any()
            )).thenReturn(menus);

            when(modelMapper.map(any(Menu.class), eq(MenuDTO.class)))
                    .thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> menuService.getMenus(null, null));
        }
    }


    @Nested
    class BuildSpecificationTests {

        @SuppressWarnings("unchecked")
        org.springframework.data.jpa.domain.Specification<Menu> callSpec(Long categoryId, String search) throws Exception {
            var m = MenuServiceImpl.class
                    .getDeclaredMethod("buildSpecification", Long.class, String.class);
            m.setAccessible(true);
            return (org.springframework.data.jpa.domain.Specification<Menu>)
                    m.invoke(menuService, categoryId, search);
        }

        jakarta.persistence.criteria.Root<Menu> mockRoot() {
            @SuppressWarnings("unchecked")
            jakarta.persistence.criteria.Root<Menu> root = mock(jakarta.persistence.criteria.Root.class);
            jakarta.persistence.criteria.Path<Object> path = mock(jakarta.persistence.criteria.Path.class);
            when(root.get(anyString())).thenReturn(path);
            when(path.get(anyString())).thenReturn(path);
            return root;
        }

        jakarta.persistence.criteria.CriteriaBuilder mockCb() {
            jakarta.persistence.criteria.CriteriaBuilder cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);
            jakarta.persistence.criteria.Predicate p = mock(jakarta.persistence.criteria.Predicate.class);
            jakarta.persistence.criteria.Expression<String> expr = mock(jakarta.persistence.criteria.Expression.class);

            when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(p);
            when(cb.equal(any(), any())).thenReturn(p);
            when(cb.or(any(), any())).thenReturn(p);
            when(cb.lower(any())).thenReturn(expr);
            when(cb.like(any(), anyString())).thenReturn(p);

            return cb;
        }

        @Test
        void spec_NoFilters() throws Exception {
            var spec = callSpec(null, null);

            var root = mockRoot();
            jakarta.persistence.criteria.CriteriaQuery<?> query =
                    mock(jakarta.persistence.criteria.CriteriaQuery.class);
            var cb = mockCb();

            spec.toPredicate(root, query, cb);

            // không gọi equal / like khi không có filter
            verify(cb, never()).equal(any(), any());
            verify(cb, never()).like(any(), anyString());
        }

        @Test
        void spec_CategoryOnly() throws Exception {
            var spec = callSpec(1L, null);

            var root = mockRoot();
            jakarta.persistence.criteria.CriteriaQuery<?> query =
                    mock(jakarta.persistence.criteria.CriteriaQuery.class);
            var cb = mockCb();

            spec.toPredicate(root, query, cb);

            verify(cb, times(1)).equal(any(), eq(1L));
            verify(cb, never()).like(any(), anyString());
        }

        @Test
        void spec_SearchOnly() throws Exception {
            var spec = callSpec(null, "pizza");

            var root = mockRoot();
            jakarta.persistence.criteria.CriteriaQuery<?> query =
                    mock(jakarta.persistence.criteria.CriteriaQuery.class);
            var cb = mockCb();

            spec.toPredicate(root, query, cb);

            // 2 like: name + description
            verify(cb, times(2)).like(any(), contains("%pizza%"));
            verify(cb, never()).equal(any(), any());
        }

        @Test
        void spec_CategoryAndSearch() throws Exception {
            var spec = callSpec(1L, "pizza");

            var root = mockRoot();
            jakarta.persistence.criteria.CriteriaQuery<?> query =
                    mock(jakarta.persistence.criteria.CriteriaQuery.class);
            var cb = mockCb();

            spec.toPredicate(root, query, cb);

            verify(cb, times(1)).equal(any(), eq(1L));
            verify(cb, times(2)).like(any(), contains("%pizza%"));
        }
    }
}

