package top.hiyorin.clashservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
public class ClashController {
    private static final Logger logger = LoggerFactory.getLogger(ClashController.class);

    @Autowired
    ClashService clashService;

    @GetMapping
    private ResponseEntity<String> getClash(
            @RequestParam("usr") String base64,
            @RequestParam(value = "interval", required = false) Integer interval,
            @RequestParam(value = "rename", required = false) String rename,
//            @RequestParam(value = "beta", required = false) Boolean beta,
            HttpServletRequest request) throws InterruptedException {
        String userAgent = request.getHeader("User-Agent");
        if (!userAgent.startsWith("ClashforWindows")) {
            if (!userAgent.startsWith("ClashForAndroid")) {
                if (!userAgent.startsWith("Shadowrocket")) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("");
                }
            }
        }

        User user = clashService.selectUser(base64);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("");
        }
        logger.info("\n[User " + user.getName() + " gets a subscription with " + userAgent + ']');
        clashService.updateCache();
        Thread.sleep(400);

        if (interval == null) {
            interval = 24;
        }
        if (rename == null) {
            rename = "桜の塔";
        }
        String fileName = URLEncoder.encode(rename, StandardCharsets.UTF_8);

        Template template = clashService.getTemplate();
        String disposition = "attachment; filename=\"" + fileName + "\"; filename*=utf-8''" + fileName;
        String userInfo = clashService.setUserInfo(template.getCache(), user.getExpires());
        Thread.sleep(400);

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
