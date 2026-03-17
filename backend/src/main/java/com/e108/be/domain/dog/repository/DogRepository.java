package com.e108.be.domain.dog.repository;

import com.e108.be.domain.dog.entity.Dog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DogRepository extends JpaRepository<Dog, Long> {

    Optional<Dog> findByIdAndUserId(Long id, Long userId);

    Optional<Dog> findFirstByUserId(Long userId);
}
