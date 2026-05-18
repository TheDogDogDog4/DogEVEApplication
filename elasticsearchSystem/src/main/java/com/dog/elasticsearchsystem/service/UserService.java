package com.dog.elasticsearchsystem.service;

import com.Dog.Doman.Result;
import com.Dog.Doman.dto.elasticsearch.ESUser;

import java.util.List;

public interface UserService {
    Result<Void> saveUser(ESUser user);
    Result<ESUser> queryUser(Long userId);
    Result<Void> deleteUser(Long userId);
    Result<Void> saveAllUser(Iterable<ESUser> users);
    Result<List<ESUser>> queryAllUser();
}
