package com.example.lennuRakendus.flights;

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

@Repository
public class FlightRepository {

    private final JdbcClient jdbcClient;
    private static final Logger logger = LoggerFactory.getLogger(FlightRepository.class);

    public FlightRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
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

    // Salvestan lennu andmed andmebaasi, lisades koha info
    public void saveToDB(Flight flight, int id) {
        jdbcClient.sql(
                "INSERT INTO flights(id, airline_name, departure_airport, destination_airport, arrival_date, flight_date, price) values(:id, :airline, :departureAirport, :destinationAirport, cast(:flightArrivalDate as timestamp), cast(:flightDate as timestamp), :price)")
                .param("id", id)
                .param("airline", flight.airline_name())
                .param("departureAirport", flight.departure_airport())
                .param("destinationAirport", flight.destination_airport())
                .param("flightArrivalDate", flight.arrival_date())
                .param("flightDate", flight.flight_date())
                .param("price", flight.price())
                .update();
        saveSeatsToDB(flight, id);
    }

    // Loob kõikide lendude jaoks andmebaasi vajalikud tabelid
    // ja salvestab kõik lennud, kui andmebaas on tühi (ehk dockeri esmakordsel
    // jooksutamisel)
    public void saveAll(List<Flight> flights) {
        int id = 1;
        jdbcClient.sql(
                "CREATE TABLE IF NOT EXISTS flights (id INT PRIMARY KEY, airline_name VARCHAR(255), departure_airport VARCHAR(255), destination_airport VARCHAR(255), arrival_date TIMESTAMP, flight_date TIMESTAMP, price integer)")
                .update();

        // teen eraldi tabeli kohtade hoimdiseks
        jdbcClient.sql("CREATE TABLE IF NOT EXISTS seats (flight_id INT, seat_id VARCHAR(255), is_taken BOOLEAN)")
                .update();

        List<String> isFlightEmpty = jdbcClient.sql("SELECT COUNT(*) FROM flights").query(String.class).list();
        if (isFlightEmpty.get(0).equals("0")) {
            logger.info("table empty");

            for (Flight flight : flights) {
                saveToDB(flight, id++);
            }
        }

    }

    // Tagastan kõik saadavad lennujaamad sihtkohtadeks
    public List<String> getDestinations() {
        return jdbcClient.sql("select destination_airport from flights;")
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

    // Soovitan kohti vastavalt lennu ID-le ja vajaliku kohtade arvule
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
