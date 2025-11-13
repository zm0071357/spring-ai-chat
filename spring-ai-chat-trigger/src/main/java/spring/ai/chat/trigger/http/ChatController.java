package spring.ai.chat.trigger.http;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import spring.ai.chat.api.ChatService;
import spring.ai.chat.api.dto.ChatRequestDTO;
import spring.ai.chat.domain.chat.service.advisor.RAGAdvisor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.UUID;

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

    @Value("${github.username}")
    private String username;

    @Value("${github.token}")
    private String token;

    @PostMapping("/call")
    @Override
    public String chat(@RequestBody ChatRequestDTO chatRequestDTO) {
        if (StringUtils.isNotBlank(chatRequestDTO.getTag())) {
            log.info("RAG对话");
            SearchRequest searchRequest = SearchRequest.builder()
                    .topK(chatRequestDTO.getTopK() == null ? 5 : chatRequestDTO.getTopK())
                    .filterExpression("knowledge == '" + chatRequestDTO.getTag() + "'")
                    //.similarityThreshold(chatRequestDTO.getSimilarityThreshold() == null ? 0.8 : chatRequestDTO.getSimilarityThreshold())
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
                    //.similarityThreshold(chatRequestDTO.getSimilarityThreshold() == null ? 0.8 : chatRequestDTO.getSimilarityThreshold())
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

    @PostMapping("/repo_git_upload")
    @Override
    public String repoGit(@RequestParam("tag") String tag,
                          @RequestParam("repoUrl") String repoUrl) throws IOException {
        log.info("拉取代码库开始，拉取地址：{}", repoUrl);
        // 系统临时目录
        String localPath = System.getProperty("java.io.tmpdir") + "/clone-repo-" + UUID.randomUUID();
        File localDir = new File(localPath);
        try {
            // 确保目录存在
            FileUtils.forceMkdir(localDir);
            try (Git git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(localDir)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, token))
                    .call()) {
                Files.walkFileTree(Paths.get(localPath), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        try {
                            log.info("文件路径:{}", file.toString());
                            PathResource resource = new PathResource(file);
                            TikaDocumentReader reader = new TikaDocumentReader(resource);
                            List<Document> documents = reader.get();

                            if (documents == null || documents.isEmpty()) {
                                log.warn("文件内容为空，跳过处理: {}", file);
                                return FileVisitResult.CONTINUE;
                            }

                            List<Document> documentSplitterList = tokenTextSplitter.apply(documents);
                            documents.forEach(doc -> doc.getMetadata().put("knowledge", tag));
                            documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", tag));
                            pgVectorStore.accept(documentSplitterList);

                        } catch (IllegalArgumentException e) {
                            log.error("内容异常文件: {} | 错误信息: {}", file, e.getMessage());
                        } catch (Exception e) {
                            log.error("处理文件失败: {} | 错误类型: {}", file, e.getClass().getSimpleName());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            // 删除临时目录
            deleteDirectoryWithRetry(localDir);
        }
        log.info("拉取代码库完成，拉取地址：{}", repoUrl);
        return "success";
    }

    /**
     * 删除目录
     * @param directory
     */
    private void deleteDirectoryWithRetry(File directory) {
        int retryCount = 0;
        while (retryCount < 3) {
            try {
                FileUtils.deleteDirectory(directory);
                log.info("成功删除目录: {}", directory.getAbsolutePath());
                return;
            } catch (IOException e) {
                retryCount++;
                log.warn("删除目录失败(尝试 {} / {}), 原因: {}", retryCount, 3, e.getMessage());
                if (retryCount < 3) {
                    try {
                        Thread.sleep(1000L * retryCount); // 等待时间递增
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("无法删除目录: {}", directory.getAbsolutePath(), e);
                }
            }
        }
    }

}
