package com.dog.elasticsearchsystem.service.Impl;

import com.Dog.Doman.Result;
import com.dog.elasticsearchsystem.dao.UserRepository;
import com.Dog.Doman.dto.elasticsearch.ESUser;
import com.dog.elasticsearchsystem.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public Result<Void> saveUser(ESUser user) {
        log.info("es 加数据 | {}", user.getUserId());
        userRepository.save(user);
        return Result.success();
    }

    @Override
    public Result<ESUser> queryUser(Long userId) {
        log.info("es 查数据 | {}", userId);
        Optional<ESUser> optional = userRepository.findById(userId);
        return Result.success(optional.orElse(null));
    }

    @Override
    public Result<Void> deleteUser(Long userId) {
        log.info("es 删数据 | {}", userId);
        userRepository.deleteById(userId);
        return Result.success();
    }

    @Override
    public Result<Void> saveAllUser(Iterable<ESUser> users) {
        log.info("es 批量加数据 | {}", users);
        userRepository.saveAll(users);
        return Result.success();
    }

    @Override
    public Result<List<ESUser>> queryAllUser() {
        log.info("es 批量查数据");
        Iterable<ESUser> users = userRepository.findAll();
        return Result.success(StreamSupport.stream(users.spliterator(), false).collect(Collectors.toList()));
    }


}
