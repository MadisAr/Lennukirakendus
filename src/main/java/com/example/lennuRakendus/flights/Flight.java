package com.example.lennuRakendus.flights;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//flight record lennuandmete mappimiseks
@JsonIgnoreProperties(ignoreUnknown = true)
public record Flight(
        Integer id,
        String flight_date,
        String arrival_date,
        String airline_name,
        String departure_airport,
        String destination_airport,
        Integer price) {
}