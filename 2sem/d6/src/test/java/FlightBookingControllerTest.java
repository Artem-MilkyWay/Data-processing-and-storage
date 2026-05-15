
import org.example.Main;
import org.example.api.dto.BookingCreateRequest;
import org.example.api.dto.CheckinRequest;
import org.example.service.BookingService;
import org.example.service.FlightQueries;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = Main.class)
@AutoConfigureMockMvc
class FlightBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FlightQueries queries;

    @MockBean
    private BookingService bookingService;

    @Test
    void cities_shouldReturn200() throws Exception {
        when(queries.listCities()).thenReturn(List.of());

        mockMvc.perform(get("/cities"))
                .andExpect(status().isOk());
    }

    @Test
    void airports_shouldReturn200() throws Exception {
        when(queries.listAirports("en")).thenReturn(List.of());

        mockMvc.perform(get("/airports?lang=en"))
                .andExpect(status().isOk());
    }

    @Test
    void airportsInCity_shouldReturn200() throws Exception {
        when(queries.airportsInCity(eq("MOW"), eq("en"))).thenReturn(List.of());

        mockMvc.perform(get("/cities/MOW/airports?lang=en"))
                .andExpect(status().isOk());
    }

    @Test
    void inbound_shouldReturn200() throws Exception {
        when(queries.inboundSchedule(eq("SVO"), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/airports/SVO/schedule/inbound")
                        .param("departureDate", "2025-12-05"))
                .andExpect(status().isOk());
    }

    @Test
    void outbound_shouldReturn200() throws Exception {
        when(queries.outboundSchedule(eq("SVO"), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/airports/SVO/schedule/outbound")
                        .param("departureDate", "2025-12-05"))
                .andExpect(status().isOk());
    }

    @Test
    void routes_shouldReturn200() throws Exception {
        when(queries.searchRoutes(
                any(), any(), any(), any(), any()
        )).thenReturn(List.of());

        mockMvc.perform(get("/routes")
                        .param("from", "SVO")
                        .param("to", "LED")
                        .param("departureDate", "2025-12-05")
                        .param("bookingClass", "Economy"))
                .andExpect(status().isOk());
    }

    @Test
    void createBooking_shouldReturn400_forInvalidBody() throws Exception {

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "dummy": "value"
                    }
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkin_shouldReturn400_forInvalidBody() throws Exception {

        mockMvc.perform(post("/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "dummy": "value"
                    }
                    """))
                .andExpect(status().isBadRequest());
    }
}