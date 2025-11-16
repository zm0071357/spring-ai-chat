package spring.ai.chat.domain.chat.service.nodeaction;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

@Slf4j
public class TranslationNodeAction implements NodeAction {

    private final ChatClient chatClient;

    public TranslationNodeAction(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("状态图 - 翻译节点");
        String sentence = state.value("sentence", "hello world");
        PromptTemplate promptTemplate = new PromptTemplate("你是一个英语专家，可以根据将给出的英语句子翻译成中文，只需要返回翻译后的中文，不需要返回其他信息，给出的单词：{sentence}");
        promptTemplate.add("sentence", sentence);
        String translation = chatClient.prompt().user(promptTemplate.render()).call().content();
        return Map.of("translation", translation);
    }
}
