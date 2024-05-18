package top.hiyorin.clashservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import top.hiyorin.clashservice.mapper.ClashMapper;
import top.hiyorin.clashservice.model.Node;
import top.hiyorin.clashservice.model.User;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Service
@EnableScheduling
public class ClashService {
    @Autowired
    ClashMapper clashMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public User selectUser(String base64) {
        return clashMapper.selectUser(base64);
    }

    public Node selectNode(Integer id) {
        return clashMapper.selectNode(id);
    }

    public Boolean checkSubscription(User user) {
        LocalDate currentDate = LocalDate.now();
        LocalDate expirationDate = LocalDate.parse(user.getExpires()).plusDays(1);
        return currentDate.isBefore(expirationDate) && user.getType() > 0;
    }

    public String setUserInfo(String expires, String cache) {
        LocalDate date = LocalDate.parse(expires);
        LocalDateTime dateTime = date.atStartOfDay();
        long timestamp = dateTime.toEpochSecond(ZoneOffset.UTC) - 28800;
        return cache + timestamp;
    }

    public String getRules(Boolean beta, String uuid) {
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
        String rules = new String(Objects.requireNonNull(response.getBody()), StandardCharsets.UTF_8);
        rules = rules.replace("uuid", uuid);
        return rules;
    }

    @SuppressWarnings("SqlNoDataSourceInspection")
    public Integer getTotalRecordsFromDatabase() {
        Integer totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM node_information", Integer.class);
        return totalRecords != null ? totalRecords : 0;
    }

    @Scheduled(fixedDelay = 600000)
    public void updateCacheScheduled() {
        int totalRecords = getTotalRecordsFromDatabase();
        for (int id = 1; id <= totalRecords; id++) {
            updateCache(id);
        }
    }

    @Async
    public void updateCache(Integer id) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Clash");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                clashMapper.getSubscribeUrl(id), HttpMethod.GET, requestEntity, String.class);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            HttpHeaders responseHeaders = responseEntity.getHeaders();
            String subInfo = responseHeaders.getFirst("subscription-userinfo");
            if (subInfo != null) {
                int lastEqualIndex = subInfo.lastIndexOf("=");
                String userInfo = subInfo.substring(0, lastEqualIndex + 1);
                clashMapper.updateCache(userInfo, id);
            }
        }
    }
}
