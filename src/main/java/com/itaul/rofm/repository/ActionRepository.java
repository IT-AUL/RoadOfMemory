package com.itaul.rofm.repository;

import com.itaul.rofm.model.ActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActionRepository extends JpaRepository<ActionEntity, UUID> {
    List<ActionEntity> findByLocation_Id(UUID locationId);

}