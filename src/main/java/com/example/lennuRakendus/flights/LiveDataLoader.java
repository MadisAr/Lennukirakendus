package com.example.lennuRakendus.flights;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LiveDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(FlightRepository.class);

    // laeb Tallinna lennujaama kodulehelt alla pdf'i lennugraafiku
    private void downloadPDF(String url) throws URISyntaxException, MalformedURLException, IOException {
        URL u = new URI(url).toURL();
        Path p = Paths.get("src/main/resources/data/flightSchedule.pdf");

        try (InputStream in = u.openStream()) {
            Files.copy(in, p,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void saveFlights(int i, String[] lineParts, List<Flight> flights, String currentDestination) {
        List<Integer> days = lineParts[i].chars().filter(Character::isDigit)
                .map(Character::getNumericValue)
                .boxed()
                .collect(Collectors.toList());

        String depTime = lineParts[i + 1];
        String arrTime = lineParts[i + 2];
        String flightNr = lineParts[i + 3] + lineParts[i + 4];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate dateBegin = LocalDate.parse(lineParts[i + 5], formatter);
        LocalDate dateEnd = LocalDate.parse(lineParts[i + 7], formatter);

        LocalDate dateCurr = dateBegin;
        // lisan limiidi variable'i et mitte liiga palju andmeid lisada iga lennu kohta
        // muidu laksid andmete mahud rakenduse jaoks liiga suureks ja andmete esimest
        // korda laadimine võttis liiga kaua aega
        // päris projektis saaks ühe korraga lihtsalt kõik andmed andmebaasi laadida
        int limiit = 5;
        while (dateCurr.isBefore(dateEnd) && limiit > 0) {


            if (days.contains(dateCurr.getDayOfWeek().getValue())) {
                limiit--;
                int price = (int) (0.2 + Math.random() * 200);
                String depDateTime = dateCurr.format(DateTimeFormatter.ISO_DATE) + " " + depTime;
                String arrDateTime = dateCurr.format(DateTimeFormatter.ISO_DATE) + " " + arrTime;
                Flight f = new Flight(null, depDateTime, arrDateTime, flightNr, "Tallinn",
                        currentDestination, price);
                flights.add(f);
            }
            dateCurr = dateCurr.plusDays(1);
        }
    }

    // loeb pdf'ist read ja salvestab listi
    private List<Flight> readPDF(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            // System.out.println(text);
            List<Flight> flights = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
                String line;
                String currentDestination = null;
                while ((line = reader.readLine()) != null) {
                    String[] lineParts = line.split(" ");
                    if (lineParts[0].matches(".*[a-zA-Z].*")) {
                        for (int i = 0; i < lineParts.length; i++) {
                            if (lineParts[i].matches("^[0-9-]{7}$")) {
                                currentDestination = lineParts[0];
                                saveFlights(i, lineParts, flights, currentDestination);
                            }
                        }
                    } else if (lineParts[0].matches("^[0-9-]{7}$")) {
                        saveFlights(0, lineParts, flights, currentDestination);
                    }
                }
            }
            return flights;
        }
    }

    private String getPDFlink(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();

        Element link = doc.selectFirst("a[href^=https://airport.ee/pdf/airport-lennuplaan]");

        return link.absUrl("href");
    }

    public List<Flight> readFlights() throws IOException, URISyntaxException {
        String link = getPDFlink("https://airport.ee/en/flight-plans/");
        downloadPDF(link);
        List<Flight> flights = readPDF("src/main/resources/data/flightSchedule.pdf");

        return flights;
    }
}
