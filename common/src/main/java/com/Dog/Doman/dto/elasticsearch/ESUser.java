package com.Dog.Doman.dto.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "user_index")
public class ESUser {

    @Id
    private Long userId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String username;

    @Field(type =FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Date)
    private LocalDateTime createTime;
}
