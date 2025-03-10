package com.example.lennuRakendus.flights;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flights")
public class FlightDataController {

    private final FlightRepository flightRepository;

    public FlightDataController(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @GetMapping("/destinations")
    List<String> getDestinations() {
        return flightRepository.getDestinations();
    }

    @GetMapping("/all")
    List<Flight> findAll() {
        return flightRepository.getAllFlights();
    }

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

    @GetMapping("/takenSeats")
    List<String> getTakenSeats(@RequestParam Integer id) {
        return flightRepository.getTakenSeats(id);
    }

    @GetMapping("/recommendedSeats")
    List<String> getRecommendedSeats(@RequestParam Integer id,@RequestParam Integer nr) {
        return flightRepository.recommendedSeats(id, nr);
    }
}
