package com.itaul.rofm.repository;

import com.itaul.rofm.model.Quest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestRepository extends JpaRepository<Quest, UUID> {

    List<Quest> findByUser_Id(Long id);

    List<Quest> findByUser_Id(Long id, Pageable pageable);

    List<Quest> findByPublished(boolean published);

    List<Quest> findByPublished(boolean published, Pageable pageable);


}
