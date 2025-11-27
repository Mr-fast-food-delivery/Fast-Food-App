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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final AWSS3Service awss3Service;

    // =========================================================================
    // CREATE MENU
    // =========================================================================
    @Override
    public Response<MenuDTO> createMenu(MenuDTO menuDTO) {

        log.info("Inside createMenu()");

        // 1. Validate category
        Category category = categoryRepository.findById(menuDTO.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found with ID: " + menuDTO.getCategoryId()));

        // 2. Validate name
        if (menuDTO.getName() == null || menuDTO.getName().trim().isEmpty()) {
            throw new BadRequestException("Menu name is required");
        }

        // 3. Validate price
        if (menuDTO.getPrice() == null || menuDTO.getPrice().doubleValue() <= 0) {
            throw new BadRequestException("Price must be greater than 0");
        }

        // 4. Validate image
        MultipartFile imageFile = menuDTO.getImageFile();
        if (imageFile == null || imageFile.isEmpty()) {
            throw new BadRequestException("Menu image is required");
        }

        // 5. Upload image
        String imageName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
        URL s3Url = awss3Service.uploadFile("menus/" + imageName, imageFile);

        // 6. Save menu
        Menu menu = Menu.builder()
                .name(menuDTO.getName().trim())
                .description(menuDTO.getDescription())
                .price(menuDTO.getPrice())
                .imageUrl(s3Url.toString())
                .category(category)
                .build();

        Menu saved = menuRepository.save(menu);

        return Response.<MenuDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu created successfully")
                .data(modelMapper.map(saved, MenuDTO.class))
                .build();
    }

    // =========================================================================
    // UPDATE MENU
    // =========================================================================
    @Override
    public Response<MenuDTO> updateMenu(MenuDTO menuDTO) {

        log.info("Inside updateMenu()");

        Menu existing = menuRepository.findById(menuDTO.getId())
                .orElseThrow(() -> new NotFoundException("Menu not found"));

        // Validate new category
        Category category = categoryRepository.findById(menuDTO.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        // =====================================================================
        // 1. Validate name if provided
        // =====================================================================
        if (menuDTO.getName() != null) {
            if (menuDTO.getName().trim().isEmpty()) {
                throw new BadRequestException("Menu name cannot be empty");
            }
            existing.setName(menuDTO.getName().trim());
        }

        // =====================================================================
        // 2. Validate price if provided
        // =====================================================================
        if (menuDTO.getPrice() != null) {
            if (menuDTO.getPrice().doubleValue() <= 0) {
                throw new BadRequestException("Price must be greater than 0");
            }
            existing.setPrice(menuDTO.getPrice());
        }

        // =====================================================================
        // 3. Validate image if provided
        // =====================================================================
        MultipartFile newFile = menuDTO.getImageFile();

        if (newFile != null) {
            if (newFile.isEmpty()) {
                throw new BadRequestException("Image file cannot be empty");
            }

            // delete old
            if (existing.getImageUrl() != null && !existing.getImageUrl().isEmpty()) {
                String oldKey = existing.getImageUrl().substring(existing.getImageUrl().lastIndexOf("/") + 1);
                awss3Service.deleteFile("menus/" + oldKey);
            }

            // upload new file
            String newName = UUID.randomUUID() + "_" + newFile.getOriginalFilename();
            URL newUrl = awss3Service.uploadFile("menus/" + newName, newFile);
            existing.setImageUrl(newUrl.toString());
        }

        // Update category
        existing.setCategory(category);
        existing.setDescription(menuDTO.getDescription());

        Menu updated = menuRepository.save(existing);

        return Response.<MenuDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu updated successfully")
                .data(modelMapper.map(updated, MenuDTO.class))
                .build();
    }

    @Override
    public Response<MenuDTO> getMenuById(Long id) {

        log.info("Inside getMenuById()");

        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Menu not found"));

        MenuDTO menuDTO = modelMapper.map(menu, MenuDTO.class);

        // Sort the reviews by id in descending order
        if (menuDTO.getReviews() != null) {
            menuDTO.getReviews().sort(Comparator.comparing(ReviewDTO::getId).reversed());
        }

        return Response.<MenuDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu retrieved successfully")
                .data(menuDTO)
                .build();
    }

    @Override
    public Response<?> deleteMenu(Long id) {

        log.info("Inside deleteMenu()");

        Menu menuToDelete = menuRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Menu  not found with ID: " + id));

        // Delete the image from S3 if it exists
        String imageUrl = menuToDelete.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String keyName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            awss3Service.deleteFile("menus/" + keyName);
            log.info("Deleted image from S3: menus/" + keyName);
        }

        menuRepository.deleteById(id);
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu  deleted successfully")
                .build();
    }

    @Override
    public Response<List<MenuDTO>> getMenus(Long categoryId, String search) {

        log.info("Inside getMenus()");

        Specification<Menu> spec = buildSpecification(categoryId, search);

        Sort sort = Sort.by(Sort.Direction.DESC, "id");

        List<Menu> menuList = menuRepository.findAll(spec, sort);

        List<MenuDTO> menuDTOS = menuList.stream()
                .map(menu -> modelMapper.map(menu, MenuDTO.class))
                .toList();

        return Response.<List<MenuDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menus retrieved")
                .data(menuDTOS)
                .build();

    }


    private Specification<Menu> buildSpecification(Long categoryId, String search) {
        return (root, query, cb) -> {
            // List to accumulate all WHERE conditions
            List<Predicate> predicates = new ArrayList<>();

            // Add category filter if categoryId is provided
            if (categoryId != null) {
                // Creates condition: category.id = providedCategoryId
                predicates.add(cb.equal(
                        root.get("category").get("id"), // Navigate to category->id
                        categoryId                      // Match provided category ID
                ));
            }

            // Add search term filter if search text is provided
            if (search != null && !search.isBlank()) {
                // Prepare search term with wildcards for partial matching
                // Converts to lowercase for case-insensitive search
                String searchTerm = "%" + search.toLowerCase() + "%";

                // Creates OR condition for:
                // (name LIKE %term% OR description LIKE %term%)
                predicates.add(cb.or(
                        cb.like(
                                cb.lower(root.get("name")), // Convert name to lowercase
                                searchTerm                 // Match against search term
                        ),
                        cb.like(
                                cb.lower(root.get("description")), // Convert description to lowercase
                                searchTerm                        // Match against search term
                        )
                ));
            }

            // Combine all conditions with AND logic
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}









