package com.example.invoicetracker.model.entity;

import com.example.invoicetracker.model.converter.JpaJsonConverter;
import com.example.invoicetracker.model.enums.ActionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 20, nullable = false)
    private ActionType actionType;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "old_values", columnDefinition = "JSON")
    @Convert(converter = JpaJsonConverter.class)
    private Map<String, Object> oldValues;

    @Column(name = "new_values", columnDefinition = "JSON")
    @Convert(converter = JpaJsonConverter.class)
    private Map<String, Object> newValues;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
