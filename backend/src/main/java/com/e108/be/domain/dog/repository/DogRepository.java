package com.e108.be.domain.dog.repository;

import com.e108.be.domain.dog.entity.Dog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DogRepository extends JpaRepository<Dog, Long> {
}
