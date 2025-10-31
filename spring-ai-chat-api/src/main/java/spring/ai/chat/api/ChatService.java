package spring.ai.chat.api;

import reactor.core.publisher.Flux;
import spring.ai.chat.api.dto.ChatRequestDTO;

public interface ChatService {

    /**
     * 普通对话
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

}
