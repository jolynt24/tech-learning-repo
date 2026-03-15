package org.fractalschema.analytics;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.fractalschema.auth.User;
import org.fractalschema.cache.CacheService;
import org.fractalschema.dto.response.DataPoint;
import org.fractalschema.dto.response.StreakResponse;
import org.fractalschema.dto.response.SummaryResponse;
import org.fractalschema.dto.response.TrendResponse;
import org.fractalschema.entries.DailyEntry;
import org.fractalschema.enums.ErrorCode;
import org.fractalschema.enums.GoalType;
import org.fractalschema.enums.Period;
import org.fractalschema.enums.TrendDirection;
import org.fractalschema.exceptions.CustomExceptions;
import org.fractalschema.goals.Goal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class AnalyticsService {

    @Inject
    CacheService cacheService;

    public List<StreakResponse> getCurrentStreaks(String userId) {
        String cacheKey = "user:" + userId + ":streaks";
        Optional<List<StreakResponse>> cached = cacheService.get(cacheKey, new TypeReference<>() {});
        if (cached.isPresent()) return cached.get();

        User user = User.findByUsername(userId);
        if (user == null) throw new CustomExceptions(ErrorCode.VALIDATION_ERROR);

        // Fetch all active goals in one query and index by type
        Map<GoalType, Goal> goalByType = Goal.<Goal>find("user=?1 and active=true", user)
                .stream()
                .collect(Collectors.toMap(Goal::getGoalType, Function.identity(), (a, b) -> a));
        if (goalByType.isEmpty()) return List.of();

        // Fetch up to 365 days of entries in one query instead of one query per day
        LocalDate today = LocalDate.now();
        Map<LocalDate, DailyEntry> entriesByDate = DailyEntry.<DailyEntry>find(
                "user=?1 and entryDate >= ?2 order by entryDate desc",
                user, today.minusDays(365))
                .stream()
                .collect(Collectors.toMap(DailyEntry::getEntryDate, Function.identity()));

        List<StreakResponse> responses = new ArrayList<>();
        for (GoalType type : GoalType.values()) {
            Goal goal = goalByType.get(type);
            if (goal == null) continue;

            long currentStreak = 0L;
            LocalDate day = today.minusDays(1);
            while (true) {
                DailyEntry entry = entriesByDate.get(day);
                if (entry == null || !isGoalMet(type, entry, goal)) break;
                currentStreak++;
                day = day.minusDays(1);
            }

            DailyEntry todayEntry = entriesByDate.get(today);
            boolean activeToday = todayEntry != null && isGoalMet(type, todayEntry, goal);

            responses.add(StreakResponse.builder()
                    .goalType(type)
                    .currentStreak(currentStreak)
                    .longestStreak(currentStreak)
                    .activeToday(activeToday)
                    .calculatedAt(Instant.now())
                    .build());
        }

        cacheService.set(cacheKey, responses, 3600);
        return responses;
    }

    private boolean isGoalMet(GoalType type, DailyEntry entry, Goal goal) {
        return switch (type) {
            case SLEEP   -> entry.getSleepHours() != null    && entry.getSleepHours() >= goal.getTargetValue();
            case WATER   -> entry.getWaterMl() != null       && entry.getWaterMl() >= goal.getTargetValue();
            case WORKOUT -> Boolean.TRUE.equals(entry.getWorkoutDone());
            case READING -> entry.getReadingMinutes() != null && entry.getReadingMinutes() >= goal.getTargetValue();
            case HOBBY   -> entry.getHobbyDurationMin() != null && entry.getHobbyDurationMin() >= goal.getTargetValue();
        };
    }

    private List<DataPoint> getDataPoints(GoalType goalType, List<DailyEntry> entries) {
        Function<DailyEntry, Double> getter = switch (goalType) {
            case SLEEP -> DailyEntry::getSleepHours;
            case WATER -> e -> e.getWaterMl() != null ? e.getWaterMl().doubleValue() : null;
            case WORKOUT -> e -> e.getWorkoutDurationMin() != null ? e.getWorkoutDurationMin().doubleValue() : null;
            case READING -> e -> e.getReadingMinutes() != null ? e.getReadingMinutes().doubleValue() : null;
            case HOBBY -> e -> e.getHobbyDurationMin() != null ? e.getHobbyDurationMin().doubleValue() : null;
        };
        List<DataPoint> dataPoints = new ArrayList<>();
        for (DailyEntry entry : entries) {
            Double value = getter.apply(entry);
            if (value != null) {
                dataPoints.add(DataPoint.builder().value(value).time(entry.getEntryDate()).build());
            }
        }
        return dataPoints;
    }

    private TrendDirection getTrendDirection(List<DataPoint> dataPoints) {
        if (dataPoints.size() <= 1) return TrendDirection.STABLE;

        int mid = dataPoints.size() / 2;
        double firstHalfAvg = dataPoints.subList(0, mid).stream().mapToDouble(DataPoint::getValue).average().orElse(0.0);
        double secondHalfAvg = dataPoints.subList(mid, dataPoints.size()).stream().mapToDouble(DataPoint::getValue).average().orElse(0.0);
        double threshold = Math.max(firstHalfAvg, secondHalfAvg) * 0.05;

        if (secondHalfAvg > firstHalfAvg + threshold) return TrendDirection.INCREASING;
        if (secondHalfAvg < firstHalfAvg - threshold) return TrendDirection.DECREASING;
        return TrendDirection.STABLE;
    }

    public TrendResponse getTrendAnalysis(String userId, GoalType metric, long period) {
        String cacheKey = "user:" + userId + ":trends:" + metric + ":" + period;
        Optional<TrendResponse> cached = cacheService.get(cacheKey, new TypeReference<>() {});
        if (cached.isPresent()) return cached.get();

        LocalDate today = LocalDate.now();
        List<DailyEntry> entries = DailyEntry.<DailyEntry>find(
                "user.username=?1 and entryDate <= ?2 and entryDate >= ?3 order by entryDate asc",
                userId, today, today.minusDays(period * 2L))
                .stream().toList();
        List<DataPoint> dp = getDataPoints(metric, entries);

        // Single pass for all aggregate stats
        DoubleSummaryStatistics stats = dp.stream().mapToDouble(DataPoint::getValue).summaryStatistics();

        TrendResponse response = TrendResponse.builder()
                .metric(metric).period(period)
                .average(stats.getCount() > 0 ? stats.getAverage() : 0.0)
                .min(stats.getCount() > 0 ? stats.getMin() : 0.0)
                .max(stats.getCount() > 0 ? stats.getMax() : 0.0)
                .trendDirection(getTrendDirection(dp))
                .dataPoints(dp)
                .build();
        cacheService.set(cacheKey, response, 3600);
        return response;
    }

    public SummaryResponse getPeriodSummary(String userId, Period period) {
        String cacheKey = "user:" + userId + (period == Period.WEEK ? ":weekly-summary" : ":monthly-summary");
        int ttl = period == Period.WEEK ? 6 * 3600 : 12 * 3600;
        Optional<SummaryResponse> cached = cacheService.get(cacheKey, new TypeReference<>() {});
        if (cached.isPresent()) return cached.get();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(period.getDays());
        List<DailyEntry> entries = DailyEntry.<DailyEntry>find(
                "user.username=?1 and entryDate <= ?2 and entryDate >= ?3 order by entryDate asc",
                userId, endDate, startDate)
                .stream().toList();

        // Single pass through entries instead of one stream per field
        double sleepSum = 0; int sleepCount = 0;
        long waterSum = 0; int waterCount = 0;
        long workoutDays = 0, workoutDuration = 0, readingMinutes = 0, hobbyMinutes = 0;
        double moodSum = 0; int moodCount = 0;

        for (DailyEntry e : entries) {
            if (e.getSleepHours() != null)      { sleepSum += e.getSleepHours(); sleepCount++; }
            if (e.getWaterMl() != null)          { waterSum += e.getWaterMl(); waterCount++; }
            if (Boolean.TRUE.equals(e.getWorkoutDone())) workoutDays++;
            if (e.getWorkoutDurationMin() != null) workoutDuration += e.getWorkoutDurationMin();
            if (e.getReadingMinutes() != null)   readingMinutes += e.getReadingMinutes();
            if (e.getHobbyDurationMin() != null) hobbyMinutes += e.getHobbyDurationMin();
            if (e.getMoodRating() != null)       { moodSum += e.getMoodRating(); moodCount++; }
        }

        SummaryResponse response = SummaryResponse.builder()
                .period(period)
                .startDate(startDate)
                .endDate(endDate)
                .totalEntries(entries.size())
                .avgSleepHours(sleepCount > 0 ? sleepSum / sleepCount : 0.0)
                .avgWaterMl(waterCount > 0 ? (int) (waterSum / waterCount) : 0)
                .workoutDays(workoutDays)
                .totalWorkoutDuration(workoutDuration)
                .totalReadingMinutes(readingMinutes)
                .totalHobbyMinutes(hobbyMinutes)
                .avgMoodRating(moodCount > 0 ? moodSum / moodCount : 0.0)
                .streaks(getCurrentStreaks(userId))
                .build();

        cacheService.set(cacheKey, response, ttl);
        return response;
    }
}