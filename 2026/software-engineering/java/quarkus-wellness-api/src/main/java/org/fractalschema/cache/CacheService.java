package org.fractalschema.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.fractalschema.enums.ErrorCode;
import org.fractalschema.exceptions.CustomExceptions;

import java.util.Optional;

@ApplicationScoped
public class CacheService {
    @Inject
    ObjectMapper mapper;
    @Inject
    RedisDataSource dataSource;

    public void set(String key, Object value, int ttlSeconds) {
        try {
            String jsonString = mapper.writeValueAsString(value);
            dataSource.value(String.class).setex(key, ttlSeconds, jsonString);
        } catch (JsonProcessingException e) {
            throw new CustomExceptions(ErrorCode.JSON_PROCESSING_ISSUE, e.getMessage());
        }
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String jsonString = dataSource.value(String.class).get(key);
            if (jsonString == null) return Optional.empty();
            else {
                return Optional.of(mapper.readValue(jsonString, type));
            }
        } catch (JsonProcessingException e) {
            throw new CustomExceptions(ErrorCode.JSON_PROCESSING_ISSUE, e.getMessage());
        }
    }

    public <T> Optional<T> get(String key, TypeReference<T> type) {
        try {
            String jsonString = dataSource.value(String.class).get(key);
            if (jsonString == null) return Optional.empty();
            else {
                return Optional.of(mapper.readValue(jsonString, type));
            }
        } catch (JsonProcessingException e) {
            throw new CustomExceptions(ErrorCode.JSON_PROCESSING_ISSUE, e.getMessage());
        }
    }

    public void invalidate(String key) {
        dataSource.value(String.class).getdel(key);
    }
}
