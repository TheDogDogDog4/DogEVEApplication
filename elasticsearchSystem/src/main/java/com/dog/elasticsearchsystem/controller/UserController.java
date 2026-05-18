package com.dog.elasticsearchsystem.controller;

import com.Dog.Doman.Result;
import com.Dog.Doman.dto.elasticsearch.ESUser;
import com.dog.elasticsearchsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
@RequestMapping("/elasticsearch")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/save")
    public Result<Void> saveUser(ESUser user) {
        return userService.saveUser(user);
    }

    @GetMapping("/query")
    public Result<ESUser> queryUser(@RequestHeader Long userId) {
        return userService.queryUser(userId);
    }

    @DeleteMapping("/delete")
    public Result<Void> deleteUser(@RequestHeader Long userId) {
        return userService.deleteUser(userId);
    }

    @PostMapping("save/all")
    public Result<Void> saveAllUser(@RequestBody Iterable<ESUser> users) {
        return userService.saveAllUser(users);
    }

    @GetMapping("/query/all")
    public Result<List<ESUser>> queryAllUser() {
        return userService.queryAllUser();
    }
}
