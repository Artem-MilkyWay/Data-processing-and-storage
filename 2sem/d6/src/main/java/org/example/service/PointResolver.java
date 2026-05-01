package org.example.service;

import org.example.db.Db;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PointResolver {
    private final Db db;

    public PointResolver(Db db) {
        this.db = db;
    }

    public List<String> resolveAirportCodes(String point) {
        Integer count = db.jdbc().queryForObject(
                "select count(*) from bookings.airports_data where airport_code = ?",
                Integer.class,
                point
        );
        if (count != null && count > 0) {
            return List.of(point);
        }

        return db.jdbc().query(
                """
                select airport_code
                from bookings.airports_data
                where city->>'en' = ?
                   or city->>'ru' = ?
                order by airport_code
                """,
                (rs, i) -> rs.getString("airport_code"),
                point,
                point
        );
    }
}

