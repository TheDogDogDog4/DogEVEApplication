package com.dog.usersystem.listener;

import com.Dog.Doman.Result;
import com.Dog.Doman.ResultEnum;
import com.Dog.Doman.dto.elasticsearch.ESUser;
import com.Dog.Doman.dto.postgreSQL.PgUser;
import com.Dog.Exception.BusinessException;
import com.Dog.Feign.ElasticsearchFeignClient;
import com.dog.usersystem.dao.UserMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = "user-register-topic",
        consumerGroup = "user-es-consumer-group"
)
public class UserEsSyncConsumer implements RocketMQListener<Long> {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ElasticsearchFeignClient elasticsearchFeignClient;

    @Override
    public void onMessage(Long message) {
        PgUser user = userMapper.selectById(message);

        ESUser userDTO = new ESUser();
        BeanUtils.copyProperties(user, userDTO);

        Result<Void> voidResult = elasticsearchFeignClient.saveUser(userDTO);

        if (voidResult.getCode() != 200) {
            throw new BusinessException(ResultEnum.ES_SYNC_ERROR);
        }
    }
}
