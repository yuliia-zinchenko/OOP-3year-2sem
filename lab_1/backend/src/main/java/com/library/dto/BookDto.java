package com.library.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDto {
    private Long id;
    private String title;
    private String isbn;
    private Integer year;
    private Integer totalCopies;
    private Integer availableCopies;
    private List<String> authorNames;
}
