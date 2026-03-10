package org.fractalschema.goals;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.fractalschema.dto.request.CreateGoalRequest;
import org.fractalschema.dto.request.UpdateGoalRequest;
import org.fractalschema.dto.response.GoalResponse;

import java.util.List;

@Path("/api/goals")
@RolesAllowed("user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GoalResource {

    @Inject GoalService goalService;

    @POST
    public Response createGoal(@Valid CreateGoalRequest goalRequest) {
        GoalResponse goalResponse = goalService.createGoal(goalRequest);
        return Response.status(Response.Status.CREATED).entity(goalResponse).build();
    }

    @GET
    public Response getAllGoals() {
        List<GoalResponse> goals = goalService.getAllGoals();
        return Response.ok(goals).build();
    }

    @GET @Path("/{id}")
    public Response getGoal(@PathParam("id")Long id) {
        GoalResponse goalResponse = goalService.getGoal(id);
        return Response.ok(goalResponse).build();
    }

    @PATCH @Path("/{id}")
    public Response updateGoal(@PathParam("id") Long id, UpdateGoalRequest request) {
        GoalResponse goalResponse = goalService.updateGoal(id, request);
        return Response.ok(goalResponse).build();
    }

    @DELETE @Path("/{id}")
    public Response deleteGoal(@PathParam("id") Long id) {
        goalService.deleteGoal(id);
        return Response.noContent().build();
    }

    @GET @Path("/progress")
    public Response getGoalProgress(@QueryParam("period") String period) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }
}
