package org.fractalschema.analytics;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.fractalschema.dto.response.StreakResponse;
import org.fractalschema.dto.response.SummaryResponse;
import org.fractalschema.dto.response.TrendResponse;
import org.fractalschema.enums.GoalType;
import org.fractalschema.enums.Period;

import java.util.List;

@Path("/api/analytics")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AnalyticsResource {

    @Inject
    AnalyticsService analyticsService;

    @Inject
    SecurityIdentity identity;

    @GET @Path("/streaks")
    public Response getCurrentStreaks() {
        String username = identity.getPrincipal().getName();
        List<StreakResponse> responses = analyticsService.getCurrentStreaks(username);
        return Response.ok(responses).build();
    }

    @GET @Path("/trends")
    public Response getTrendAnalysis(@QueryParam("metric") GoalType metric, @QueryParam("period") long period) {
        String username = identity.getPrincipal().getName();
        TrendResponse response = analyticsService.getTrendAnalysis(username, metric, period);
        return Response.ok(response).build();
    }

    @GET @Path("/summary")
    public Response getPeriodSummary(@QueryParam("period") Period period) {
        String username = identity.getPrincipal().getName();
        SummaryResponse response = analyticsService.getPeriodSummary(username, period);
        return Response.ok(response).build();
    }

}
