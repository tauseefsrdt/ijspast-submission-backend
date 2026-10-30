package com.ijspast.journal.repository;

import com.ijspast.journal.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, String> {

    Optional<Author> findByEmailIgnoreCase(String email);

    Optional<Author> findByUserId(String userId);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Author> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, String id);

    boolean existsByPhoneAndIdNot(String phone, String id);

    @Query("SELECT DISTINCT a FROM Author a LEFT JOIN FETCH a.manuscripts ORDER BY a.name ASC")
    List<Author> findAllWithManuscripts();
}
