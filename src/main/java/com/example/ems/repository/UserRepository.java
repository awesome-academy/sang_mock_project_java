package com.example.ems.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;

import com.example.ems.dto.response.UserSelectDto;
import com.example.ems.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.persistence.QueryHint;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> searchUsers(String keyword, Boolean isActive, Pageable pageable);
    
    @Query("SELECT new com.example.ems.dto.response.UserSelectDto(u.id, u.name) FROM User u ORDER BY u.name ASC")
    List<UserSelectDto> findAllUserForSelect();
    
    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:isActive IS NULL OR u.isActive = :isActive)")
     List<User> searchUsersForExport(@Param("keyword") String keyword, @Param("isActive") Boolean isActive);

    @QueryHints(value = {
        @QueryHint(name = HINT_FETCH_SIZE, value = "50"),
        @QueryHint(name = HINT_READ_ONLY, value = "true")
    })
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive)")
    Stream<User> streamUsersForExport(@Param("keyword") String keyword, @Param("isActive") Boolean isActive);

    @Query("SELECT u.email FROM User u")
    List<String> findAllEmails();
}