package org.example.service;

import org.example.api.dto.BookingCreateRequest;
import org.example.api.dto.BookingCreateResponse;
import org.example.api.dto.CheckinRequest;
import org.example.api.dto.CheckinResponse;
import org.example.db.Db;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class BookingService {
    private final JdbcTemplate jdbc;

    public BookingService(Db db) {
        this.jdbc = db.jdbc();
    }

    @Transactional
    public BookingCreateResponse createBooking(BookingCreateRequest req) {
        BookingClass bookingClass = BookingClass.parse(req.bookingClass);

        String bookRef = tryCreateBookingRow();
        String ticketNo = tryCreateTicketRow(bookRef, req);

        BigDecimal total = BigDecimal.ZERO;
        for (String flightNo : req.route) {
            int flightId = parseFlightId(flightNo);
            BigDecimal price = priceForFlight(flightId, bookingClass);
            total = total.add(price);
            jdbc.update(
                    """
                    insert into bookings.segments(ticket_no, flight_id, fare_conditions, price)
                    values(?,?,?,?)
                    """,
                    ticketNo, flightId, bookingClass.name(), price
            );
        }

        jdbc.update("update bookings.bookings set total_amount = ? where book_ref = ?", total, bookRef);
        return new BookingCreateResponse(bookRef, "CONFIRMED");
    }

    @Transactional
    public CheckinResponse checkin(CheckinRequest req) {
        int flightId = parseFlightId(req.flightNo);

        String ticketNo = jdbc.query(
                """
                select t.ticket_no
                from bookings.tickets t
                join bookings.segments s on s.ticket_no = t.ticket_no
                where t.book_ref = ?
                  and s.flight_id = ?
                limit 1
                """,
                (rs) -> rs.next() ? rs.getString("ticket_no") : null,
                req.bookingId, flightId
        );
        if (ticketNo == null) throw new IllegalArgumentException("Booking/flight not found");

        // Find fare class for that segment (seat pool).
        String fare = jdbc.query(
                "select fare_conditions from bookings.segments where ticket_no = ? and flight_id = ? limit 1",
                (rs) -> rs.next() ? rs.getString(1) : null,
                ticketNo, flightId
        );
        if (fare == null) throw new IllegalArgumentException("Segment not found");

        String seatNo = pickFreeSeat(flightId, fare);
        Integer boardingNo = nextBoardingNo(flightId);

        jdbc.update(
                """
                insert into bookings.boarding_passes(ticket_no, flight_id, seat_no, boarding_no, boarding_time)
                values(?,?,?,?,?)
                on conflict (ticket_no, flight_id) do update
                  set seat_no = excluded.seat_no,
                      boarding_no = excluded.boarding_no,
                      boarding_time = excluded.boarding_time
                """,
                ticketNo, flightId, seatNo, boardingNo, OffsetDateTime.now()
        );

        String checkinId = Ids.checkinId();
        return new CheckinResponse(
                checkinId,
                "CHECKED_IN",
                seatNo,
                "https://example.com/boarding-pass/" + checkinId
        );
    }

    private String tryCreateBookingRow() {
        for (int i = 0; i < 10; i++) {
            String bookRef = Ids.bookingRef();
            try {
                jdbc.update(
                        "insert into bookings.bookings(book_ref, book_date, total_amount) values(?,?,0)",
                        bookRef, OffsetDateTime.now()
                );
                return bookRef;
            } catch (DuplicateKeyException ignored) {
            }
        }
        throw new IllegalStateException("Could not generate booking id");
    }

    private String tryCreateTicketRow(String bookRef, BookingCreateRequest req) {
        for (int i = 0; i < 10; i++) {
            String ticketNo = Ids.ticketNo();
            try {
                jdbc.update(
                        """
                        insert into bookings.tickets(ticket_no, book_ref, passenger_id, passenger_name, outbound)
                        values(?,?,?,?,true)
                        """,
                        ticketNo,
                        bookRef,
                        req.passenger.email,
                        req.passenger.firstName + " " + req.passenger.lastName,
                        true
                );
                return ticketNo;
            } catch (DuplicateKeyException ignored) {
            }
        }
        throw new IllegalStateException("Could not generate ticket id");
    }

    private BigDecimal priceForFlight(int flightId, BookingClass bookingClass) {
        BigDecimal avg = jdbc.query(
                """
                select avg(price) as p
                from bookings.segments
                where flight_id = ? and fare_conditions = ?
                """,
                (rs) -> rs.next() ? rs.getBigDecimal("p") : null,
                flightId, bookingClass.name()
        );
        if (avg != null) return avg;
        return switch (bookingClass) {
            case Economy -> new BigDecimal("100.00");
            case Comfort -> new BigDecimal("180.00");
            case Business -> new BigDecimal("350.00");
        };
    }

    private int parseFlightId(String flightNo) {
        try {
            return Integer.parseInt(flightNo);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("flightNo must be numeric (flight_id in demo DB)");
        }
    }

    private String pickFreeSeat(int flightId, String fare) {
        // Get airplane_code via route for this flight (from timetable view).
        String airplane = jdbc.query(
                "select airplane_code from bookings.timetable where flight_id = ?",
                (rs) -> rs.next() ? rs.getString(1) : null,
                flightId
        );
        if (airplane == null) throw new IllegalArgumentException("Flight not found: " + flightId);

        List<String> seats = jdbc.query(
                """
                select s.seat_no
                from bookings.seats s
                where s.airplane_code = ?
                  and s.fare_conditions = ?
                  and not exists (
                    select 1 from bookings.boarding_passes bp
                    where bp.flight_id = ? and bp.seat_no = s.seat_no
                  )
                order by s.seat_no
                limit 1
                """,
                (rs, i) -> rs.getString(1),
                airplane, fare, flightId
        );
        if (seats.isEmpty()) throw new IllegalArgumentException("No free seats left");
        return seats.get(0);
    }

    private Integer nextBoardingNo(int flightId) {
        Integer n = jdbc.queryForObject(
                "select coalesce(max(boarding_no), 0) + 1 from bookings.boarding_passes where flight_id = ?",
                Integer.class,
                flightId
        );
        return n == null ? 1 : n;
    }
}

