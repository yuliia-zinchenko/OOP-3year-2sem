package com.library.repository;

import com.library.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("""
        select distinct b from Book b left join b.authors a
        where lower(b.title) like lower(concat('%', :q, '%'))
           or lower(coalesce(b.isbn, '')) like lower(concat('%', :q, '%'))
           or lower(a.fullName) like lower(concat('%', :q, '%'))
        order by b.title
    """)
    List<Book> search(@Param("q") String q);

    @Override
    @Query("select b from Book b left join fetch b.authors")
    List<Book> findAll();

    @Query("select b from Book b left join fetch b.authors where b.id = :id")
    Optional<Book> findByIdWithAuthors(@Param("id") Long id);
}
