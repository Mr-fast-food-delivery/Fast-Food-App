package unit;

import com.phegon.FoodApp.exceptions.BadRequestException;
import com.phegon.FoodApp.exceptions.NotFoundException;
import com.phegon.FoodApp.response.Response;
import com.phegon.FoodApp.role.dtos.RoleDTO;
import com.phegon.FoodApp.role.entity.Role;
import com.phegon.FoodApp.role.repository.RoleRepository;
import com.phegon.FoodApp.role.services.RoleServiceImpl;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    RoleRepository roleRepository;

    @Mock
    org.modelmapper.ModelMapper modelMapper;

    @InjectMocks
    RoleServiceImpl roleService;

    RoleDTO mockDto() {
        RoleDTO dto = new RoleDTO();
        dto.setId(1L);
        dto.setName("ADMIN");
        return dto;
    }

    Role mockRole() {
        Role role = new Role();
        role.setId(1L);
        role.setName("ADMIN");
        return role;
    }

    // =====================================================
    // A. CREATE ROLE TESTS (5 TEST CASES)
    // =====================================================
    @Nested
    @DisplayName("CREATE ROLE TESTS")
    class CreateRoleTests {

        @Test
        void createRole_Success() {
            RoleDTO dto = mockDto();
            Role role = mockRole();

            when(modelMapper.map(dto, Role.class)).thenReturn(role);
            when(roleRepository.save(role)).thenReturn(role);
            when(modelMapper.map(role, RoleDTO.class)).thenReturn(dto);

            Response<RoleDTO> res = roleService.createRole(dto);

            assertEquals(200, res.getStatusCode());
            assertEquals("ADMIN", res.getData().getName());
        }

        @Test
        void createRole_ModelMapper_ToEntity_Fails() {
            RoleDTO dto = mockDto();
            when(modelMapper.map(dto, Role.class)).thenThrow(new RuntimeException("mapper error"));

            assertThrows(RuntimeException.class,
                    () -> roleService.createRole(dto));
        }

        @Test
        void createRole_SaveFails() {
            RoleDTO dto = mockDto();
            Role role = mockRole();

            when(modelMapper.map(dto, Role.class)).thenReturn(role);
            when(roleRepository.save(role)).thenThrow(new RuntimeException("db error"));

            assertThrows(RuntimeException.class,
                    () -> roleService.createRole(dto));
        }

        @Test
        void createRole_ModelMapper_ToDTO_Fails() {
            RoleDTO dto = mockDto();
            Role role = mockRole();

            when(modelMapper.map(dto, Role.class)).thenReturn(role);
            when(roleRepository.save(role)).thenReturn(role);
            when(modelMapper.map(role, RoleDTO.class))
                    .thenThrow(new RuntimeException("mapper dto error"));

            assertThrows(RuntimeException.class,
                    () -> roleService.createRole(dto));
        }

        @Test
        void createRole_FieldMappingCorrect() {
            RoleDTO dto = new RoleDTO();
            dto.setName("ADMIN");

            Role mapped = new Role();
            mapped.setName("ADMIN");

            // map DTO -> Entity
            when(modelMapper.map(any(RoleDTO.class), eq(Role.class)))
                    .thenReturn(mapped);

            // simulate DB saved entity
            Role saved = new Role();
            saved.setId(1L);
            saved.setName("ADMIN");

            when(roleRepository.save(any(Role.class))).thenReturn(saved);

            // map Entity -> DTO
            when(modelMapper.map(any(Role.class), eq(RoleDTO.class)))
                    .thenReturn(dto);

            Response<RoleDTO> response = roleService.createRole(dto);

            assertEquals("ADMIN", response.getData().getName());
        }

    }

    // =====================================================
    // B. UPDATE ROLE TESTS (6 TEST CASES)
    // =====================================================
    @Nested
    @DisplayName("UPDATE ROLE TESTS")
    class UpdateRoleTests {

        @Test
        void updateRole_Success() {
            RoleDTO dto = mockDto();
            Role existing = mockRole();
            Role updated = mockRole();

            when(roleRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.empty());
            when(roleRepository.save(existing)).thenReturn(updated);
            when(modelMapper.map(updated, RoleDTO.class)).thenReturn(dto);

            Response<RoleDTO> res = roleService.updateRole(dto);
            assertEquals(200, res.getStatusCode());
        }

        @Test
        void updateRole_RoleNotFound() {
            RoleDTO dto = mockDto();
            when(roleRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> roleService.updateRole(dto));
        }

        @Test
        void updateRole_RoleNameAlreadyExists() {
            RoleDTO dto = mockDto();
            Role existing = mockRole();

            when(roleRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(roleRepository.findByName("ADMIN"))
                    .thenReturn(Optional.of(new Role()));

            assertThrows(BadRequestException.class,
                    () -> roleService.updateRole(dto));
        }

        @Test
        void updateRole_SaveFails() {
            RoleDTO dto = mockDto();
            Role existing = mockRole();

            when(roleRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.empty());
            when(roleRepository.save(existing)).thenThrow(new RuntimeException("db error"));

            assertThrows(RuntimeException.class,
                    () -> roleService.updateRole(dto));
        }

        @Test
        void updateRole_ModelMapperFails() {
            RoleDTO dto = mockDto();
            Role existing = mockRole();

            when(roleRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.empty());
            when(roleRepository.save(existing)).thenReturn(existing);

            when(modelMapper.map(existing, RoleDTO.class))
                    .thenThrow(new RuntimeException("mapper error"));

            assertThrows(RuntimeException.class,
                    () -> roleService.updateRole(dto));
        }

        @Test
        void updateRole_FieldMappingCorrect() {
            RoleDTO dto = new RoleDTO();
            dto.setId(1L);
            dto.setName("MANAGER");

            Role existing = mockRole();

            when(roleRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(roleRepository.findByName("MANAGER")).thenReturn(Optional.empty());
            when(roleRepository.save(any())).thenReturn(existing);
            when(modelMapper.map(any(), eq(RoleDTO.class))).thenReturn(dto);

            roleService.updateRole(dto);

            assertEquals("MANAGER", existing.getName());
        }
    }

    // =====================================================
    // C. GET ALL ROLES TESTS (3 TEST CASES)
    // =====================================================
    @Nested
    @DisplayName("GET ALL ROLES TESTS")
    class GetAllRolesTests {

        @Test
        void getAllRoles_Success() {
            List<Role> roles = List.of(mockRole());
            RoleDTO dto = mockDto();

            when(roleRepository.findAll()).thenReturn(roles);
            when(modelMapper.map(any(Role.class), eq(RoleDTO.class))).thenReturn(dto);

            Response<List<RoleDTO>> res = roleService.getAllRoles();

            assertEquals(200, res.getStatusCode());
            assertEquals(1, res.getData().size());
        }

        @Test
        void getAllRoles_EmptyList() {
            when(roleRepository.findAll()).thenReturn(Collections.emptyList());

            Response<List<RoleDTO>> res = roleService.getAllRoles();

            assertTrue(res.getData().isEmpty());
        }

        @Test
        void getAllRoles_ModelMapperFails() {
            List<Role> roles = List.of(mockRole());

            when(roleRepository.findAll()).thenReturn(roles);
            when(modelMapper.map(any(Role.class), eq(RoleDTO.class)))
                    .thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class,
                    () -> roleService.getAllRoles());
        }
    }

    // =====================================================
    // D. DELETE ROLE TESTS (3 TEST CASES)
    // =====================================================
    @Nested
    @DisplayName("DELETE ROLE TESTS")
    class DeleteRoleTests {

        @Test
        void deleteRole_Success() {
            when(roleRepository.existsById(1L)).thenReturn(true);
            doNothing().when(roleRepository).deleteById(1L);

            Response<?> res = roleService.deleteRole(1L);

            assertEquals(200, res.getStatusCode());
            verify(roleRepository).deleteById(1L);
        }

        @Test
        void deleteRole_NotFound() {
            when(roleRepository.existsById(1L)).thenReturn(false);

            assertThrows(NotFoundException.class,
                    () -> roleService.deleteRole(1L));
        }

        @Test
        void deleteRole_DeleteFails() {
            when(roleRepository.existsById(1L)).thenReturn(true);
            doThrow(new RuntimeException("delete error"))
                    .when(roleRepository).deleteById(1L);

            assertThrows(RuntimeException.class,
                    () -> roleService.deleteRole(1L));
        }
    }

}
