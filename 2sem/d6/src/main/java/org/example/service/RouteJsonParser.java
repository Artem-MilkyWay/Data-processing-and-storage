package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.api.dto.RouteLegDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RouteJsonParser {

    private final ObjectMapper mapper;

    public RouteJsonParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<RouteLegDto> parse(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<RouteLegDto>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse route json", e);
        }
    }
}