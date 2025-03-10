package com.example.lennuRakendus.flights;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class FlightRepository {

    private final JdbcClient jdbcClient;
    private static final Logger logger = LoggerFactory.getLogger(FlightRepository.class);

    public FlightRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public static Boolean[] randomizeSeats() {
        // arvestan hetkel, et lennukis on 20 rida, igas reas 6 kohta
        // listis on true koik kohad mis on voetud
        Boolean[] takenSeats = new Boolean[120];

        // tõenaosus, mida kasutatakse, et arvutada, kas koht on võetud või mitte
        // maaran miinimumiks 0.3, et lennud oleks rohkem tais
        int flightPopularity = (int) (0.3 + Math.random() * 0.7 * 10);

        for (int i = 0; i < takenSeats.length; i++) {
            int seatChance = (int) (Math.random() * 10);

            takenSeats[i] = seatChance > flightPopularity;
        }

        return takenSeats;
    }

    public void saveSeatsToDB(Flight flight, int id) {
        logger.info("saving seats");
        Boolean[] takenSeats = randomizeSeats();
        int i = 1;
        int j = 0;
        while (i - 1 < 20 && j < 6) {
            String seatId = i + String.valueOf((char) ('A' + j));

            jdbcClient.sql("INSERT INTO seats (flight_id, seat_id, is_taken) VALUES(:flight_id, :seat_id, :is_taken)")
                    .param("flight_id", id)
                    .param("seat_id", seatId)
                    .param("is_taken", takenSeats[(i - 1) * 6 + j])
                    .update();

            if (j == 5) {
                j = 0;
                i++;
            } else
                j++;
        }
    }

    public void saveToDB(Flight flight, int id) {
        // kasutan h2 andmebaasi, et simuleerida andmebaasiga tööd
        logger.info("creating table");
        jdbcClient.sql(
                "INSERT INTO flights(id, airline, departure_airport, destination_airport, arrival_date, flight_date, price) values(:id, :airline, :departureAirport, :destinationAirport, :flightArrivalDate, :flightDate, :price)")
                .param("id", id)
                .param("airline", flight.getAirline_name())
                .param("departureAirport", flight.getDeparture_airport())
                .param("destinationAirport", flight.getDestination_airport())
                .param("flightArrivalDate", flight.getArrival_date())
                .param("flightDate", flight.getFlight_date())
                .param("price", flight.getPrice())
                .update();
        saveSeatsToDB(flight, id);
    }

    public void saveAll(List<Flight> flights) {
        int id = 1;
        jdbcClient.sql(
                "CREATE TABLE flights (id INT PRIMARY KEY, airline VARCHAR(255), departure_airport VARCHAR(255), destination_airport VARCHAR(255), arrival_date DATETIME, flight_date DATETIME, price integer)")
                .update();

        // teen eraldi tabeli kohtade hoimdiseks
        jdbcClient.sql("CREATE TABLE seats (flight_id INT, seat_id VARCHAR(255), is_taken BOOLEAN)").update();
        for (Flight flight : flights) {
            saveToDB(flight, id++);
        }
    }

    public List<String> getDestinations() {
        return jdbcClient.sql("select destination_airport from flights;")
                .query(String.class)
                .list();
    }

    public List<Flight> getAllFlights() {
        return jdbcClient.sql("select * from flights")
                .query(Flight.class)
                .list();
    }

    // teeb vastavalt parameetritele sql paringu, et saada vastavad lennud
    // andmebaasist
    List<Flight> getFlights(String flightDate, Integer minPrice, Integer maxPrice, String airlineName,
            String departureAirport, String destinationAirport, String time) {

        StringBuilder sql = new StringBuilder("SELECT * FROM flights WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (flightDate != null) {
            sql.append(" AND CAST(flight_date AS date) = ?");
            params.add(flightDate);
        }

        if (minPrice != null) {
            sql.append(" AND price > ?");
            params.add(minPrice);
        }

        if (time != null) {
            String minTime = null;
            String maxTime = null;
            switch (time) {
                case "morning" -> {
                    minTime = "06:00";
                    maxTime = "12:00";
                }
                case "afternoon" -> {
                    minTime = "12:00";
                    maxTime = "18:00";
                }
                case "evening" -> {
                    minTime = "18:00";
                    maxTime = "00:00";
                }
                case "night" -> {
                    minTime = "00:00";
                    maxTime = "06:00";
                }
            }

            sql.append(" AND cast(flight_date as time) BETWEEN ? AND ? ");
            params.add(minTime);
            params.add(maxTime);
        }

        if (maxPrice != null) {
            sql.append(" AND price < ?");
            params.add(maxPrice);
        }

        if (airlineName != null) {
            sql.append(" AND airline = ?");
            params.add(airlineName);
        }

        if (departureAirport != null) {
            sql.append(" AND departure_airport = ?");
            params.add(departureAirport);
        }

        if (destinationAirport != null) {
            sql.append(" AND destination_airport = ?");
            params.add(destinationAirport);
        }

        return jdbcClient.sql(sql.toString())
                .params(params.toArray())
                .query(Flight.class)
                .list();
    }

    List<String> getTakenSeats(Integer id) {
        return jdbcClient.sql("SELECT seat_id from seats where flight_id = ? and is_taken;")
                .params(id)
                .query(String.class)
                .list();
    }

    // teen koha tähisest char numbrid ja vaatan kui palju kahe koha numbrid
    // erinevad
    boolean areSeatsTogether(String seatA, String seatB) {
        int difference = Math.abs(seatA.charAt(0) + seatA.charAt(1) - seatB.charAt(0) + seatB.charAt(1));

        if (seatA.charAt(0) == seatB.charAt(0) && difference < 2)
            return true;

        return false;
    }

    List<String> recommendedSeats(Integer id, Integer nr) {
        List<String> freeSeats = jdbcClient.sql("SELECT seat_id from seats where flight_id = ? and not is_taken;")
                .params(id)
                .query(String.class)
                .list();

        List<List<String>> consecutiveSeats = new ArrayList<>();
        List<String> currentConsecutiveSeats = new ArrayList<>();
        int count = 0;
        for (int i = 0; i + 1 < freeSeats.size(); i++) {
            if (areSeatsTogether(freeSeats.get(i), freeSeats.get(i + 1))) {
                count++;
                currentConsecutiveSeats.add(freeSeats.get(i));
                currentConsecutiveSeats.add(freeSeats.get(i + 1));
            } else if (count > 0) {
                count = 0;
            }
        }

        return null;
    }
}
