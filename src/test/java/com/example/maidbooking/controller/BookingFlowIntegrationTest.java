package com.example.maidbooking.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingFlowIntegrationTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(LocalDateTime.of(2026, 11, 30, 10, 0).atZone(ZONE).toInstant(), ZONE);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void bookPayCancelAndRebookFlow() throws Exception {
        String maidId = addMaid("HSR Layout");
        String search = "/maids/search?locality=HSR Layout&startTime=2026-12-01T09:00&durationMinutes=60";

        mockMvc.perform(get(search)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));

        String booking = postJson("/bookings", """
                {"customerId":"c1","maidId":"%s","serviceType":"CLEANING","bookingType":"RECURRING",
                 "durationMinutes":120,"daysOfWeek":["TUESDAY"],"timeOfDay":"09:00",
                 "startDate":"2026-12-01","endDate":"2026-12-08"}
                """.formatted(maidId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.occurrences", hasSize(2)))
                .andExpect(jsonPath("$.occurrences[0].amount").value(500.0))
                .andReturn().getResponse().getContentAsString();
        String bookingId = JsonPath.read(booking, "$.id");
        String occurrenceId = JsonPath.read(booking, "$.occurrences[0].id");

        mockMvc.perform(get(search)).andExpect(jsonPath("$", hasSize(0)));

        postJson("/bookings/" + bookingId + "/occurrences/" + occurrenceId + "/payments",
                "{\"method\":\"UPI\",\"instrument\":\"customer@okaxis\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/bookings/" + bookingId + "/occurrences/" + occurrenceId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRefund").value(500.0));

        mockMvc.perform(get(search)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void errorsAreMappedToHttpStatuses() throws Exception {
        String maidId = addMaid("Whitefield");
        String scheduled = """
                {"customerId":"c1","maidId":"%s","serviceType":"CLEANING","bookingType":"SCHEDULED",
                 "durationMinutes":60,"startTime":"2026-12-01T10:00"}
                """;

        postJson("/maids", "{}").andExpect(status().isBadRequest());
        postJson("/bookings", scheduled.formatted("unknown-maid")).andExpect(status().isNotFound());
        postJson("/bookings", """
                {"customerId":"c1","maidId":"%s","serviceType":"CLEANING","bookingType":"RECURRING",
                 "durationMinutes":60,"daysOfWeek":[null],"timeOfDay":"09:00",
                 "startDate":"2026-12-01","endDate":"2026-12-08"}
                """.formatted(maidId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("daysOfWeek")));

        String booking = postJson("/bookings", scheduled.formatted(maidId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        postJson("/bookings", scheduled.formatted(maidId)).andExpect(status().isConflict());

        String paymentUrl = "/bookings/" + JsonPath.read(booking, "$.id")
                + "/occurrences/" + JsonPath.read(booking, "$.occurrences[0].id") + "/payments";
        postJson(paymentUrl, "{\"method\":\"UPI\",\"instrument\":\"bad\"}").andExpect(status().isBadRequest());
    }

    @Test
    void invalidMaidInputReturnsBadRequestInStandardFormat() throws Exception {
        String maid = """
                {"name":"Sunita","gender":"%s","locality":"HSR Layout","services":%s,
                 "pricePerHour":250,"rating":4.5,"availability":%s}
                """;
        String window = "[{\"dayOfWeek\":\"TUESDAY\",\"startTime\":\"08:00\",\"endTime\":\"18:00\"}]";

        postJson("/maids", maid.formatted("FEMALE", "[null]", window))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        postJson("/maids", maid.formatted("FEMALE", "[\"CLEANING\"]", "[null]"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        postJson("/maids", maid.formatted("ROBOT", "[\"CLEANING\"]", window))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        postJson("/maids", "{\"name\":")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get("/maids/search?minRating=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("minRating has an invalid value"));
    }

    private String addMaid(String locality) throws Exception {
        String response = postJson("/maids", """
                {"name":"Sunita","gender":"FEMALE","locality":"%s","services":["CLEANING"],
                 "pricePerHour":250,"rating":4.5,
                 "availability":[{"dayOfWeek":"TUESDAY","startTime":"08:00","endTime":"18:00"}]}
                """.formatted(locality))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
