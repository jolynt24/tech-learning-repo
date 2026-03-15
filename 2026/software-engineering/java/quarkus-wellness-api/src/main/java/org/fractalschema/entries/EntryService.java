package org.fractalschema.entries;

import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.fractalschema.auth.User;
import org.fractalschema.cache.CacheService;
import org.fractalschema.dto.request.CreateEntryRequest;
import org.fractalschema.dto.request.MealRequest;
import org.fractalschema.dto.request.UpdateEntryRequest;
import org.fractalschema.dto.response.EntryResponse;
import org.fractalschema.enums.ErrorCode;
import org.fractalschema.exceptions.CustomExceptions;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EntryService {

    @Inject
    SecurityIdentity identity;

    @Inject
    CacheService cacheService;

    private User getCurrentUser() {
        return User.findByUsername(identity.getPrincipal().getName());
    }

    private void addMealsToEntry(List<MealRequest> mealRequests, DailyEntry entry) {
        if (mealRequests == null || mealRequests.isEmpty()) return;
        for (MealRequest mealRequest : mealRequests) {
            Meal meal = new Meal();
            meal.setMealType(mealRequest.getMealType());
            meal.setDescription(mealRequest.getDescription());
            meal.setCalories(mealRequest.getCalories());
            meal.setEntry(entry);
            entry.getMeals().add(meal);
        }
    }

    @Transactional
    public EntryResponse createEntry(CreateEntryRequest entryRequest) {
        LocalDate entryDate = entryRequest.getEntryDate();
        User user = getCurrentUser();

        DailyEntry.<DailyEntry>find("user=?1 and entryDate=?2", user, entryDate).firstResultOptional()
                .ifPresent(e -> { throw new CustomExceptions(ErrorCode.DUPLICATE_ENTRY); });

        DailyEntry entry = new DailyEntry();
        entry.setUser(user);
        entry.setEntryDate(entryDate);
        entry.setSleepHours(entryRequest.getSleepHours());
        entry.setSleepQuality(entryRequest.getSleepQuality());
        entry.setWaterMl(entryRequest.getWaterMl());
        entry.setWorkoutDone(entryRequest.getWorkoutDone());
        entry.setWorkoutType(entryRequest.getWorkoutType());
        entry.setWorkoutDurationMin(entryRequest.getWorkoutDurationMin());
        entry.setReadingMinutes(entryRequest.getReadingMinutes());
        entry.setReadingPages(entryRequest.getReadingPages());
        entry.setReadingBook(entryRequest.getReadingBook());
        entry.setHobbyActivity(entryRequest.getHobbyActivity());
        entry.setHobbyDurationMin(entryRequest.getHobbyDurationMin());
        entry.setMoodRating(entryRequest.getMoodRating());
        entry.setNotes(entryRequest.getNotes());

        addMealsToEntry(entryRequest.getMeals(), entry);

        entry.persist();
        cacheService.invalidate("user:" + user.getUsername() + ":streaks");

        return EntryResponse.from(entry);
    }

    @Transactional
    public EntryResponse getEntry(LocalDate date) {
        User user = getCurrentUser();

        String cacheKey = "user:" + user.getUsername() + ":entry:" + date;
        Optional<EntryResponse> cached = cacheService.get(cacheKey, new TypeReference<>() {});
        if (cached.isPresent()) return cached.get();

        DailyEntry entry = DailyEntry.<DailyEntry>find("user=?1 and entryDate=?2", user, date)
                .firstResultOptional().orElseThrow(() -> new CustomExceptions(ErrorCode.ENTRY_NOT_FOUND));
        EntryResponse response = EntryResponse.from(entry);

        cacheService.set(cacheKey, response, 24 * 3600);
        return response;
    }

    @Transactional
    public List<EntryResponse> getEntry(LocalDate from, LocalDate to) {
        User user = getCurrentUser();

        String cacheKey = "user:" + user.getUsername() + ":entries:range:from:" + from + ":to:" + to;
        Optional<List<EntryResponse>> cached = cacheService.get(cacheKey, new TypeReference<>() {});
        if (cached.isPresent()) return cached.get();

        List<EntryResponse> responses = DailyEntry.<DailyEntry>find(
                "user=?1 and entryDate >= ?2 and entryDate <= ?3 order by entryDate asc", user, from, to)
                .stream().map(EntryResponse::from).toList();

        // Short TTL — range caches cannot be precisely invalidated on mutation
        cacheService.set(cacheKey, responses, 3600);
        return responses;
    }

    @Transactional
    public List<EntryResponse> getEntry(int limit) {
        User user = getCurrentUser();
        return DailyEntry.<DailyEntry>find("user=?1 order by entryDate desc", user)
                .page(0, limit)
                .stream().map(EntryResponse::from).toList();
    }

    @Transactional
    public EntryResponse updateEntry(LocalDate date, UpdateEntryRequest entryRequest) {
        User user = getCurrentUser();
        DailyEntry entry = DailyEntry.<DailyEntry>find("user=?1 and entryDate=?2", user, date)
                .firstResultOptional().orElseThrow(() -> new CustomExceptions(ErrorCode.ENTRY_NOT_FOUND));

        Optional.ofNullable(entryRequest.getSleepHours()).ifPresent(entry::setSleepHours);
        Optional.ofNullable(entryRequest.getSleepQuality()).ifPresent(entry::setSleepQuality);
        Optional.ofNullable(entryRequest.getWaterMl()).ifPresent(entry::setWaterMl);
        Optional.ofNullable(entryRequest.getWorkoutDone()).ifPresent(entry::setWorkoutDone);
        Optional.ofNullable(entryRequest.getWorkoutType()).ifPresent(entry::setWorkoutType);
        Optional.ofNullable(entryRequest.getWorkoutDurationMin()).ifPresent(entry::setWorkoutDurationMin);
        Optional.ofNullable(entryRequest.getReadingMinutes()).ifPresent(entry::setReadingMinutes);
        Optional.ofNullable(entryRequest.getReadingPages()).ifPresent(entry::setReadingPages);
        Optional.ofNullable(entryRequest.getReadingBook()).ifPresent(entry::setReadingBook);
        Optional.ofNullable(entryRequest.getHobbyActivity()).ifPresent(entry::setHobbyActivity);
        Optional.ofNullable(entryRequest.getHobbyDurationMin()).ifPresent(entry::setHobbyDurationMin);
        Optional.ofNullable(entryRequest.getMoodRating()).ifPresent(entry::setMoodRating);
        Optional.ofNullable(entryRequest.getNotes()).ifPresent(entry::setNotes);

        entry.getMeals().clear();
        addMealsToEntry(entryRequest.getMeals(), entry);

        cacheService.invalidate("user:" + user.getUsername() + ":entry:" + date);
        cacheService.invalidate("user:" + user.getUsername() + ":streaks");
        return EntryResponse.from(entry);
    }

    @Transactional
    public void deleteEntry(LocalDate date) {
        User user = getCurrentUser();
        long count = DailyEntry.delete("user=?1 and entryDate=?2", user, date);
        if (count == 0L) {
            throw new CustomExceptions(ErrorCode.ENTRY_NOT_FOUND);
        }
        cacheService.invalidate("user:" + user.getUsername() + ":entry:" + date);
        cacheService.invalidate("user:" + user.getUsername() + ":streaks");
    }
}