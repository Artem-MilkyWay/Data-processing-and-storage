package org.example.service;

import org.example.api.dto.*;
import org.example.db.Db;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Service
public class FlightQueries {

    private final Db db;
    private final PointResolver pointResolver;
    private final RouteJsonParser parser;

    public FlightQueries(Db db, PointResolver pointResolver, RouteJsonParser parser) {
        this.db = db;
        this.pointResolver = pointResolver;
        this.parser = parser;
    }

    public List<CityDto> listCities() {
        return db.jdbc().query(
                """
                select distinct (city->>'en') as city_name
                from bookings.airports_data
                order by city_name
                """,
                (rs, i) -> new CityDto(rs.getString("city_name"), rs.getString("city_name"))
        );
    }

    public List<AirportDto> listAirports(String lang) {
        String l = (lang == null || lang.isBlank()) ? "en" : lang;

        return db.jdbc().query(
                """
                select
                  airport_code,
                  coalesce(airport_name->>?, airport_name->>'en') as airport_name,
                  coalesce(city->>?, city->>'en') as city_name
                from bookings.airports_data
                order by airport_code
                """,
                (rs, i) -> new AirportDto(
                        rs.getString("airport_code"),
                        rs.getString("airport_name"),
                        rs.getString("city_name")
                ),
                l, l
        );
    }

    public List<AirportDto> airportsInCity(String cityCode, String lang) {
        String l = (lang == null || lang.isBlank()) ? "en" : lang;

        var rows = db.jdbc().query(
                """
                select
                  airport_code,
                  coalesce(airport_name->>?, airport_name->>'en') as airport_name,
                  coalesce(city->>?, city->>'en') as city_name
                from bookings.airports_data
                where city->>'en' = ? or city->>'ru' = ?
                order by airport_code
                """,
                (rs, i) -> new AirportDto(
                        rs.getString("airport_code"),
                        rs.getString("airport_name"),
                        rs.getString("city_name")
                ),
                l, l, cityCode, cityCode
        );

        if (rows.isEmpty()) throw new IllegalArgumentException("City not found: " + cityCode);
        return rows;
    }
    public List<ScheduleInboundDto> inboundSchedule(String airportCode, LocalDate departureDate) {
        return db.jdbc().query(
                """
                select
                  r.days_of_week,
                  to_char(t.scheduled_arrival_local, 'HH24:MI') as arrival_time,
                  t.flight_id::text as flight_no,
                  t.departure_airport as origin
                from bookings.timetable t
                join bookings.routes r
                  on r.route_no = t.route_no
                 and r.validity @> t.scheduled_departure
                where t.arrival_airport = ?
                  and t.scheduled_arrival_local >= (?::date)
                  and t.scheduled_arrival_local < (?::date + interval '1 day')
                order by t.scheduled_arrival_local
                """,
                (rs, i) -> new ScheduleInboundDto(
                        intArrayToDow(rs.getArray("days_of_week")),
                        rs.getString("arrival_time"),
                        rs.getString("flight_no"),
                        rs.getString("origin")
                ),
                airportCode, departureDate, departureDate
        );
    }

    public List<ScheduleOutboundDto> outboundSchedule(String airportCode, LocalDate departureDate) {
        return db.jdbc().query(
                """
                select
                  r.days_of_week,
                  to_char(t.scheduled_departure_local, 'HH24:MI') as departure_time,
                  t.flight_id::text as flight_no,
                  t.arrival_airport as destination
                from bookings.timetable t
                join bookings.routes r
                  on r.route_no = t.route_no
                 and r.validity @> t.scheduled_departure
                where t.departure_airport = ?
                  and t.scheduled_departure_local >= (?::date)
                  and t.scheduled_departure_local < (?::date + interval '1 day')
                order by t.scheduled_departure_local
                """,
                (rs, i) -> new ScheduleOutboundDto(
                        intArrayToDow(rs.getArray("days_of_week")),
                        rs.getString("departure_time"),
                        rs.getString("flight_no"),
                        rs.getString("destination")
                ),
                airportCode, departureDate, departureDate
        );
    }

    public List<RouteDto> searchRoutes(
            String from,
            String to,
            LocalDate departureDate,
            String bookingClassRaw,
            String maxConnectionsRaw
    ) {
        BookingClass bookingClass = BookingClass.parse(bookingClassRaw);
        int maxLegs = parseMaxLegs(maxConnectionsRaw);

        List<String> fromAirports = pointResolver.resolveAirportCodes(from);
        List<String> toAirports = pointResolver.resolveAirportCodes(to);

        if (fromAirports.isEmpty()) throw new IllegalArgumentException("Unknown from point: " + from);
        if (toAirports.isEmpty()) throw new IllegalArgumentException("Unknown to point: " + to);

        String sql = """
        with recursive routes as (

            select
                t.flight_id,
                t.departure_airport,
                t.arrival_airport,
                array[t.flight_id] as path_ids,

                jsonb_build_array(
                    jsonb_build_object(
                        'flightNo', t.flight_id::text,
                        'from', t.departure_airport,
                        'to', t.arrival_airport,
                        'departureTime', to_char(t.scheduled_departure_local, 'YYYY-MM-DD\"T\"HH24:MI:SS'),
                        'arrivalTime', to_char(t.scheduled_arrival_local, 'YYYY-MM-DD\"T\"HH24:MI:SS')
                    )
                ) as route,

                1 as depth

            from bookings.timetable t
            where t.departure_airport = any(?)
              and t.scheduled_departure >= ?::date
              and t.scheduled_departure < (?::date + interval '1 day')

            union all

            select
                t.flight_id,
                r.departure_airport,
                t.arrival_airport,
                r.path_ids || t.flight_id,

                r.route || jsonb_build_object(
                    'flightNo', t.flight_id::text,
                    'from', t.departure_airport,
                    'to', t.arrival_airport,
                    'departureTime', to_char(t.scheduled_departure_local, 'YYYY-MM-DD\"T\"HH24:MI:SS'),
                    'arrivalTime', to_char(t.scheduled_arrival_local, 'YYYY-MM-DD\"T\"HH24:MI:SS')
                ),

                r.depth + 1

            from routes r
            join bookings.timetable t
              on t.departure_airport = r.arrival_airport
             and t.scheduled_departure >= ?::date
             and t.scheduled_departure < (?::date + interval '1 day')
            where r.depth < ?
              and not (t.flight_id = any(r.path_ids))
        )

        select route, (depth - 1) as connections
        from routes
        where arrival_airport = any(?)
        """;

        return db.jdbc().query(
                connection -> {
                    var ps = connection.prepareStatement(sql);

                    ps.setArray(1, connection.createArrayOf("text", fromAirports.toArray()));
                    ps.setObject(2, departureDate);
                    ps.setObject(3, departureDate);
                    ps.setObject(4, departureDate);
                    ps.setObject(5, departureDate);
                    ps.setInt(6, maxLegs);
                    ps.setArray(7, connection.createArrayOf("text", toAirports.toArray()));

                    return ps;
                },
                (rs, i) -> {
                    String json = rs.getString("route");

                    List<RouteLegDto> legs = parser.parse(json);

                    return new RouteDto(
                            legs,
                            rs.getInt("connections"),
                            bookingClass.name()
                    );
                }
        );
    }

    private int parseMaxLegs(String raw) {
        if (raw == null || raw.isBlank()) return 4;
        return switch (raw) {
            case "0" -> 1;
            case "1" -> 2;
            case "2" -> 3;
            case "3" -> 4;
            case "unbound" -> 6;
            default -> throw new IllegalArgumentException("maxConnections must be 0,1,2,3,unbound");
        };
    }

    private RowMapper<ScheduleInboundDto> inboundMapper() {
        return (rs, i) -> new ScheduleInboundDto(
                intArrayToDow(rs.getArray("days_of_week")),
                rs.getString("arrival_time"),
                rs.getString("flight_no"),
                rs.getString("origin")
        );
    }

    private RowMapper<ScheduleOutboundDto> outboundMapper() {
        return (rs, i) -> new ScheduleOutboundDto(
                intArrayToDow(rs.getArray("days_of_week")),
                rs.getString("departure_time"),
                rs.getString("flight_no"),
                rs.getString("destination")
        );
    }

    private List<String> intArrayToDow(Array arr) throws SQLException {
        if (arr == null) return List.of();

        Integer[] ints = (Integer[]) arr.getArray();
        List<String> out = new ArrayList<>(ints.length);

        for (Integer d : ints) out.add(dowName(d));

        return out;
    }

    private String dowName(Integer d) {
        if (d == null) return "UNKNOWN";
        return switch (d) {
            case 0 -> "Sunday";
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            default -> "UNKNOWN";
        };
    }

    private record FlightEdge(
            int flightId,
            String from,
            String to,
            String departureLocal,
            String arrivalLocal
    ) {}
}