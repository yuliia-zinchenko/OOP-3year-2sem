package com.library.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    private Long id;
    private String title;
    private String isbn;
    private Integer year;
    private Integer totalCopies;
    private Integer availableCopies;
    @Builder.Default
    private List<Author> authors = new ArrayList<>();
}
