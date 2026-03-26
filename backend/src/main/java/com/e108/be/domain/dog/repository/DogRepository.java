package com.e108.be.domain.dog.repository;

import com.e108.be.domain.dog.entity.Dog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DogRepository extends JpaRepository<Dog, Long> {

    Optional<Dog> findByIdAndUser_Id(Long id, Long userId);

    Optional<Dog> findFirstByUser_Id(Long userId);

    List<Dog> findAllByUser_Id(Long userId);
}
