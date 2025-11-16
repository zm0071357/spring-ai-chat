package spring.ai.chat.api;

import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import spring.ai.chat.api.dto.ChatRequestDTO;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ChatService {

    /**
     * 对话
     * @param chatRequestDTO
     * @return
     */
    String chat(ChatRequestDTO chatRequestDTO);

    /**
     * 流式输出
     * @param chatRequestDTO
     * @return
     */
    Flux<String> chatStream(ChatRequestDTO chatRequestDTO);

    /**
     * 上传知识库
     * @param tag 标签
     * @param fileList 文件集合
     * @return
     */
    String upload(String tag, List<MultipartFile> fileList);

    /**
     * 拉取Git代码库并上传知识库
     * @param tag 标签
     * @param repoUrl 拉取地址
     * @return
     */
    String repoGit(String tag, String repoUrl) throws IOException;


    Map<String, Object> testGraph(String word);
}

