package com.example.sesac_library.repository;

import com.example.sesac_library.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author);

    List<Book> findByCategoryId(Long categoryId);

    @Query("SELECT b FROM Book b WHERE b.bookCopies > 0")
    List<Book> findAvailableBooks();

    @Query("SELECT b FROM Book b WHERE b.bookCopies = 0")
    List<Book> findBorrowedBooks();

    @Query("SELECT b FROM Book b WHERE " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR b.categoryId = :categoryId)")
    List<Book> searchBooks(@Param("keyword") String keyword, @Param("categoryId") Long categoryId);
}