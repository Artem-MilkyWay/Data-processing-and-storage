package org.example.api;

import jakarta.validation.Valid;
import org.example.api.dto.*;
import org.example.service.BookingService;
import org.example.service.FlightQueries;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class FlightBookingController {
    private final FlightQueries queries;
    private final BookingService bookingService;

    public FlightBookingController(FlightQueries queries, BookingService bookingService) {
        this.queries = queries;
        this.bookingService = bookingService;
    }

    @GetMapping("/cities")
    public List<CityDto> cities() {
        return queries.listCities();
    }

    @GetMapping("/airports")
    public List<AirportDto> airports(@RequestParam(required = false) String lang) {
        return queries.listAirports(lang);
    }

    @GetMapping("/cities/{cityCode}/airports")
    public List<AirportDto> airportsInCity(@PathVariable String cityCode, @RequestParam(required = false) String lang) {
        return queries.airportsInCity(cityCode, lang);
    }

    @GetMapping("/airports/{airportCode}/schedule/inbound")
    public List<ScheduleInboundDto> inbound(
            @PathVariable String airportCode,
            @RequestParam LocalDate departureDate
    ) {
        return queries.inboundSchedule(airportCode, departureDate);
    }

    @GetMapping("/airports/{airportCode}/schedule/outbound")
    public List<ScheduleOutboundDto> outbound(
            @PathVariable String airportCode,
            @RequestParam LocalDate departureDate
    ) {
        return queries.outboundSchedule(airportCode, departureDate);
    }

    @GetMapping("/routes")
    public List<RouteDto> routes(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam LocalDate departureDate,
            @RequestParam String bookingClass,
            @RequestParam(required = false) String maxConnections
    ) {
        return queries.searchRoutes(from, to, departureDate, bookingClass, maxConnections);
    }

    @PostMapping("/bookings")
    public BookingCreateResponse createBooking(@Valid @RequestBody BookingCreateRequest request) {
        return bookingService.createBooking(request);
    }

    @PostMapping("/checkin")
    public CheckinResponse checkin(@Valid @RequestBody CheckinRequest request) {
        return bookingService.checkin(request);
    }
}

