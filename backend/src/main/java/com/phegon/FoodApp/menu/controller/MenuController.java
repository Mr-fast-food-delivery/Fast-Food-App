package com.phegon.FoodApp.menu.controller;

import com.phegon.FoodApp.menu.dtos.MenuDTO;
import com.phegon.FoodApp.menu.services.MenuService;
import com.phegon.FoodApp.response.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<?>> createMenu(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam Long categoryId,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        MenuDTO dto = new MenuDTO();
        dto.setName(name);
        dto.setDescription(description);
        dto.setPrice(price);
        dto.setCategoryId(categoryId);
        dto.setImageFile(imageFile);

        return ResponseEntity.ok(menuService.createMenu(dto));
    }


    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<MenuDTO>> updateMenu(
            @RequestParam Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam Long categoryId,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        MenuDTO dto = new MenuDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setDescription(description);
        dto.setPrice(price);
        dto.setCategoryId(categoryId);
        dto.setImageFile(imageFile);

        return ResponseEntity.ok(menuService.updateMenu(dto));
    }


    @GetMapping("/{id}")
    public ResponseEntity<Response<MenuDTO>> getMenuById(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.getMenuById(id));
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<?>> deleteMenu(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.deleteMenu(id));
    }


    @GetMapping
    public ResponseEntity<Response<List<MenuDTO>>> getMenus(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(menuService.getMenus(categoryId, search));
    }

}
