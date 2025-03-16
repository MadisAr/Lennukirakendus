package com.example.lennuRakendus.flights;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;

@Repository
public class FlightRepository {
    private final LiveDataLoader liveDataLoader;
    private final JdbcClient jdbcClient;
    private static final Logger logger = LoggerFactory.getLogger(FlightRepository.class);

    public FlightRepository(JdbcClient jdbcClient, com.example.lennuRakendus.flights.LiveDataLoader liveDataLoader) {
        this.jdbcClient = jdbcClient;
        this.liveDataLoader = liveDataLoader;
    }

    // Arvutan lennuki kohtade võtmisvõimalused, et luua juhuslikult täidetud
    // kohtade loend
    // Lennukis on 120 kohta, iga koha jaoks on määratud tõenäosus
    public static Boolean[] randomizeSeats() {
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

    // Salvestan lennuki kohad andmebaasi, lisades iga koha täidetuse staatuse
    public void saveSeatsToDB(Flight flight, int id) {
        Boolean[] takenSeats = randomizeSeats();

        StringBuilder batchQuery = new StringBuilder(
                "INSERT INTO seats (flight_id, seat_id, is_taken) VALUES ");

        List<Object> params = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            for (int j = 0; j < 6; j++) {
                if (!params.isEmpty()) {
                    batchQuery.append(", ");
                }
                batchQuery.append("(?, ?, ?)");

                String seatId = i + String.valueOf((char) ('A' + j));
                params.add(id);
                params.add(seatId);
                params.add(takenSeats[(i - 1) * 6 + j]);
            }
        }

        jdbcClient.sql(batchQuery.toString())
                .params(params.toArray())
                .update();
    }

    // Loob kõikide lendude jaoks andmebaasi vajalikud tabelid
    // ja salvestab kõik lennud, kui andmebaas on tühi (ehk dockeri esmakordsel
    // jooksutamisel)
    @PostConstruct
    public void saveAll() throws IOException, URISyntaxException {
        jdbcClient.sql("TRUNCATE TABLE flights;").update();
        jdbcClient.sql("TRUNCATE TABLE seats;").update();

        jdbcClient.sql(
                "CREATE TABLE IF NOT EXISTS flights (id INT PRIMARY KEY, airline_name VARCHAR(255), departure_airport VARCHAR(255), destination_airport VARCHAR(255), arrival_date TIMESTAMP, flight_date TIMESTAMP, price integer)")
                .update();

        jdbcClient.sql("CREATE TABLE IF NOT EXISTS seats (flight_id INT, seat_id VARCHAR(255), is_taken BOOLEAN)")
                .update();

        List<String> isFlightEmpty = jdbcClient.sql("SELECT COUNT(*) FROM flights").query(String.class).list();
        if (isFlightEmpty.get(0).equals("0")) {
            logger.info("table empty");
            List<Flight> flights = liveDataLoader.readFlights();

            int id = 1;

            StringBuilder flightBatchQuery = new StringBuilder(
                    "INSERT INTO flights(id, airline_name, departure_airport, destination_airport, arrival_date, flight_date, price) VALUES ");
            List<Object> flightParams = new ArrayList<>();

            for (Flight flight : flights) {
                if (!flightParams.isEmpty()) {
                    flightBatchQuery.append(", ");
                }
                flightBatchQuery.append("(?, ?, ?, ?, cast(? as timestamp), cast(? as timestamp), ?)");

                flightParams.add(id);
                flightParams.add(flight.airline_name());
                flightParams.add(flight.departure_airport());
                flightParams.add(flight.destination_airport());
                flightParams.add(flight.arrival_date());
                flightParams.add(flight.flight_date());
                flightParams.add(flight.price());

                saveSeatsToDB(flight, id);
                id++;
            }

            jdbcClient.sql(flightBatchQuery.toString())
                    .params(flightParams.toArray())
                    .update();
        }
    }

    // Tagastan kõik saadavad lennujaamad sihtkohtadeks
    public List<String> getDestinations() {
        return jdbcClient.sql("select distinct destination_airport from flights order by destination_airport;")
                .query(String.class)
                .list();
    }

    // Tagastan kõik lennud andmebaasist
    public List<Flight> getAllFlights() {
        return jdbcClient.sql("select * from flights")
                .query(Flight.class)
                .list();
    }

    // teeb vastavalt parameetritele sql paringu, et saada vastavad lennud
    // andmebaasist
    List<Flight> getFlights(String flightDate, Integer minPrice, Integer maxPrice, String airlineName,
            String departureAirport, String destinationAirport, String time) {

        // teen stringbuilderi, millele lisatakse jarjest sql paringu vastavaid osi kui
        // vaja
        StringBuilder sql = new StringBuilder("SELECT * FROM flights WHERE flight_date > CURRENT_TIMESTAMP");
        List<Object> params = new ArrayList<>();

        if (flightDate != null) {
            sql.append(" AND CAST(flight_date AS date) = CAST(? AS date)");
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

            sql.append(" AND cast(flight_date as time) BETWEEN cast(? as time) AND cast(? as time)");
            params.add(minTime);
            params.add(maxTime);
        }

        if (maxPrice != null) {
            sql.append(" AND price < ?");
            params.add(maxPrice);
        }

        if (airlineName != null) {
            sql.append(" AND airline_name = ?");
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

        sql.append(" ORDER BY flight_date ASC");

        return jdbcClient.sql(sql.toString())
                .params(params.toArray())
                .query(Flight.class)
                .list();
    }

    // Tagastab kõik võetud kohad vastavalt lennu ID-le
    List<String> getTakenSeats(Integer id) {
        return jdbcClient.sql("SELECT seat_id from seats where flight_id = ? and is_taken;")
                .params(id)
                .query(String.class)
                .list();
    }

    class Seat {
        String letter;
        int number;

        Seat(String seat) {
            this.letter = seat.replaceAll("\\d", "");
            this.number = Integer.parseInt(seat.replaceAll("[A-Z]", ""));
        }
    }

    // Vaatan, kas kaks kohta on kõrvuti. Võrdlen numbri ja tähe järgi
    boolean areSeatsTogether(String seatA, String seatB) {
        Seat a = new Seat(seatA);
        Seat b = new Seat(seatB);

        return a.number == b.number && Math.abs(a.letter.charAt(0) - b.letter.charAt(0)) < 2 &&
                !(a.letter.equals("C") && b.letter.equals("D")) &&
                !(a.letter.equals("D") && b.letter.equals("C"));
    }

    // annab leitud kohtadele kuni 12 punkti et hinnata kui hästi need kasutajale
    // sobivad, panin kõige suurema kaalu kohtade kokkupaigutamisele, sest see
    // tundus isiklikult kõige olulisem
    private Integer seatsRating(List<String> foundSeats, Integer numSeatsNeeded, Boolean windowSeat, Boolean legRoom,
            Boolean aisle) {
        // 0-6 punkti kas kohti on piisavalt kasutaja inimestele
        Integer rating = Math.round((foundSeats.size() / numSeatsNeeded) * 3) * foundSeats.size();

        for (int i = 0; i < foundSeats.size(); i++) {
            Seat a = new Seat(foundSeats.get(i));

            // kui rida on esimene ehk rohkema jalaruumiga ja seda soovitakse lisan
            // reitingusse punktid ja maaran legroom valeks et boonust ei saaks mitu korda
            // saada
            if (legRoom && a.number == 1) {
                legRoom = false;
                rating += 2;
            }

            if (windowSeat && (a.letter.equals("A") || a.letter.equals("F"))) {
                windowSeat = false;
                rating += 2;
            }

            if (aisle && (a.letter.equals("C") || a.letter.equals("D"))) {
                aisle = false;
                rating += 2;
            }
        }

        return rating;
    }

    // Soovitan kohti vastavalt lennu ID-le ja vajalikule kohtade arvule
    List<String> recommendedSeats(Integer flightId, Integer numSeatsNeeded, Boolean windowSeat, Boolean legRoom,
            Boolean aisle) {
        List<String> freeSeats = jdbcClient
                .sql("SELECT seat_id FROM seats WHERE flight_id = ? AND NOT is_taken;")
                .params(flightId)
                .query(String.class)
                .list();

        if (freeSeats.size() < numSeatsNeeded) {
            return Collections.emptyList();
        }

        // teen mapi kus keyd on listid kohtadest ja vaartused on reiting sellele
        // listile skaalal 1-10
        Map<List<String>, Integer> seatRatings = new HashMap<>();

        List<String> currentGroup = new ArrayList<>();
        currentGroup.add(freeSeats.get(0));
        for (int i = 1; i < freeSeats.size(); i++) {
            // kui järgmine koht ei ole enam kõrvuti lisan grupi ja teen uue grupi
            if (!areSeatsTogether(currentGroup.getLast(), freeSeats.get(i))) {
                Integer rating = seatsRating(currentGroup, numSeatsNeeded, windowSeat, legRoom, aisle);
                seatRatings.put(currentGroup, rating);
                currentGroup = new ArrayList<>();
            }

            // kui grupi suurus läheb suuremaks kui vaja lisan grupi ja teen uue
            if (currentGroup.size() + 1 > numSeatsNeeded) {
                Integer rating = seatsRating(currentGroup, numSeatsNeeded, windowSeat, legRoom, aisle);
                seatRatings.put(currentGroup, rating);
                currentGroup = new ArrayList<>();
            }
            currentGroup.add(freeSeats.get(i));
        }

        // kui list pole tyhi anname ka viimasele setile hinnangu
        if (!currentGroup.isEmpty()) {
            Integer rating = seatsRating(currentGroup, numSeatsNeeded, windowSeat, legRoom, aisle);
            seatRatings.put(currentGroup, rating);
        }

        // teen uue listi kohtade gruppidest, mis on sorteeritud nende reitingute järgi
        List<List<String>> sortedSeatLists = seatRatings.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((Comparator.reverseOrder()))).map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // lisan parimaid gruppe kokku kuni saan piisavalt suure grupi
        currentGroup.clear();
        int i = 0;
        while (currentGroup.size() < numSeatsNeeded) {
            currentGroup.addAll(sortedSeatLists.get(i));
            i++;
        }
        return currentGroup.subList(0, numSeatsNeeded);
    }
}
