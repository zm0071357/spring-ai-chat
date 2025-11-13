package spring.ai.chat.trigger.http;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import spring.ai.chat.api.ChatService;
import spring.ai.chat.api.dto.ChatRequestDTO;
import spring.ai.chat.domain.chat.service.advisor.RAGAdvisor;

import java.util.List;

import static spring.ai.chat.types.common.Constants.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/chat")
public class ChatController implements ChatService {

    @Resource
    private ChatClient openAIChatClient;

    @Resource
    private PgVectorStore pgVectorStore;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @PostMapping("/call")
    @Override
    public String chat(@RequestBody ChatRequestDTO chatRequestDTO) {
        if (StringUtils.isNotBlank(chatRequestDTO.getTag())) {
            log.info("RAG对话");
            SearchRequest searchRequest = SearchRequest.builder()
                    .topK(chatRequestDTO.getTopK() == null ? 5 : chatRequestDTO.getTopK())
                    .filterExpression("knowledge == '" + chatRequestDTO.getTag() + "'")
                    .build();
            RAGAdvisor ragAdvisor = new RAGAdvisor(pgVectorStore, searchRequest);
            log.info("加载OpenAI - RAGAdvisor检索增强");
            return openAIChatClient.prompt()
                    .user(chatRequestDTO.getPrompt())
                    .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatRequestDTO.getUserId()))
                    .advisors(ragAdvisor)
                    .call()
                    .content();
        }
        log.info("普通对话");
        return openAIChatClient.prompt()
                .user(chatRequestDTO.getPrompt())
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatRequestDTO.getUserId()))
                .call()
                .content();
    }

    @PostMapping(value = "/stream", produces = "text/html; charset=utf-8")
    @Override
    public Flux<String> chatStream(@RequestBody ChatRequestDTO chatRequestDTO) {
        if (StringUtils.isNotBlank(chatRequestDTO.getTag())) {
            log.info("RAG对话");
            SearchRequest searchRequest = SearchRequest.builder()
                    .topK(chatRequestDTO.getTopK() == null ? 5 : chatRequestDTO.getTopK())
                    .filterExpression("knowledge == '" + chatRequestDTO.getTag() + "'")
                    .build();
            RAGAdvisor ragAdvisor = new RAGAdvisor(pgVectorStore, searchRequest);
            log.info("加载OpenAI - RAGAdvisor检索增强");
            return openAIChatClient.prompt()
                    .user(chatRequestDTO.getPrompt())
                    .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatRequestDTO.getUserId()))
                    .advisors(ragAdvisor)
                    .stream()
                    .content();
        }
        log.info("普通对话");
        return openAIChatClient.prompt()
                .user(chatRequestDTO.getPrompt())
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatRequestDTO.getUserId()))
                .stream()
                .content();
    }

    @PostMapping("/upload")
    @Override
    public String upload(@RequestParam("tag") String tag,
                         @RequestParam("fileList") List<MultipartFile> fileList) {
        log.info("上传知识库开始");
        for (MultipartFile file : fileList) {
            // 文档解析
            TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(file.getResource());
            List<Document> documents = tikaDocumentReader.get();
            // 切割分块
            List<Document> documentSplitterList = tokenTextSplitter.apply(documents);
            // 添加标签
            documents.forEach(doc -> doc.getMetadata().put("knowledge", tag));
            documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", tag));
            // 保存
            pgVectorStore.accept(documentSplitterList);
        }
        log.info("上传知识库完成");
        return "success";
    }

}
