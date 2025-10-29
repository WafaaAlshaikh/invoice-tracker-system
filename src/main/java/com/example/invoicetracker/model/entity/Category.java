    package com.example.invoicetracker.model.entity;

    import jakarta.persistence.*;
    import jakarta.validation.constraints.*;
    import lombok.*;
    import java.util.List;
    
    import com.example.invoicetracker.model.entity.base.BaseEntity;
    @Entity
    @Table(name = "category")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class Category extends BaseEntity{

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "category_id")
        private Long categoryId;

        @Column(name = "category_code", length = 50, unique = true, nullable = false)
        @NotBlank(message = "Category code is required")
        @Size(max = 50)
        private String categoryCode;

        @Column(name = "category_name", length = 100, nullable = false)
        @NotBlank(message = "Category name is required")
        @Size(max = 100)
        private String categoryName;

        @Column(name = "description", columnDefinition = "TEXT")
        private String description;

        
        @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Product> product;
        
    }
