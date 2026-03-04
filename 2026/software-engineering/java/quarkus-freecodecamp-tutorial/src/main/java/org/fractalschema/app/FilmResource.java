package org.fractalschema.app;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.fractalschema.app.model.Film;

import java.awt.*;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/")
public class FilmResource {

    @Inject
    FilmRepository filmRepository;


    @GET
    @Path("/helloworld")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
         return "Hello World!";
    }

    @GET
    @Path("/helloworld2")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello2() {
         return "Hello World 2!";
    }

    @GET
    @Path("/film/{filmId}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getFilm(@PathParam("filmId") Short filmId) {
        Optional<Film> film = filmRepository.getFilm(filmId);
        return film.isPresent()? film.get().getTitle(): "No film was found!";
    }

    @GET
    @Path("/pagedFilms/{page}/{minLength}")
    @Produces(MediaType.TEXT_PLAIN)
    public String paged(@PathParam("page") long page, @PathParam("minLength") Short minLength) {
        return filmRepository.paged(page, minLength).stream()
            .map(f -> String.format("%s (%d min)", f.getTitle(), f.getLength()))
            .collect(Collectors.joining("\n"));
    }

    @GET
    @Path("/actors/{startsWith}/{minLength}")
    @Produces(MediaType.TEXT_PLAIN)
    public String actors(@PathParam("startsWith") String startsWith, @PathParam("minLength") Short minLength) {
        return filmRepository.actors(startsWith, minLength).stream()
                .map(f -> String.format("%s (%d min): %s", f.getTitle(), f.getLength(),
                        f.getActorList().stream()
                                .map(a -> String.format("%s %s", a.getFirstName(), a.getLastName())).collect(Collectors.joining(", "))))
                .collect(Collectors.joining("\n"));
    }

    @GET
    @Path("/update/{minLength}/{rentalRate}")
    @Produces(MediaType.TEXT_PLAIN)
    public String update(@PathParam("minLength") Short minLength, @PathParam("rentalRate") BigDecimal rentalRate) {
          filmRepository.updateRentalRate(minLength, rentalRate);
        return filmRepository.getFilms(minLength)
            .map(f -> String.format("%s (%d min) - $%f", f.getTitle(), f.getLength(), f.getRentalRate()))
            .collect(Collectors.joining("\n"));
    }
}
