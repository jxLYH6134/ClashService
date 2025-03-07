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
import top.hiyorin.clashservice.model.Node;
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
            @RequestParam(value = "beta", required = false) Boolean beta,
            HttpServletRequest request) {
        if (base64 == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("");
        }

        if (beta == null) {
            beta = false;
        }

        String userAgent = request.getHeader("User-Agent");

        User user = clashService.selectUser(base64);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("");
        }
        if (!beta) {
            if (!userAgent.startsWith("ClashforWindows")) {
                if (!userAgent.startsWith("ClashForAndroid")) {
                    if (!userAgent.startsWith("Shadowrocket")) {
                        logger.info("\nBlocked " + user.getName() + " form " + userAgent);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
                    }
                }
            }
        }
        logger.info("\n[User " + user.getName() + " have got sub from " + userAgent + ']');
        Node node = clashService.selectNode(user.getGroup());
        clashService.updateCache(user.getGroup());

        if (rename == null) {
            rename = "桜の塔";
        }
        String fileName = URLEncoder.encode(rename, StandardCharsets.UTF_8);
//        if (beta) {
//            fileName = fileName + "%20beta";
//        }
        String disposition = "attachment; filename=\"" + fileName + "\"; filename*=utf-8''" + fileName;

        String userInfo = clashService.setUserInfo(user.getExpires(), node.getCache());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", disposition);
        headers.add("Profile-Update-Interval", "24");
        headers.add("Subscription-UserInfo", userInfo);

        if (!clashService.checkSubscription(user)) {
            return ResponseEntity.status(HttpStatus.OK)
                    .headers(headers)
                    .body("Expired Subscription!\n");
        }

        if (beta && !clashService.isBeta(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("");
        }

        String profiles = clashService.getRules(beta, node.getUuid());

        if (clashService.isExtend(user)) {
            Node nodeBackup = clashService.selectNode(1);
            profiles = profiles.replace("pswd", nodeBackup.getUuid());
        }

        if (userAgent.startsWith("Shadowrocket") && clashService.isCut(user)) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(profiles.substring(0, profiles.indexOf("} #") + 1));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body("# " + userAgent + ',' + user.getName() + '\n' + profiles);
    }
}
