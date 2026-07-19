package com.fmlite.auth;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 이메일 인증 메일 발송. 발송 실패(자격증명 미설정 등)해도 로그의 링크로 개발 테스트 가능. */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public void sendVerification(String toEmail, String token) {
        String link = frontendBaseUrl + "/verify?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);

        // 개발 편의: 메일 수신 없이도 인증 가능하도록 링크를 항상 로그로 남긴다.
        log.info("[EMAIL-VERIFICATION] to={} link={}", toEmail, link);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("[FM Lite] 이메일 인증을 완료해 주세요");
            helper.setText(buildHtml(link), true);
            mailSender.send(message);
            log.info("[EMAIL-VERIFICATION] sent to {}", toEmail);
        } catch (Exception e) {
            // 발송 실패는 회원가입을 막지 않는다 (위 로그의 링크로 인증 진행 가능)
            log.warn("[EMAIL-VERIFICATION] send failed to {} ({}). 위 로그의 링크로 인증하세요.",
                    toEmail, e.getMessage());
        }
    }

    private String buildHtml(String link) {
        return """
                <div style="font-family:sans-serif;max-width:480px;margin:0 auto">
                  <h2>FM Lite 이메일 인증</h2>
                  <p>아래 버튼을 눌러 이메일 인증을 완료하세요. 링크는 24시간 후 만료됩니다.</p>
                  <p><a href="%s" style="display:inline-block;background:#059669;color:#fff;
                     padding:12px 20px;border-radius:8px;text-decoration:none">이메일 인증하기</a></p>
                  <p style="color:#888;font-size:12px">버튼이 동작하지 않으면 다음 주소를 브라우저에 붙여넣으세요:<br>%s</p>
                </div>
                """.formatted(link, link);
    }
}
