package com.itaul.rofm.repository;

import com.itaul.rofm.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    List<Location> findByUser_Id(Long id);
}
