package org.fractalschema.goals;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.fractalschema.auth.User;
import org.fractalschema.dto.request.CreateGoalRequest;
import org.fractalschema.dto.request.UpdateGoalRequest;
import org.fractalschema.dto.response.EntryResponse;
import org.fractalschema.dto.response.GoalResponse;
import org.fractalschema.entries.DailyEntry;
import org.fractalschema.enums.ErrorCode;
import org.fractalschema.exceptions.CustomExceptions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GoalService {

    @Inject
    SecurityIdentity identity;

    private User getCurrentUser() {
        return User.<User>find("username", identity.getPrincipal().getName())
                .firstResultOptional()
                .orElseThrow(() -> new CustomExceptions(ErrorCode.INVALID_CREDENTIALS));
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

        return toResponse(goal);
    }

    @Transactional
    public GoalResponse updateGoal(Long id, UpdateGoalRequest request) {
        Goal goal = Goal.findById(id);

        if (goal==null || !(goal.getUser().id.equals(getCurrentUser().id))) {
            throw new CustomExceptions(ErrorCode.GOAL_NOT_FOUND);
        }

        Optional.ofNullable(request.getGoalType()).ifPresent(goal::setGoalType);
        Optional.ofNullable(request.getTargetValue()).ifPresent(goal::setTargetValue);
        Optional.ofNullable(request.getFrequency()).ifPresent(goal::setFrequency);
        Optional.ofNullable(request.getStartDate()).ifPresent(goal::setStartDate);
        Optional.ofNullable(request.getEndDate()).ifPresent(goal::setEndDate);
        Optional.ofNullable(request.getActive()).ifPresent(goal::setActive);

        return toResponse(goal);
    }

    @Transactional
    public List<GoalResponse> getAllGoals() {
        return Goal.<Goal>find("user = ?1 order by startDate", getCurrentUser())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public GoalResponse getGoal(Long id) {
        Goal goal = Goal.<Goal>find("user = ?1 and id = ?2", getCurrentUser(), id)
                .firstResultOptional().orElseThrow(() -> new CustomExceptions(ErrorCode.GOAL_NOT_FOUND));
        return toResponse(goal);
    }

    @Transactional
    public void deleteGoal(Long id) {
        User user = getCurrentUser();
        long count = Goal.delete("user=?1 and id=?2", user, id);
        if (count == 0L) {
            throw new CustomExceptions(ErrorCode.GOAL_NOT_FOUND);
        }
    }


}
