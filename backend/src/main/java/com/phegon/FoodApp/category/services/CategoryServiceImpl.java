package com.phegon.FoodApp.category.services;

import com.phegon.FoodApp.category.dtos.CategoryDTO;
import com.phegon.FoodApp.category.entity.Category;
import com.phegon.FoodApp.category.repository.CategoryRepository;
import com.phegon.FoodApp.exceptions.BadRequestException;
import com.phegon.FoodApp.exceptions.NotFoundException;
import com.phegon.FoodApp.menu.repository.MenuRepository;
import com.phegon.FoodApp.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final MenuRepository menuRepository;

    // =========================================================================
    // ADD CATEGORY
    // =========================================================================
    @Override
    public Response<CategoryDTO> addCategory(CategoryDTO categoryDTO) {

        log.info("Inside addCategory()");

        // 1. Validate name
        if (categoryDTO.getName() == null || categoryDTO.getName().trim().isEmpty()) {
            throw new BadRequestException("Category name is required");
        }

        // 2. Check duplicate
        boolean exists = categoryRepository.existsByNameIgnoreCase(categoryDTO.getName());
        if (exists) {
            throw new BadRequestException("Category name already exists");
        }

        // 3. Save
        Category category = modelMapper.map(categoryDTO, Category.class);
        categoryRepository.save(category);

        return Response.<CategoryDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Category added successfully")
                .build();
    }

    // =========================================================================
    // UPDATE CATEGORY
    // =========================================================================
    @Override
    public Response<CategoryDTO> updateCategory(CategoryDTO categoryDTO) {

        log.info("Inside updateCategory()");

        Category category = categoryRepository.findById(categoryDTO.getId())
                .orElseThrow(() -> new NotFoundException("Category Not Found"));

        // Nếu client truyền name → validate
        if (categoryDTO.getName() != null) {

            String newName = categoryDTO.getName().trim();

            if (newName.isEmpty()) {
                throw new BadRequestException("Category name cannot be empty");
            }

            // check duplicate and must not match itself
            boolean exists = categoryRepository.existsByNameIgnoreCaseAndIdNot(newName, categoryDTO.getId());
            if (exists) {
                throw new BadRequestException("Category name already exists");
            }

            category.setName(newName);
        }

        if (categoryDTO.getDescription() != null)
            category.setDescription(categoryDTO.getDescription());

        categoryRepository.save(category);

        return Response.<CategoryDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Category updated successfully")
                .build();
    }

    // =========================================================================
    // GET BY ID
    // =========================================================================
    @Override
    public Response<CategoryDTO> getCategoryById(Long id) {

        log.info("Inside getCategoryById()");

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category Not Found"));

        CategoryDTO categoryDTO = modelMapper.map(category, CategoryDTO.class);

        return Response.<CategoryDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Category retrieved successfully")
                .data(categoryDTO)
                .build();
    }

    // =========================================================================
    // GET ALL
    // =========================================================================
    @Override
    public Response<List<CategoryDTO>> getAllCategories() {

        log.info("Inside getAllCategories()");
        List<Category> categories = categoryRepository.findAll();

        List<CategoryDTO> categoryDTOS = categories.stream()
                .map(c -> modelMapper.map(c, CategoryDTO.class))
                .toList();

        return Response.<List<CategoryDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("All categories retrieved successfully")
                .data(categoryDTOS)
                .build();
    }

    // =========================================================================
    // DELETE
    // =========================================================================
    @Override
    public Response<?> deleteCategory(Long id) {

        log.info("Inside deleteCategory()");

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category Not Found"));

        boolean linked = menuRepository.existsByCategoryId(id);
        if (linked) {
            throw new BadRequestException("Category is linked to Menu and cannot be deleted");
        }

        categoryRepository.deleteById(id);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Category deleted successfully")
                .build();
    }
}
