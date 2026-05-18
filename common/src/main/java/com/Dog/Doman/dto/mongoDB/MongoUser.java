package com.Dog.Doman.dto.mongoDB;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "user")
public class MongoUser {
    @Id
    private Long userId;

    private String username;

    @Transient
    private String password;

    private String email;

    private LocalDateTime createTime;

    @Transient
    private String esiRefreshToken;
}
