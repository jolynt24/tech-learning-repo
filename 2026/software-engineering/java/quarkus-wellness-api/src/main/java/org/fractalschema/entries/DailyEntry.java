package org.fractalschema.entries;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.fractalschema.auth.User;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "daily_entries", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "entry_date"})
})
public class DailyEntry extends PanacheEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("24.0")
    @Column(name = "sleep_hours")
    private Double sleepHours;

    @Min(1) @Max(5)
    @Column(name = "sleep_quality")
    private Integer sleepQuality;

    @Min(0)
    @Column(name = "water_ml")
    private Integer waterMl;

    @Column(name = "workout_done")
    private Boolean workoutDone;

    @Size(max = 100)
    @Column(name = "workout_type", length = 100)
    private String workoutType;

    @Min(0)
    @Column(name = "workout_duration_min")
    private Integer workoutDurationMin;

    @Min(0)
    @Column(name = "reading_minutes")
    private Integer readingMinutes;

    @Min(0)
    @Column(name = "reading_pages")
    private Integer readingPages;

    @Size(max = 255)
    @Column(name = "reading_book")
    private String readingBook;

    @Size(max = 255)
    @Column(name = "hobby_activity")
    private String hobbyActivity;

    @Min(0)
    @Column(name = "hobby_duration_min")
    private Integer hobbyDurationMin;

    @Min(1) @Max(5)
    @Column(name = "mood_rating")
    private Integer moodRating;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Meal> meals = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}