create index if not exists idx_airports_code on bookings.airports_data (airport_code);
create index if not exists idx_airports_city_en on bookings.airports_data ((city->>'en'));
create index if not exists idx_airports_city_ru on bookings.airports_data ((city->>'ru'));

create index if not exists idx_routes_dep_arr on bookings.routes (departure_airport, arrival_airport);
create index if not exists idx_routes_route_no on bookings.routes (route_no);
create index if not exists idx_routes_validity_gist on bookings.routes using gist (validity);

create index if not exists idx_flights_scheduled_departure on bookings.flights (scheduled_departure);
create index if not exists idx_flights_route_no on bookings.flights (route_no);
create index if not exists idx_flights_route_departure on bookings.flights (route_no, scheduled_departure);

create index if not exists idx_tickets_book_ref on bookings.tickets (book_ref);
create index if not exists idx_segments_ticket_no on bookings.segments (ticket_no);
create index if not exists idx_segments_flight_fare on bookings.segments (flight_id, fare_conditions);

create index if not exists idx_boarding_flight_seat on bookings.boarding_passes (flight_id, seat_no);