package com.ijspast.journal.repository;

import com.ijspast.journal.entity.User;
import com.ijspast.journal.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, String id);

    boolean existsByPhoneAndIdNot(String phone, String id);

    List<User> findByRole(UserRole role);

    List<User> findByRoleOrderByNameAsc(UserRole role);

    Optional<User> findByEmailIgnoreCaseAndRole(String email, UserRole role);

    Optional<User> findByPhoneAndRole(String phone, UserRole role);
}
