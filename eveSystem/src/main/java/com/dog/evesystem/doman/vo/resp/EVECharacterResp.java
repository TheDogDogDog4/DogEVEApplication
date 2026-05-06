package com.dog.evesystem.doman.vo.resp;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EVECharacterResp {

    private String characterName;
    private LocalDateTime ExpiresOn;
}
