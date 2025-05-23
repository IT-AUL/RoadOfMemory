package com.itaul.rofm.repository;

import com.itaul.rofm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long> {
}
