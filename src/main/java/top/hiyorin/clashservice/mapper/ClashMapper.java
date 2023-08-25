package top.hiyorin.clashservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import top.hiyorin.clashservice.model.Template;
import top.hiyorin.clashservice.model.User;

@Mapper
public interface ClashMapper {
    Integer updateUser(User user);

    User selectUser(String base64);

    Template getTemplate();
}
