package spring.ai.chat.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static spring.ai.chat.types.common.Constants.*;

/**
 * 对话配置类
 */
@Slf4j
@Configuration
public class OpenAIConfig {

    @Bean("chatMemory")
    public ChatMemory chatMemory() {
        log.info("加载OpenAI - ChatMemory记忆配置");
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }

    @Bean("messageChatMemoryAdvisor")
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        log.info("加载OpenAI - MessageChatMemoryAdvisor上下文记忆");
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean("openAIChatClient")
    public ChatClient openAIChatClient(OpenAiChatModel model,
                                       ToolCallbackProvider toolCallbackProvider,
                                       MessageChatMemoryAdvisor messageChatMemoryAdvisor) {
        log.info("加载OpenAI - openAIChatClient普通对话客户端");
        return ChatClient.builder(model)
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(messageChatMemoryAdvisor)
                .build();
    }

    @Bean("vectorStore")
    @ConditionalOnBean(name = "pgVectorJdbcTemplate")
    public PgVectorStore pgVectorStore(@Value("${spring.ai.openai.base-url}") String baseUrl,
                                       @Value("${spring.ai.openai.api-key}") String apiKey,
                                       @Value("${spring.ai.openai.embedding.options.model}") String model,
                                       @Value("${spring.ai.openai.embedding.options.dimensions}") Integer dimensions,
                                       @Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate) {
        log.info("加载OpenAI - PG向量数据库");
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        OpenAiEmbeddingOptions openAiEmbeddingOptions = OpenAiEmbeddingOptions.builder()
                .model(model)
                .dimensions(dimensions)
                .build();
        OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, openAiEmbeddingOptions);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName("vector_store_openai")
                .build();
    }

    @Bean("tokenTextSplitter")
    public TokenTextSplitter tokenTextSplitter() {
        log.info("加载OpenAI - tokenTextSplitter文件切割");
        return new TokenTextSplitter();
    }

    @Bean("taskAnalysisClient")
    public ChatClient taskAnalysisClient(OpenAiChatModel model,
                                         ToolCallbackProvider toolCallbackProvider,
                                         MessageChatMemoryAdvisor messageChatMemoryAdvisor) {
        log.info("加载OpenAI - taskAnalysisClient任务分析客户端");
        return ChatClient.builder(model)
                .defaultSystem(TASK_ANALYSIS_PROMPT)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(messageChatMemoryAdvisor)
                .build();
    }

    @Bean("taskPrecisionClient")
    public ChatClient taskPrecisionClient(OpenAiChatModel model,
                                          ToolCallbackProvider toolCallbackProvider,
                                          MessageChatMemoryAdvisor messageChatMemoryAdvisor) {
        log.info("加载OpenAI - taskPrecisionClient任务执行客户端");
        return ChatClient.builder(model)
                .defaultSystem(TASK_PRECISION_PROMPT)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(messageChatMemoryAdvisor)
                .build();
    }

    @Bean("qualitySupervisorClient")
    public ChatClient qualitySupervisorClient(OpenAiChatModel model,
                                              ToolCallbackProvider toolCallbackProvider,
                                              MessageChatMemoryAdvisor messageChatMemoryAdvisor) {
        log.info("加载OpenAI - qualitySupervisorClient质量监督客户端");
        return ChatClient.builder(model)
                .defaultSystem(QUALITY_SUPERVISOR_PROMPT)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(messageChatMemoryAdvisor)
                .build();
    }

    @Bean("resultSummaryClient")
    public ChatClient resultSummaryClient(OpenAiChatModel model,
                                          MessageChatMemoryAdvisor messageChatMemoryAdvisor) {
        log.info("加载OpenAI - resultSummaryClient结果总结客户端");
        return ChatClient.builder(model)
                .defaultSystem(RESULT_SUMMARY_PROMPT)
                .defaultAdvisors(messageChatMemoryAdvisor)
                .build();
    }

}
