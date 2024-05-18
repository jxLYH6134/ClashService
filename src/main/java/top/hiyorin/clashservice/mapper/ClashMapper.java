package top.hiyorin.clashservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import top.hiyorin.clashservice.model.Node;
import top.hiyorin.clashservice.model.User;

@Mapper
public interface ClashMapper {
    User selectUser(String base64);

    Node selectNode(Integer id);

    String getSubscribeUrl(Integer id);

    Integer updateCache(String cache, Integer id);
}
