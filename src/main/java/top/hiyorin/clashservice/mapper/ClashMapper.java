package top.hiyorin.clashservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import top.hiyorin.clashservice.model.User;

@Mapper
public interface ClashMapper {
    User selectUser(String base64);

    String getCache();

    String getSubscribeUrl();

    Integer updateCache(String cache);
}
