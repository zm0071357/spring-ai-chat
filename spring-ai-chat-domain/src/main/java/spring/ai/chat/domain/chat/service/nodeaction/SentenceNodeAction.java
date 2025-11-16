package spring.ai.chat.domain.chat.service.nodeaction;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

@Slf4j
public class SentenceNodeAction implements NodeAction {

    private final ChatClient chatClient;

    public SentenceNodeAction(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("状态图 - 造句节点");
        String word = state.value("word", "hello world");
        PromptTemplate promptTemplate = new PromptTemplate("你是一个英语专家，可以根据给出的单词造一个英语句子，只需要返回造句，不需要返回其他信息，给出的单词：{word}");
        promptTemplate.add("word", word);
        String sentence = chatClient.prompt().user(promptTemplate.render()).call().content();
        return Map.of("sentence", sentence);
    }
}
