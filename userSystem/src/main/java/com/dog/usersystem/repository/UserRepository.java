package com.dog.usersystem.repository;

import com.Dog.Doman.dto.mongoDB.MongoUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<MongoUser, String> {
}
