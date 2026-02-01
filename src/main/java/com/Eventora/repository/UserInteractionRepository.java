package com.Eventora.repository;

import com.Eventora.entity.UserInteraction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserInteractionRepository extends MongoRepository<UserInteraction, String> {

    Optional<UserInteraction> findByUserId(Long userId);
}
