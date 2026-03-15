package org.fractalschema;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.fractalschema.analytics.AnalyticsService;
import org.fractalschema.auth.User;
import org.fractalschema.cache.CacheService;
import org.fractalschema.entries.DailyEntry;
import org.fractalschema.enums.Period;
import org.fractalschema.goals.Goal;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ScheduledService {

    private static final Logger LOG = Logger.getLogger(ScheduledService.class);

    @Inject
    CacheService cacheService;

    @Inject
    AnalyticsService analyticsService;

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void calculateDailyStreaks() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // Query 1: all active goals, grouped by user
        List<Goal> activeGoals = Goal.<Goal>find("active = true").list();
        if (activeGoals.isEmpty()) {
            LOG.debug("No active goals — skipping streak calculation");
            return;
        }

        Map<User, List<Goal>> goalsByUser = activeGoals.stream()
                .collect(Collectors.groupingBy(Goal::getUser));

        // Query 2: yesterday's entries for all relevant users in one shot
        List<User> users = List.copyOf(goalsByUser.keySet());
        Map<User, DailyEntry> entriesByUser = DailyEntry.<DailyEntry>find(
                "user IN ?1 and entryDate = ?2", users, yesterday)
                .stream()
                .collect(Collectors.toMap(DailyEntry::getUser, e -> e));

        // Check each user's goals against yesterday's entry, then bust the cache
        for (Map.Entry<User, List<Goal>> entry : goalsByUser.entrySet()) {
            User user = entry.getKey();
            List<Goal> goals = entry.getValue();
            DailyEntry yesterdayEntry = entriesByUser.get(user);

            for (Goal goal : goals) {
                boolean met = yesterdayEntry != null && isGoalMet(goal, yesterdayEntry);
                LOG.debugf("User %s | %s goal: %s",
                        user.getUsername(),
                        goal.getGoalType(),
                        met ? "streak continues" : "streak broken");
            }

            // Invalidate so the next request to /analytics/streaks recomputes accurately
            cacheService.invalidate("user:" + user.getUsername() + ":streaks");
        }

        LOG.infof("Daily streak check complete — %d users processed", goalsByUser.size());
    }

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void cleanupExpiredGoals() {
        LocalDate today = LocalDate.now();

        List<Goal> expiredGoals = Goal.<Goal>find(
                "active = true and endDate < ?1", today).list();

        if (expiredGoals.isEmpty()) {
            LOG.debug("No expired goals to clean up");
            return;
        }

        for (Goal goal : expiredGoals) {
            goal.setActive(false);
            cacheService.invalidate("user:" + goal.getUser().getUsername() + ":goals");
            cacheService.invalidate("user:" + goal.getUser().getUsername() + ":goal:" + goal.id);
            cacheService.invalidate("user:" + goal.getUser().getUsername() + ":streaks");
        }

        LOG.infof("Deactivated %d expired goals", expiredGoals.size());
    }

    @Scheduled(every = "6h")
    @Transactional
    public void warmupPopularCaches() {
        // Active users = those with at least one active goal
        List<User> activeUsers = Goal.<Goal>find("active = true")
                .stream()
                .map(Goal::getUser)
                .distinct()
                .toList();

        if (activeUsers.isEmpty()) {
            LOG.debug("No active users — skipping cache warmup");
            return;
        }

        int warmed = 0;
        for (User user : activeUsers) {
            String userId = user.getUsername();
            try {
                analyticsService.getPeriodSummary(userId, Period.WEEK);
                analyticsService.getPeriodSummary(userId, Period.MONTH);
                warmed++;
            } catch (Exception e) {
                LOG.warnf("Cache warmup failed for user %s: %s", user.getUsername(), e.getMessage());
            }
        }

        LOG.infof("Cache warmup complete — %d/%d users pre-computed", warmed, activeUsers.size());
    }

    private boolean isGoalMet(Goal goal, DailyEntry entry) {
        return switch (goal.getGoalType()) {
            case SLEEP   -> entry.getSleepHours() != null    && entry.getSleepHours() >= goal.getTargetValue();
            case WATER   -> entry.getWaterMl() != null       && entry.getWaterMl() >= goal.getTargetValue();
            case WORKOUT -> Boolean.TRUE.equals(entry.getWorkoutDone());
            case READING -> entry.getReadingMinutes() != null && entry.getReadingMinutes() >= goal.getTargetValue();
            case HOBBY   -> entry.getHobbyDurationMin() != null && entry.getHobbyDurationMin() >= goal.getTargetValue();
        };
    }
}