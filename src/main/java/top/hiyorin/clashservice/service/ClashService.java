package top.hiyorin.clashservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import top.hiyorin.clashservice.mapper.ClashMapper;
import top.hiyorin.clashservice.model.Template;
import top.hiyorin.clashservice.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class ClashService {
    @Autowired
    ClashMapper clashMapper;

    public Boolean updateUser(Integer id, User user) {
        user.setId(id);
        return clashMapper.updateUser(user) > 0;
    }

    public User selectUser(String base64) {
        return clashMapper.selectUser(base64);
    }

    public Template getTemplate() {
        Template template = clashMapper.getTemplate();
        return template;
    }

    public Boolean checkSubscription(User user) {
        LocalDate currentDate = LocalDate.now();
        LocalDate expirationDate = LocalDate.parse(user.getExpires()).plusDays(1);
        return currentDate.isBefore(expirationDate) && user.getType() > 0;
    }

    public String setUserInfo(String url, String expires) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Clash");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        HttpHeaders responseHeaders = responseEntity.getHeaders();
        String subInfo = responseHeaders.getFirst("subscription-userinfo");

        LocalDate date = LocalDate.parse(expires);
        LocalDateTime dateTime = date.atStartOfDay();
        long timestamp = dateTime.toEpochSecond(ZoneOffset.UTC) - 28800;

        int lastEqualIndex = subInfo.lastIndexOf("=");
        String userInfo = subInfo.substring(0, lastEqualIndex + 1);
        return userInfo + timestamp;
    }
}
