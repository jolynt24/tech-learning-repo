package org.fractalschema.goals;

import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.fractalschema.auth.User;
import org.fractalschema.cache.CacheService;
import org.fractalschema.dto.request.CreateGoalRequest;
import org.fractalschema.dto.request.UpdateGoalRequest;
import org.fractalschema.dto.response.GoalResponse;
import org.fractalschema.enums.ErrorCode;
import org.fractalschema.exceptions.CustomExceptions;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GoalService {

    @Inject
    SecurityIdentity identity;

    @Inject
    CacheService cacheService;

    private User getCurrentUser() {
        User user = User.findByUsername(identity.getPrincipal().getName());
        if (user == null) throw new CustomExceptions(ErrorCode.INVALID_CREDENTIALS);
        return user;
    }

    private GoalResponse toResponse(Goal goal) {
        return GoalResponse.builder()
            .id(goal.id)
            .goalLabel(goal.getGoalType().getLabel())
            .goalMetrics(goal.getGoalType().getMetrics())
            .target(goal.getTargetValue())
            .goalFrequency(goal.getFrequency().getLabel())
            .frequencyDays(goal.getFrequency().getDays())
            .active(goal.isActive())
            .startDate(goal.getStartDate())
            .endDate(goal.getEndDate())
            .createdAt(goal.getCreatedAt())
            .build();
    }

    private void invalidateGoalCaches(String username, Long goalId) {
        cacheService.invalidate("user:" + username + ":goals");
        cacheService.invalidate("user:" + username + ":goal:" + goalId);
        cacheService.invalidate("user:" + username + ":streaks");
    }

    @Transactional
    public GoalResponse createGoal(CreateGoalRequest goalRequest) {
        User user = getCurrentUser();

        Goal goal = new Goal();
        goal.setUser(user);
        goal.setGoalType(goalRequest.getGoalType());
        goal.setTargetValue(goalRequest.getTarget());
        goal.setFrequency(goalRequest.getGoalFrequency());
        goal.setStartDate(goalRequest.getStartDate());
        goal.setEndDate(goalRequest.getEndDate());
        goal.setActive(true);
        goal.persist();

        cacheService.invalidate("user:" + user.getUsername() + ":goals");
        cacheService.invalidate("user:" + user.getUsername() + ":streaks");

        return toResponse(goal);
    }

    @Transactional
    public GoalResponse updateGoal(Long id, UpdateGoalRequest request) {
        User user = getCurrentUser();

        // Null check must come before any field access
        Goal goal = Goal.findById(id);
        if (goal == null || !goal.getUser().id.equals(user.id)) {
            throw new CustomExceptions(ErrorCode.GOAL_NOT_FOUND);
        }

        Optional.ofNullable(request.getGoalType()).ifPresent(goal::setGoalType);
        Optional.ofNullable(request.getTargetValue()).ifPresent(goal::setTargetValue);
        Optional.ofNullable(request.getFrequency()).ifPresent(goal::setFrequency);
        Optional.ofNullable(request.getStartDate()).ifPresent(goal::setStartDate);
        Optional.ofNullable(request.getEndDate()).ifPresent(goal::setEndDate);
        Optional.ofNullable(request.getActive()).ifPresent(goal::setActive);

        invalidateGoalCaches(user.getUsername(), id);
        return toResponse(goal);
    }

    @Transactional
    public List<GoalResponse> getAllGoals() {
        User user = getCurrentUser();

        String cacheKey = "user:" + user.getUsername() + ":goals";
        Optional<List<GoalResponse>> cached = cacheService.get(cacheKey, new TypeReference<>() {});
        if (cached.isPresent()) return cached.get();

        List<GoalResponse> responses = Goal.<Goal>find("user = ?1 order by startDate", user)
                .stream().map(this::toResponse).toList();

        cacheService.set(cacheKey, responses, 3600);
        return responses;
    }

    @Transactional
    public GoalResponse getGoal(Long id) {
        User user = getCurrentUser();

        String cacheKey = "user:" + user.getUsername() + ":goal:" + id;
        Optional<GoalResponse> cached = cacheService.get(cacheKey, new TypeReference<>() {});
        if (cached.isPresent()) return cached.get();

        Goal goal = Goal.<Goal>find("user = ?1 and id = ?2", user, id)
                .firstResultOptional().orElseThrow(() -> new CustomExceptions(ErrorCode.GOAL_NOT_FOUND));
        GoalResponse response = toResponse(goal);

        cacheService.set(cacheKey, response, 3600);
        return response;
    }

    @Transactional
    public void deleteGoal(Long id) {
        User user = getCurrentUser();
        long count = Goal.delete("user=?1 and id=?2", user, id);
        if (count == 0L) {
            throw new CustomExceptions(ErrorCode.GOAL_NOT_FOUND);
        }
        invalidateGoalCaches(user.getUsername(), id);
    }
}