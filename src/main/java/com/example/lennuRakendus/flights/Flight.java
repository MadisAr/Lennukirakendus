package com.example.lennuRakendus.flights;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Flight {
    private Integer id;
    private String flight_date;
    private String arrival_date;
    private String airline;
    private String departure_airport;
    private String destination_airport;
    private Integer price;

    public String getFlight_date() {
        return flight_date;
    }

    public void setFlight_date(String flight_date) {
        this.flight_date = flight_date;
    }

    public String getArrival_date() {
        return arrival_date;
    }

    public void setArrival_date(String arrival_date) {
        this.arrival_date = arrival_date;
    }

    public String getAirline_name() {
        return airline;
    }

    public void setAirline_name(String airline_name) {
        this.airline = airline_name;
    }

    public String getDeparture_airport() {
        return departure_airport;
    }

    public void setDeparture_airport(String departure_airport) {
        this.departure_airport = departure_airport;
    }

    public String getDestination_airport() {
        return destination_airport;
    }

    public void setDestination_airport(String destination_airport) {
        this.destination_airport = destination_airport;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}
