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

@SuppressWarnings("SpellCheckingInspection")
@RestController
public class ClashController {
    private static final Logger logger = LoggerFactory.getLogger(ClashController.class);

    @Autowired
    ClashService clashService;

    @GetMapping
    private ResponseEntity<String> getClash(
            @RequestParam(value = "usr", required = false) String base64,
            @RequestParam(value = "rename", required = false) String rename,
//            @RequestParam(value = "beta", required = false) Boolean beta,
            HttpServletRequest request) throws InterruptedException {
        if (base64 == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("");
        }

        String userAgent = request.getHeader("User-Agent");
        if (!userAgent.startsWith("ClashforWindows")) {
            if (!userAgent.startsWith("ClashForAndroid")) {
                if (!userAgent.startsWith("Shadowrocket")) {
                    logger.info("\nBlocked form " + userAgent);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("");
                }
            }
        }

        User user = clashService.selectUser(base64);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("");
        }
        logger.info("\n[User " + user.getName() + " gets a sub by " + userAgent + ']');
        clashService.updateCache();
        Thread.sleep(800);

        if (rename == null) {
            rename = "桜の塔";
        }
        String fileName = URLEncoder.encode(rename, StandardCharsets.UTF_8);
        String disposition = "attachment; filename=\"" + fileName + "\"; filename*=utf-8''" + fileName;

        Template template = clashService.getTemplate();
        String userInfo = clashService.setUserInfo(template.getCache(), user.getExpires());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", disposition);
        headers.add("Profile-Update-Interval", "24");
        headers.add("Subscription-UserInfo", userInfo);

        if (!clashService.checkSubscription(user)) {
            return ResponseEntity.status(HttpStatus.OK)
                    .headers(headers)
                    .body("Expired Subscription!\n");
        }

        String profiles = template.getRule();

        if (userAgent.startsWith("Shadowrocket") & user.getType() < 2) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(profiles.substring(0, profiles.indexOf("} #") + 1));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body("# " + userAgent + ',' + user.getName() + '\n' + profiles);
    }
}
