package com.Dog.Feign;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "user-system")
public interface UserFeignClient {
}
