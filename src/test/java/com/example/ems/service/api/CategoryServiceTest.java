
package com.example.ems.service.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.ems.constant.CategoryType;
import com.example.ems.dto.request.CategoryRequest;
import com.example.ems.dto.response.CategoryResponse;
import com.example.ems.entity.Category;
import com.example.ems.entity.User;
import com.example.ems.exception.OperationNotPermittedException;
import com.example.ems.repository.CategoryRepository;
import com.example.ems.repository.UserRepository;
import com.example.ems.service.api.CategoryService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    private SecurityContext securityContext;
    private Authentication authentication;
    private User mockUser;
    private final String TEST_EMAIL = "user@test.com";

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        mockUser = User.builder()
                .name("Test User")
                .email(TEST_EMAIL)
                .build();
        mockUser.setId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- TEST: GET ALL ---
    @Test
    @DisplayName("Get All: Should return mixed list (Global + Private)")
    void getAllCategories_Success() {
        // GIVEN
        Category globalCat = Category.builder().name("Global").user(null).build();
        Category privateCat = Category.builder().name("Private").user(mockUser).build();

        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(categoryRepository.findAllByUserIdOrGlobal(mockUser.getId())).thenReturn(List.of(globalCat, privateCat));

        // WHEN
        List<CategoryResponse> result = categoryService.getAllCategories();

        // THEN
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(CategoryResponse::isGlobal)); // Check global flag
    }

    // --- TEST: CREATE ---
    @Test
    @DisplayName("Create: Should save category successfully")
    void createCategory_Success() {
        // GIVEN
        CategoryRequest req = new CategoryRequest();
        req.setName("New Cat");
        req.setType("EXPENSE");

        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArguments()[0]);

        // WHEN
        CategoryResponse res = categoryService.createCategory(req);

        // THEN
        assertNotNull(res);
        assertEquals("New Cat", res.getName());
        assertEquals(CategoryType.EXPENSE, res.getType());
        assertFalse(res.isGlobal());
    }

    // --- TEST: UPDATE ---
    @Test
    @DisplayName("Update: Should fail if category belongs to another user")
    void updateCategory_Fail_NotOwner() {
        // GIVEN
        User otherUser = User.builder().email("other@test.com").build();
        otherUser.setId(UUID.randomUUID());
        
        Category otherCat = Category.builder().user(otherUser).build();
        otherCat.setId(UUID.randomUUID());

        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(categoryRepository.findByIdAndIsDeletedFalse(otherCat.getId())).thenReturn(Optional.of(otherCat));

        CategoryRequest req = new CategoryRequest();
        req.setName("Hacked Name");

        // WHEN & THEN
        assertThrows(OperationNotPermittedException.class, () -> {
            categoryService.updateCategory(otherCat.getId(), req);
        });
    }

    @Test
    @DisplayName("Update: Should fail if category is Global")
    void updateCategory_Fail_Global() {
        // GIVEN
        Category globalCat = Category.builder().user(null).build(); // Global
        globalCat.setId(UUID.randomUUID());

        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(categoryRepository.findByIdAndIsDeletedFalse(globalCat.getId())).thenReturn(Optional.of(globalCat));

        CategoryRequest req = new CategoryRequest();

        // WHEN & THEN
        assertThrows(OperationNotPermittedException.class, () -> {
            categoryService.updateCategory(globalCat.getId(), req);
        });
    }

    // --- TEST: DELETE ---
    @Test
    @DisplayName("Delete: Should soft delete successfully")
    void deleteCategory_Success() {
        // GIVEN
        Category myCat = Category.builder().user(mockUser).isDeleted(false).build();
        myCat.setId(UUID.randomUUID());

        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(categoryRepository.findByIdAndIsDeletedFalse(myCat.getId())).thenReturn(Optional.of(myCat));

        // WHEN
        categoryService.deleteCategory(myCat.getId());

        // THEN
        assertTrue(myCat.getIsDeleted()); // Verify soft delete flag set to true
        verify(categoryRepository, times(1)).save(myCat);
    }
}