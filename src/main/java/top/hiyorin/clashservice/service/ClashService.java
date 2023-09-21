package top.hiyorin.clashservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import top.hiyorin.clashservice.mapper.ClashMapper;
import top.hiyorin.clashservice.model.User;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@EnableScheduling
public class ClashService {
    @Autowired
    ClashMapper clashMapper;

    public User selectUser(String base64) {
        return clashMapper.selectUser(base64);
    }

    public Boolean checkSubscription(User user) {
        LocalDate currentDate = LocalDate.now();
        LocalDate expirationDate = LocalDate.parse(user.getExpires());
        return currentDate.isBefore(expirationDate) && user.getType() > 0;
    }

    public String setUserInfo(String expires) {
        LocalDate date = LocalDate.parse(expires).plusDays(-1);
        LocalDateTime dateTime = date.atStartOfDay();
        long timestamp = dateTime.toEpochSecond(ZoneOffset.UTC) - 28800;
        return clashMapper.getCache() + timestamp;
    }

    public String getRules(Boolean beta) {
        String serverUrl = "http://localhost/private/release.yml";
        if (beta) {
            serverUrl = "http://localhost/private/beta.yml";
        }
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
        ResponseEntity<byte[]> response = restTemplate.exchange(
                serverUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class
        );
        return new String(response.getBody(), StandardCharsets.UTF_8);
    }

    @Async
    @Scheduled(fixedDelay = 600000)
    public void updateCache() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Clash");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                clashMapper.getSubscribeUrl(), HttpMethod.GET, requestEntity, String.class);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            HttpHeaders responseHeaders = responseEntity.getHeaders();
            String subInfo = responseHeaders.getFirst("subscription-userinfo");
            assert subInfo != null;
            int lastEqualIndex = subInfo.lastIndexOf("=");
            String userInfo = subInfo.substring(0, lastEqualIndex + 1);
            clashMapper.updateCache(userInfo);
        }
    }
}
