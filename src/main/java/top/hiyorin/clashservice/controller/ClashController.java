package top.hiyorin.clashservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.hiyorin.clashservice.model.Template;
import top.hiyorin.clashservice.model.User;
import top.hiyorin.clashservice.service.ClashService;

@RestController
public class ClashController {
    @Autowired
    ClashService clashService;

    @GetMapping
    private ResponseEntity<String> getClash(
            @RequestParam("usr") String base64,
            @RequestParam(value = "interval", required = false) Integer interval) {
        User user = clashService.selectUser(base64);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("");
        }

        if (interval == null) {
            interval = 24;
        }

        Template template = clashService.getTemplate();

        String disposition = "attachment; filename=\"%E6%A1%9C%E3%81%AE%E5%A1%94\"; filename*=utf-8''%E6%A1%9C%E3%81%AE%E5%A1%94";
        String userInfo = clashService.setUserInfo(template.getSubscribeUrl(), user.getExpires());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", disposition);
        headers.add("profile-update-interval", interval.toString());
        headers.add("Subscription-UserInfo", userInfo);

        if (!clashService.checkSubscription(user)) {
            return ResponseEntity.status(HttpStatus.OK)
                    .headers(headers)
                    .body("Forbidden Error!\n");
        }

        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(template.getRule());
    }
}
