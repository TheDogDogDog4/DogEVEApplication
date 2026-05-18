package com.Dog.Feign;

import com.Dog.Doman.Result;
import com.Dog.Doman.dto.elasticsearch.ESUser;
import com.Dog.Doman.dto.postgreSQL.PgUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "elasticsearch-system")
public interface ElasticsearchFeignClient {

    @PostMapping("/save")
    Result<Void> saveUser(ESUser user);

    @GetMapping("/query")
    Result<PgUser> queryUser(@RequestHeader Long userId);

    @DeleteMapping("/delete")
    Result<Void> deleteUser(@RequestHeader Long userId);

    @PostMapping("save/all")
    Result<Void> saveAllUser(@RequestBody Iterable<PgUser> users);

    @GetMapping("/query/all")
    Result<List<PgUser>> queryAllUser();
}
