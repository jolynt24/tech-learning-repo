package org.fractalschema.entries;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.fractalschema.enums.MealType;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "meals")
public class Meal extends PanacheEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "entry_id", nullable = false)
    private DailyEntry entry;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @Size(max = 255)
    @NotNull
    @Column(name = "description", nullable = false)
    private String description;

    @Min(0)
    @Column(name = "calories")
    private Integer calories;

    @NotNull
    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;

    @PrePersist
    protected void onCreate() {
        loggedAt = Instant.now();
    }
}