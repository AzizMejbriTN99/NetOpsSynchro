package com.mejbri.pfe.netopssynchro.repository;

import com.mejbri.pfe.netopssynchro.entity.City;
import com.mejbri.pfe.netopssynchro.entity.StockLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockLocationRepository extends JpaRepository<StockLocation, Long> {
    List<StockLocation> findByCity(City city);
    List<StockLocation> findByActiveTrue();
    List<StockLocation> findByCityAndActiveTrue(City city);
}