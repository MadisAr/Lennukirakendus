


// package com.example.lennuRakendus.flights;

// import java.io.IOException;
// import java.io.InputStream;
// import java.util.List;

// import org.springframework.stereotype.Component;

// import com.fasterxml.jackson.core.type.TypeReference;
// import com.fasterxml.jackson.databind.ObjectMapper;

// import jakarta.annotation.PostConstruct;


// public class JsonDataLoader {

//     private final FlightRepository flightRepository;
//     private final ObjectMapper objectMapper;

//     public JsonDataLoader(FlightRepository flightRepository, ObjectMapper objectMapper) {
//         this.flightRepository = flightRepository;
//         this.objectMapper = objectMapper;
//     }

//     // loen JSON faili ja salvestab lennud andmebaasi
//     public void readToDB() throws IOException {
//         try (InputStream inputStream = getClass().getResourceAsStream("/data/flights.json")) {
//             List<Flight> flights = objectMapper.readValue(inputStream, new TypeReference<List<Flight>>() {
//             });
//             flightRepository.saveAll(flights);
//         }
//     }

//     @PostConstruct
//     public void init() throws IOException {
//         // iga kord kui leht avatakse, laetakse näidisandmed failist h2 local
//         // andmebaasi
//         readToDB();
//     }
// }
