package spring.ai.chat.trigger.http;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import spring.ai.chat.api.ChatService;
import spring.ai.chat.api.dto.ChatRequestDTO;

import static spring.ai.chat.types.common.Constants.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/chat")
public class ChatController implements ChatService {

    @Resource
    private ChatClient openAIChatClient;

    @PostMapping("/call")
    @Override
    public String chat(@RequestBody ChatRequestDTO chatRequestDTO) {
        return openAIChatClient.prompt()
                .user(chatRequestDTO.getPrompt())
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatRequestDTO.getUserId()))
                .call()
                .content();
    }

    @PostMapping(value = "/stream", produces = "text/html; charset=utf-8")
    @Override
    public Flux<String> chatStream(@RequestBody ChatRequestDTO chatRequestDTO) {
        return openAIChatClient.prompt()
                .user(chatRequestDTO.getPrompt())
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatRequestDTO.getUserId()))
                .stream()
                .content();
    }

}
