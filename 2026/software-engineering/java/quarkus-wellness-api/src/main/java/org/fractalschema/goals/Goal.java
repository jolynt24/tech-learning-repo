package org.fractalschema.goals;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.fractalschema.auth.User;
import org.fractalschema.enums.GoalFrequency;
import org.fractalschema.enums.GoalType;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "goals")
public class Goal extends PanacheEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 20)
    private GoalType goalType;

    @NotNull
    @Column(name = "target_value", nullable = false)
    private BigDecimal targetValue;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private GoalFrequency frequency;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ColumnDefault("true")
    @Column(name = "active", nullable = false)
    private boolean active;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

}