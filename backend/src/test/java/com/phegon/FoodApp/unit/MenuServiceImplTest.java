package com.phegon.FoodApp.unit;

import com.phegon.FoodApp.aws.AWSS3Service;
import com.phegon.FoodApp.category.entity.Category;
import com.phegon.FoodApp.category.repository.CategoryRepository;
import com.phegon.FoodApp.exceptions.BadRequestException;
import com.phegon.FoodApp.exceptions.NotFoundException;
import com.phegon.FoodApp.menu.dtos.MenuDTO;
import com.phegon.FoodApp.menu.entity.Menu;
import com.phegon.FoodApp.menu.repository.MenuRepository;
import com.phegon.FoodApp.menu.services.MenuServiceImpl;
import com.phegon.FoodApp.response.Response;
import com.phegon.FoodApp.review.dtos.ReviewDTO;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

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

    MultipartFile mockFile_NoStrict() {
        MultipartFile f = mock(MultipartFile.class, RETURNS_DEEP_STUBS);
        lenient().when(f.isEmpty()).thenReturn(false);
        lenient().when(f.getOriginalFilename()).thenReturn("original.png");
        return f;
    }
    
    @Nested
    @DisplayName("CREATE MENU TESTS")
    class CreateMenuTests {

        @Test
        void createMenu_Success() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(mockFile_NoStrict());

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/ok.png"));

            Menu saved = mockMenu();
            when(menuRepository.save(any())).thenReturn(saved);

            when(modelMapper.map(saved, MenuDTO.class)).thenReturn(dto);

            Response<MenuDTO> res = menuService.createMenu(dto);

            assertEquals(200, res.getStatusCode());
            assertNotNull(res.getData());
        }

        @Test
        void createMenu_CategoryNotFound() {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(mockFile_NoStrict());

            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> menuService.createMenu(dto));
        }

        @Test
        void createMenu_ImageNull() {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(null);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));

            assertThrows(BadRequestException.class,
                    () -> menuService.createMenu(dto));
        }

        @Test
        void createMenu_ImageEmpty() {
            MenuDTO dto = mockMenuDTO();

            MultipartFile file = mock(MultipartFile.class);
            lenient().when(file.isEmpty()).thenReturn(true);
            dto.setImageFile(file);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory()));

            assertThrows(BadRequestException.class,
                    () -> menuService.createMenu(dto));
        }

        @Test
        void createMenu_UploadFails() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(mockFile_NoStrict());

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenThrow(new RuntimeException("upload failed"));

            assertThrows(RuntimeException.class,
                    () -> menuService.createMenu(dto));
        }

        @Test
        void createMenu_SaveFails() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(mockFile_NoStrict());

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/a.png"));

            when(menuRepository.save(any()))
                    .thenThrow(new RuntimeException("db error"));

            assertThrows(RuntimeException.class,
                    () -> menuService.createMenu(dto));
        }

        @Test
        void createMenu_ModelMapperFails() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(mockFile_NoStrict());

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/a.png"));

            when(menuRepository.save(any()))
                    .thenReturn(mockMenu());

            when(modelMapper.map(any(), eq(MenuDTO.class)))
                    .thenThrow(new RuntimeException("mapper error"));

            assertThrows(RuntimeException.class,
                    () -> menuService.createMenu(dto));
        }

        @Test
        void createMenu_ImageNamePattern() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setImageFile(mockFile_NoStrict());

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/UUID_original.png"));

            when(menuRepository.save(any()))
                    .thenReturn(mockMenu());

            when(modelMapper.map(any(), eq(MenuDTO.class)))
                    .thenReturn(dto);

            Response<MenuDTO> res = menuService.createMenu(dto);

            assertEquals(200, res.getStatusCode());
        }
    }

    @Nested
    @DisplayName("UPDATE MENU TESTS")
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

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(existingWithImage()));

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(mockCategory()));

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
            dto.setImageFile(mockFile_NoStrict());

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(existingWithImage()));

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/new.png"));

            Menu updated = existingWithImage();
            updated.setImageUrl("https://s3.com/new.png");

            when(menuRepository.save(any())).thenReturn(updated);

            when(modelMapper.map(updated, MenuDTO.class))
                    .thenReturn(dto);

            Response<MenuDTO> res = menuService.updateMenu(dto);

            assertEquals(200, res.getStatusCode());

            // old image must be deleted
            verify(awss3Service).deleteFile("menus/old.png");

            // new file must be uploaded
            verify(awss3Service).uploadFile(startsWith("menus/"), any());
        }

        @Test
        void updateMenu_MenuNotFound() {
            MenuDTO dto = mockMenuDTO();
            dto.setId(999L);

            when(menuRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> menuService.updateMenu(dto));
        }

        @Test
        void updateMenu_CategoryNotFound() {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(existingWithImage()));

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> menuService.updateMenu(dto));
        }

        @Test
        void updateMenu_OldImageNull_NoDelete() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setImageFile(mockFile_NoStrict());

            Menu menuNoImage = mockMenu();
            menuNoImage.setImageUrl(null);

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(menuNoImage));

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/new.png"));

            when(menuRepository.save(any())).thenReturn(menuNoImage);

            when(modelMapper.map(any(), eq(MenuDTO.class)))
                    .thenReturn(dto);

            menuService.updateMenu(dto);

            verify(awss3Service, never()).deleteFile(anyString());
            verify(awss3Service).uploadFile(anyString(), any());
        }

        @Test
        void updateMenu_UploadNewImageFails() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setImageFile(mockFile_NoStrict());

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(existingWithImage()));

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenThrow(new RuntimeException("upload error"));

            assertThrows(RuntimeException.class,
                    () -> menuService.updateMenu(dto));
        }

        @Test
        void updateMenu_SaveFails() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setImageFile(mockFile_NoStrict());

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(existingWithImage()));

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/new.png"));

            when(menuRepository.save(any()))
                    .thenThrow(new RuntimeException("db error"));

            assertThrows(RuntimeException.class,
                    () -> menuService.updateMenu(dto));
        }

        @Test
        void updateMenu_ModelMapperFails() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setImageFile(mockFile_NoStrict());

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(existingWithImage()));

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/new.png"));

            when(menuRepository.save(any()))
                    .thenReturn(existingWithImage());

            when(modelMapper.map(any(), eq(MenuDTO.class)))
                    .thenThrow(new RuntimeException("mapper error"));

            assertThrows(RuntimeException.class,
                    () -> menuService.updateMenu(dto));
        }

        @Test
        void updateMenu_FieldMappingCorrect() {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setName("New Name");
            dto.setDescription("New Desc");
            dto.setPrice(BigDecimal.valueOf(222));
            dto.setImageFile(null);

            Menu existing = existingWithImage();

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(existing));

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(mockCategory()));

            ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);

            when(menuRepository.save(captor.capture()))
                    .thenReturn(existing);

            when(modelMapper.map(any(), eq(MenuDTO.class)))
                    .thenReturn(dto);

            menuService.updateMenu(dto);

            Menu saved = captor.getValue();
            assertEquals("New Name", saved.getName());
            assertEquals("New Desc", saved.getDescription());
            assertEquals(BigDecimal.valueOf(222), saved.getPrice());
        }

        @Test
        void updateMenu_ImageNamePattern() throws Exception {
            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);
            dto.setImageFile(mockFile_NoStrict());

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(existingWithImage()));

            when(categoryRepository.findById(1L))
                    .thenReturn(Optional.of(mockCategory()));

            when(awss3Service.uploadFile(anyString(), any()))
                    .thenReturn(new URL("https://s3.com/new.png"));

            when(menuRepository.save(any()))
                    .thenReturn(existingWithImage());

            when(modelMapper.map(any(), eq(MenuDTO.class)))
                    .thenReturn(dto);

            menuService.updateMenu(dto);

            ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
            verify(awss3Service).uploadFile(keyCap.capture(), any());

            String key = keyCap.getValue();
            assertTrue(key.startsWith("menus/"));
            assertTrue(key.contains("_"));
        }
    }

    @Nested
    @DisplayName("GET MENU BY ID TESTS")
    class GetMenuByIdTests {

        @Test
        void getMenuById_Success() {
            Menu menu = mockMenu();

            MenuDTO dto = mockMenuDTO();
            dto.setId(10L);

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(menu));

            when(modelMapper.map(menu, MenuDTO.class))
                    .thenReturn(dto);

            Response<MenuDTO> res = menuService.getMenuById(10L);

            assertEquals(200, res.getStatusCode());
            assertEquals(10L, res.getData().getId());
        }

        @Test
        void getMenuById_NotFound() {
            when(menuRepository.findById(10L))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> menuService.getMenuById(10L));
        }

        @Test
        void getMenuById_ModelMapperFails() {
            Menu menu = mockMenu();

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(menu));

            when(modelMapper.map(menu, MenuDTO.class))
                    .thenThrow(new RuntimeException("Mapper error"));

            assertThrows(RuntimeException.class,
                    () -> menuService.getMenuById(10L));
        }

        @Test
        void getMenuById_ReviewSortDesc() {

            Menu menu = mockMenu();

            ReviewDTO r1 = new ReviewDTO(); r1.setId(1L);
            ReviewDTO r2 = new ReviewDTO(); r2.setId(5L);
            ReviewDTO r3 = new ReviewDTO(); r3.setId(3L);

            List<ReviewDTO> reviews = new ArrayList<>(List.of(r1, r2, r3));

            MenuDTO dto = mockMenuDTO();
            dto.setReviews(reviews);

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(menu));

            when(modelMapper.map(menu, MenuDTO.class))
                    .thenReturn(dto);

            Response<MenuDTO> res = menuService.getMenuById(10L);

            List<ReviewDTO> sorted = res.getData().getReviews();

            assertEquals(5L, sorted.get(0).getId());
            assertEquals(3L, sorted.get(1).getId());
            assertEquals(1L, sorted.get(2).getId());
        }

        @Test
        void getMenuById_ReviewNullList() {
            Menu menu = mockMenu();

            MenuDTO dto = mockMenuDTO();
            dto.setReviews(null);

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(menu));

            when(modelMapper.map(menu, MenuDTO.class))
                    .thenReturn(dto);

            // Must not throw
            assertDoesNotThrow(() -> menuService.getMenuById(10L));
        }
    }

    @Nested
    @DisplayName("DELETE MENU TESTS")
    class DeleteMenuTests {

        @Test
        void deleteMenu_Success_WithImage() {

            Menu menu = mockMenu();
            menu.setId(10L);
            menu.setImageUrl("https://s3.com/a.png");

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(menu));

            // deleteFile không return gì → dùng doNothing
            doNothing().when(awss3Service)
                    .deleteFile("menus/a.png");

            // deleteById cũng không trả về
            doNothing().when(menuRepository)
                    .deleteById(10L);

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

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(menu));

            doNothing().when(menuRepository).deleteById(10L);

            menuService.deleteMenu(10L);

            verify(awss3Service, never()).deleteFile(anyString());
            verify(menuRepository).deleteById(10L);
        }


        @Test
        void deleteMenu_NotFound() {

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> menuService.deleteMenu(10L));
        }


        @Test
        void deleteMenu_DeleteFileFails() {

            Menu menu = mockMenu();
            menu.setId(10L);
            menu.setImageUrl("https://s3.com/a.png");

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(menu));

            doThrow(new RuntimeException("s3 error"))
                    .when(awss3Service)
                    .deleteFile("menus/a.png");

            assertThrows(RuntimeException.class,
                    () -> menuService.deleteMenu(10L));
        }


        @Test
        void deleteMenu_DeleteFails() {

            Menu menu = mockMenu();
            menu.setId(10L);
            menu.setImageUrl("https://s3.com/a.png");

            when(menuRepository.findById(10L))
                    .thenReturn(Optional.of(menu));

            // deleteFile OK
            doNothing().when(awss3Service)
                    .deleteFile("menus/a.png");

            // deleteById lỗi
            doThrow(new RuntimeException("db error"))
                    .when(menuRepository)
                    .deleteById(10L);

            assertThrows(RuntimeException.class,
                    () -> menuService.deleteMenu(10L));
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
    @DisplayName("BUILD SPECIFICATION TESTS (FULL FIX)")
    class BuildSpecificationTests {

        // --- Call private method ---
        Specification<Menu> callSpec(Long categoryId, String search) throws Exception {
            var m = MenuServiceImpl.class
                    .getDeclaredMethod("buildSpecification", Long.class, String.class);
            m.setAccessible(true);
            return (Specification<Menu>) m.invoke(menuService, categoryId, search);
        }

        // --- ROOT FOR: NO FILTER ---
        Root<Menu> mockRoot_NoFilters() {
            Root<Menu> root = mock(Root.class);

            // KHÔNG STUB bất kỳ root.get(...) nào
            // → để tránh UnnecessaryStubbing
            return root;
        }

        // --- ROOT FOR: CATEGORY ONLY ---
        Root<Menu> mockRoot_Category() {
            Root<Menu> root = mock(Root.class);
            Path<Object> cat = mock(Path.class);
            Path<Object> catId = mock(Path.class);

            doReturn(cat).when(root).get("category");
            doReturn(catId).when(cat).get("id");

            return root;
        }

        // --- ROOT FOR: SEARCH ONLY ---
        Root<Menu> mockRoot_Search() {
            Root<Menu> root = mock(Root.class);
            Path<Object> name = mock(Path.class);
            Path<Object> desc = mock(Path.class);

            doReturn(name).when(root).get("name");
            doReturn(desc).when(root).get("description");

            return root;
        }

        // --- ROOT FOR: CATEGORY + SEARCH ---
        Root<Menu> mockRoot_CategoryAndSearch() {
            Root<Menu> root = mock(Root.class);

            Path<Object> cat = mock(Path.class);
            Path<Object> catId = mock(Path.class);
            Path<Object> name = mock(Path.class);
            Path<Object> desc = mock(Path.class);

            doReturn(cat).when(root).get("category");
            doReturn(catId).when(cat).get("id");

            doReturn(name).when(root).get("name");
            doReturn(desc).when(root).get("description");

            return root;
        }

        // --- CriteriaBuilder FIX ---
        CriteriaBuilder mockCb() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Predicate p = mock(Predicate.class);
            Expression<String> expr = mock(Expression.class);

            lenient().when(cb.and(any(Predicate[].class))).thenReturn(p);
            lenient().when(cb.equal(any(), any())).thenReturn(p);
            lenient().when(cb.or(any(), any())).thenReturn(p);
            lenient().when(cb.lower(any())).thenReturn(expr);
            lenient().when(cb.like(any(), anyString())).thenReturn(p);

            return cb;
        }

        // ======== TESTS =========

        @Test
        void spec_NoFilters() throws Exception {
            var spec = callSpec(null, null);

            Root<Menu> root = mockRoot_NoFilters();  // không stub gì
            CriteriaQuery<?> query = mock(CriteriaQuery.class);
            CriteriaBuilder cb = mockCb();

            spec.toPredicate(root, query, cb);

            verify(cb, never()).equal(any(), any());
            verify(cb, never()).like(any(), anyString());
        }

        @Test
        void spec_CategoryOnly() throws Exception {
            var spec = callSpec(1L, null);

            Root<Menu> root = mockRoot_Category();
            CriteriaQuery<?> query = mock(CriteriaQuery.class);
            CriteriaBuilder cb = mockCb();

            spec.toPredicate(root, query, cb);

            verify(cb, times(1)).equal(any(), eq(1L));
            verify(cb, never()).like(any(), anyString());
        }

        @Test
        void spec_SearchOnly() throws Exception {
            var spec = callSpec(null, "pizza");

            Root<Menu> root = mockRoot_Search();
            CriteriaQuery<?> query = mock(CriteriaQuery.class);
            CriteriaBuilder cb = mockCb();

            spec.toPredicate(root, query, cb);

            verify(cb, times(2)).like(any(), contains("pizza"));
            verify(cb, never()).equal(any(), any());
        }

        @Test
        void spec_CategoryAndSearch() throws Exception {
            var spec = callSpec(1L, "pizza");

            Root<Menu> root = mockRoot_CategoryAndSearch();
            CriteriaQuery<?> query = mock(CriteriaQuery.class);
            CriteriaBuilder cb = mockCb();

            spec.toPredicate(root, query, cb);

            verify(cb, times(1)).equal(any(), eq(1L));
            verify(cb, times(2)).like(any(), contains("pizza"));
        }
    }

}

