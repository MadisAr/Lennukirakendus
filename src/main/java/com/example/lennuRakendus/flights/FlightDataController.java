package com.example.lennuRakendus.flights;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flights")
public class FlightDataController {
    private static final Logger logger = LoggerFactory.getLogger(FlightDataController.class);
    private final FlightRepository flightRepository;

    public FlightDataController(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    // tagastab kõik sihtkohad andmebaasist
    @GetMapping("/destinations")
    List<String> getDestinations() {
        return flightRepository.getDestinations();
    }

    // tagastab kõik lennud andmebaasist
    @GetMapping("/all")
    List<Flight> findAll() {
        return flightRepository.getAllFlights();
    }

    // teeb lennupäringu vastavalt parameetritele ja tagastab vastavad lennuds
    @GetMapping("")
    List<Flight> getFlights(@RequestParam(required = false) String flight_date,
            @RequestParam(required = false) Integer min_price,
            @RequestParam(required = false) Integer max_price,
            @RequestParam(required = false) String airline_name,
            @RequestParam(required = false) String departure_airport,
            @RequestParam(required = false) String destination_airport,
            @RequestParam(required = false) String time) {
        return flightRepository.getFlights(flight_date, min_price, max_price, airline_name, departure_airport,
                destination_airport, time);
    }

    // tagastab kõik broneeritud kohad lennu järgi
    @GetMapping("/takenSeats")
    List<String> getTakenSeats(@RequestParam Integer id) {
        return flightRepository.getTakenSeats(id);
    }

    // tagastab soovitatud vabad kohad
    @GetMapping("/recommendedSeats")
    List<String> getRecommendedSeats(@RequestParam Integer id, @RequestParam Integer nr,
            @RequestParam Boolean windowSeat, @RequestParam Boolean legRoom, @RequestParam Boolean aisle) {
        return flightRepository.recommendedSeats(id, nr, windowSeat, legRoom, aisle);
    }
}
