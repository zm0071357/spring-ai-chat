package spring.ai.chat.config.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.stereotype.Service;

/**
 * 自定义Advisor
 */
@Slf4j
@Service
public class TestAdvisor implements BaseAdvisor {

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        log.info("TestAdvisor - before");
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        log.info("TestAdvisor - after");
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
