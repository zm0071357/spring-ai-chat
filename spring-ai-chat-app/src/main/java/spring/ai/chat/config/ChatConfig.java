package spring.ai.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import spring.ai.chat.types.common.Constants;

/**
 * 对话配置类
 */
@Slf4j
@Configuration
public class ChatConfig {

    @Bean
    public ChatMemory chatMemory() {
        log.info("加载会话记忆管理");
        return new InMemoryChatMemory();
    }

    @Bean("ollamaChatClient")
    public ChatClient ollamaChatClient(OllamaChatModel model, ChatMemory chatMemory) {
        log.info("加载Ollama对话客户端");
        return ChatClient.builder(model)
                .defaultSystem(Constants.DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .build();
    }

    @Bean("openAIChatClient")
    public ChatClient openAIChatClient(OpenAiChatModel model, ChatMemory chatMemory) {
        log.info("加载OpenAI对话客户端");
        return ChatClient.builder(model)
                .defaultSystem(Constants.DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .build();
    }

}
