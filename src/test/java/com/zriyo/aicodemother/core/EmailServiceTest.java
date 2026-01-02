package com.zriyo.aicodemother.core;

import com.zriyo.aicodemother.service.email.EmailService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest()
@ActiveProfiles("dev") // ← 强制使用 dev 配置
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    // ✅ 正确：用 @Configuration 包裹
    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        public ChatModel chatModel() {
            return new ChatModel() {
                @Override
                public String chat(String userMessage) {
                    return "Mocked response for: " + userMessage;
                }

                @Override
                public ChatResponse doChat(ChatRequest chatRequest) {
                    return ChatResponse.builder().build();
                }
            };
        }
    }

    @Test
    void testSendSimpleEmail_ShouldGenerateHtmlWithCodeAndNotThrow() throws MessagingException {
        String to = "1956018949@qq.com";
        String subject = "【ZriyoCode】您的登录验证码\n欢迎注册！";
        String code = "123456";

//        emailService.sendSimpleEmail(to, subject, code, EmailCaptchaType.LOGIN_VERIFY);
    }

}
