package com.mejbri.pfe.netopssynchro.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GeocodeService {

    @Value("${google.maps.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public double[] geocode(String address) {
        try {
            String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
                    + address.replace(" ", "+") + ",+Tunisia&key=" + apiKey;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode location = root.path("results").get(0)
                    .path("geometry").path("location");
            return new double[]{
                    location.path("lat").asDouble(),
                    location.path("lng").asDouble()
            };
        } catch (Exception e) {
            return null;
        }
    }
}
