package com.mejbri.pfe.netopssynchro.repository;

import com.mejbri.pfe.netopssynchro.entity.AppLocation;
import com.mejbri.pfe.netopssynchro.entity.City;
import com.mejbri.pfe.netopssynchro.entity.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppLocationRepository extends JpaRepository<AppLocation, Long> {
    List<AppLocation> findByActiveTrue();
    List<AppLocation> findByCityAndActiveTrue(City city);
    List<AppLocation> findByTypeAndActiveTrue(LocationType type);
    List<AppLocation> findByCityAndTypeAndActiveTrue(City city, LocationType type);
    Optional<AppLocation> findByCityAndType(City city, LocationType type);
}