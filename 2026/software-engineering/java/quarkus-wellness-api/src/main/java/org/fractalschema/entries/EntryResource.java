package org.fractalschema.entries;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.fractalschema.dto.request.CreateEntryRequest;
import org.fractalschema.dto.request.UpdateEntryRequest;
import org.fractalschema.dto.response.EntryResponse;

import java.time.LocalDate;
import java.util.List;

@Path("/api/entries")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class EntryResource {

    @Inject
    EntryService entryService;

    @POST
    public Response createEntry(@Valid CreateEntryRequest entryRequest) {
        EntryResponse entryResponse = entryService.createEntry(entryRequest);
        return Response.status(Response.Status.CREATED).entity(entryResponse).build();
    }

    @GET @Path("/{date}")
    public Response getEntry(@PathParam("date") LocalDate date) {
        EntryResponse response = entryService.getEntry(date);
        return Response.ok(response).build();
    }

    @PUT @Path("/{date}")
    public Response updateEntry(@PathParam("date") LocalDate date, @Valid UpdateEntryRequest entryRequest) {
        EntryResponse response = entryService.updateEntry(date, entryRequest);
        return Response.ok(response).build();
    }

    @DELETE @Path("/{date}")
    public Response deleteEntry(@PathParam("date") LocalDate date) {
        entryService.deleteEntry(date);
        return Response.noContent().build();
    }

    @GET @Path("/range")
    public Response getEntry(@QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        List<EntryResponse> responses = entryService.getEntry(from, to);
        return Response.ok(responses).build();
    }

    @GET @Path("/latest")
    public Response getLatestEntries(@QueryParam("limit") @DefaultValue("7") int limit) {
        List<EntryResponse> responses = entryService.getEntry(limit);
        return Response.ok(responses).build();
    }

}
