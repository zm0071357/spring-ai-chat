package spring.ai.chat.config;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import spring.ai.chat.domain.chat.service.nodeaction.SentenceNodeAction;
import spring.ai.chat.domain.chat.service.nodeaction.TranslationNodeAction;

import java.util.HashMap;

@Slf4j
@Configuration
public class GraphConfig {

    @Bean("compiledGraph")
    public CompiledGraph compiledGraph(@Qualifier("openAIChatClient") ChatClient chatClient) throws GraphStateException {
        log.info("加载OpenAI - Graph状态图");
        // 定义数据处理策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("word", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("sentence", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("translation", KeyStrategy.REPLACE);
            return keyStrategyHashMap;
        };
        // 定义状态图
        StateGraph stateGraph = new StateGraph("stateGraph", keyStrategyFactory);
        // 新增节点
        stateGraph.addNode("sentenceNodeAction", AsyncNodeAction.node_async(new SentenceNodeAction(chatClient)));
        stateGraph.addNode("translationNodeAction", AsyncNodeAction.node_async(new TranslationNodeAction(chatClient)));
        // 新增边
        stateGraph.addEdge(StateGraph.START, "sentenceNodeAction");
        stateGraph.addEdge("sentenceNodeAction", "translationNodeAction");
        stateGraph.addEdge("translationNodeAction", StateGraph.END);
        // 返回编译后的状态图
        return stateGraph.compile();
    }

}
