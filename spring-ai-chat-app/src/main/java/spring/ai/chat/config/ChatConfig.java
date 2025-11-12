package spring.ai.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import spring.ai.chat.config.advisor.TestAdvisor;

import static spring.ai.chat.types.common.Constants.DEFAULT_SYSTEM_PROMPT;

/**
 * 对话配置类
 */
@Slf4j
@Configuration
public class ChatConfig {

    @Bean("chatMemory")
    public ChatMemory chatMemory() {
        log.info("加载ChatMemory");
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }

    @Bean("messageChatMemoryAdvisor")
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        log.info("加载MessageChatMemoryAdvisor");
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean("openAIChatClient")
    public ChatClient openAIChatClient(OpenAiChatModel model,
                                       ToolCallbackProvider toolCallbackProvider,
                                       MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                                       TestAdvisor testAdvisor) {
        log.info("加载OpenAI对话客户端");
        return ChatClient.builder(model)
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(messageChatMemoryAdvisor, testAdvisor)
                .build();
    }

}
