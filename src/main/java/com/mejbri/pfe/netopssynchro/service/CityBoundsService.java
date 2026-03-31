package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.entity.AppLocation;
import com.mejbri.pfe.netopssynchro.entity.City;
import com.mejbri.pfe.netopssynchro.entity.LocationType;
import com.mejbri.pfe.netopssynchro.repository.AppLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CityBoundsService {

    private final AppLocationRepository locationRepo;

    public Optional<City> detect(double lat, double lng) {
        List<AppLocation> centers = locationRepo.findByTypeAndActiveTrue(LocationType.CITY_CENTER);
        for (AppLocation loc : centers) {
            if (loc.getMinLat() == null) continue;
            if (lat >= loc.getMinLat() && lat <= loc.getMaxLat()
                    && lng >= loc.getMinLng() && lng <= loc.getMaxLng()) {
                return Optional.of(loc.getCity());
            }
        }
        return Optional.empty();
    }

    public double[] getCenterCoordinates(City city) {
        return locationRepo.findByCityAndType(city, LocationType.CITY_CENTER)
                .map(l -> new double[]{l.getLatitude(), l.getLongitude()})
                .orElseThrow(() -> new RuntimeException("No center defined for city: " + city));
    }
}