package org.fractalschema.app;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.fractalschema.app.model.Film;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class FilmRepository {

    @Inject
    EntityManager entityManager;

    private static final int PAGE_SIZE = 20;

    public Optional<Film> getFilm(Short filmId) {
        return Optional.ofNullable(entityManager.find(Film.class, filmId));
    }

    public Stream<Film> getFilms(Short minLength) {
        return (Stream<Film>) entityManager.createQuery(
                "SELECT f FROM Film f WHERE f.length > :minLength ORDER BY f.length", Film.class)
            .setParameter("minLength", minLength)
                .getResultStream();
    }

    public List<Film> paged(long page, Short minLength) {
        return entityManager.createQuery(
                "SELECT new Film(f.id, f.title, f.length) FROM Film f WHERE f.length > :minLength ORDER BY f.length", Film.class)
            .setParameter("minLength", minLength)
            .setFirstResult((int) (page * PAGE_SIZE))
            .setMaxResults(PAGE_SIZE)
            .getResultList();
    }

    public List<Film> actors(String startsWith, Short minLength) {
        return entityManager.createQuery(
                "SELECT DISTINCT f FROM Film f JOIN FETCH f.actorList " +
                "WHERE f.title LIKE :prefix AND f.length > :minLength ORDER BY f.length DESC", Film.class)
            .setParameter("prefix", startsWith + "%").setParameter("minLength", minLength)
            .getResultList();
    }

    
    @Transactional
    public int updateRentalRate(Short minLength, BigDecimal rentalRate) {
        return entityManager.createQuery(
                "UPDATE Film f SET f.rentalRate = :rentalRate WHERE f.length > :minLength")
            .setParameter("rentalRate", rentalRate)
            .setParameter("minLength", minLength)
            .executeUpdate();
    }
}
