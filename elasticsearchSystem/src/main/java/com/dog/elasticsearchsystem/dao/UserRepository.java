package com.dog.elasticsearchsystem.dao;

import com.Dog.Doman.dto.elasticsearch.ESUser;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends ElasticsearchRepository<ESUser, Long> {

}
