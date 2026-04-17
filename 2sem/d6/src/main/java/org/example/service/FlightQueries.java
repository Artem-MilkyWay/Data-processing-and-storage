package org.example.service;

import org.example.api.dto.*;
import org.example.db.Db;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Service
public class FlightQueries {
    private final Db db;
    private final PointResolver pointResolver;

    public FlightQueries(Db db, PointResolver pointResolver) {
        this.db = db;
        this.pointResolver = pointResolver;
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
                (rs, i) -> new AirportDto(rs.getString("airport_code"),
                        rs.getString("airport_name"),
                        rs.getString("city_name")),
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
                (rs, i) -> new AirportDto(rs.getString("airport_code"),
                        rs.getString("airport_name"),
                        rs.getString("city_name")),
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
                join bookings.routes r on r.route_no = t.route_no and r.validity @> t.scheduled_departure
                where t.arrival_airport = ?
                  and t.scheduled_arrival_local >= (?::date)
                  and t.scheduled_arrival_local < ((?::date) + interval '1 day')
                order by t.scheduled_arrival_local
                """,
                inboundMapper(),
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
                join bookings.routes r on r.route_no = t.route_no and r.validity @> t.scheduled_departure
                where t.departure_airport = ?
                  and t.scheduled_departure_local >= (?::date)
                  and t.scheduled_departure_local < ((?::date) + interval '1 day')
                order by t.scheduled_departure_local
                """,
                outboundMapper(),
                airportCode, departureDate, departureDate
        );
    }

    public List<RouteDto> searchRoutes(String from, String to, LocalDate departureDate, String bookingClassRaw, String maxConnectionsRaw) {
        BookingClass bookingClass = BookingClass.parse(bookingClassRaw);
        int maxLegs = parseMaxLegs(maxConnectionsRaw);

        List<String> fromAirports = pointResolver.resolveAirportCodes(from);
        List<String> toAirports = pointResolver.resolveAirportCodes(to);
        if (fromAirports.isEmpty()) throw new IllegalArgumentException("Unknown from point: " + from);
        if (toAirports.isEmpty()) throw new IllegalArgumentException("Unknown to point: " + to);

        // Preload candidate flights for that date (UTC range by scheduled_departure).
        // We'll do routing in memory to keep implementation readable.
        var flights = db.jdbc().query(
                """
                select
                  t.flight_id,
                  t.departure_airport,
                  t.arrival_airport,
                  t.scheduled_departure,
                  t.scheduled_arrival,
                  t.scheduled_departure_local,
                  t.scheduled_arrival_local
                from bookings.timetable t
                where t.scheduled_departure >= (?::date)
                  and t.scheduled_departure < ((?::date) + interval '1 day')
                order by t.scheduled_departure
                """,
                (rs, i) -> new FlightEdge(
                        rs.getInt("flight_id"),
                        rs.getString("departure_airport"),
                        rs.getString("arrival_airport"),
                        rs.getString("scheduled_departure_local"),
                        rs.getString("scheduled_arrival_local")
                ),
                departureDate, departureDate
        );

        var byFrom = new HashMap<String, List<FlightEdge>>();
        for (var f : flights) byFrom.computeIfAbsent(f.from, k -> new ArrayList<>()).add(f);

        List<RouteDto> out = new ArrayList<>();
        int maxDepth = maxLegs;
        for (String start : fromAirports) {
            dfsRoutes(byFrom, toAirports, start, maxDepth, new ArrayList<>(), new HashSet<>(), out, bookingClass);
        }

        // De-dup by flight sequence.
        var seen = new HashSet<String>();
        var uniq = new ArrayList<RouteDto>();
        for (var r : out) {
            String key = r.route().stream().map(RouteLegDto::flightNo).reduce((a, b) -> a + "-" + b).orElse("");
            if (seen.add(key)) uniq.add(r);
        }
        return uniq;
    }

    private void dfsRoutes(
            Map<String, List<FlightEdge>> byFrom,
            List<String> targetAirports,
            String current,
            int remainingLegs,
            List<FlightEdge> path,
            Set<Integer> usedFlights,
            List<RouteDto> out,
            BookingClass bookingClass
    ) {
        if (remainingLegs < 0) return;
        if (!path.isEmpty() && targetAirports.contains(current)) {
            out.add(toDto(path, bookingClass));
            return;
        }
        if (remainingLegs == 0) return;

        for (var e : byFrom.getOrDefault(current, List.of())) {
            if (!usedFlights.add(e.flightId)) continue;
            path.add(e);
            dfsRoutes(byFrom, targetAirports, e.to, remainingLegs - 1, path, usedFlights, out, bookingClass);
            path.remove(path.size() - 1);
            usedFlights.remove(e.flightId);
        }
    }

    private RouteDto toDto(List<FlightEdge> path, BookingClass bookingClass) {
        var legs = path.stream()
                .map(e -> new RouteLegDto(
                        Integer.toString(e.flightId),
                        e.from,
                        e.to,
                        e.departureLocal,
                        e.arrivalLocal
                ))
                .toList();
        int connections = Math.max(0, legs.size() - 1);
        return new RouteDto(legs, connections, bookingClass.name());
    }

    private int parseMaxLegs(String maxConnectionsRaw) {
        if (maxConnectionsRaw == null || maxConnectionsRaw.isBlank()) return 4; // default: allow up to 3 connections => 4 legs
        return switch (maxConnectionsRaw) {
            case "0" -> 1;
            case "1" -> 2;
            case "2" -> 3;
            case "3" -> 4;
            case "unbound" -> 6; // safety cap
            default -> throw new IllegalArgumentException("maxConnections must be one of 0,1,2,3,unbound");
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

    private String dowName(Integer pgDow) {
        // Our data stores "days_of_week integer[]". Assume 0=Sun..6=Sat (Postgres extract(dow)).
        if (pgDow == null) return "UNKNOWN";
        return switch (pgDow) {
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

    private record FlightEdge(int flightId, String from, String to, String departureLocal, String arrivalLocal) {}
}

