package spring.ai.chat.config;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import spring.ai.chat.domain.chat.service.nodeaction.english.SentenceNodeAction;
import spring.ai.chat.domain.chat.service.nodeaction.english.TranslationNodeAction;
import spring.ai.chat.domain.chat.service.nodeaction.task.QualitySupervisorNodeAction;
import spring.ai.chat.domain.chat.service.nodeaction.task.ResultSummaryNodeAction;
import spring.ai.chat.domain.chat.service.nodeaction.task.TaskAnalysisNodeAction;
import spring.ai.chat.domain.chat.service.nodeaction.task.TaskPrecisionNodeAction;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class GraphConfig {

    @Bean("englishAssistantGraph")
    public CompiledGraph englishAssistantGraph(@Qualifier("openAIChatClient") ChatClient chatClient) throws GraphStateException {
        log.info("加载OpenAI - 英语助手Graph状态图");
        // 定义数据处理策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("word", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("sentence", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("translation", KeyStrategy.REPLACE);
            return keyStrategyHashMap;
        };
        // 定义状态图
        StateGraph stateGraph = new StateGraph("englishAssistantGraph", keyStrategyFactory);
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

    @Bean("taskAssistantGraph")
    public CompiledGraph taskAssistantGraph(@Qualifier("taskAnalysisClient") ChatClient taskAnalysisClient,
                                            @Qualifier("taskPrecisionClient") ChatClient taskPrecisionClient,
                                            @Qualifier("qualitySupervisorClient") ChatClient qualitySupervisorClient,
                                            @Qualifier("resultSummaryClient") ChatClient resultSummaryClient
    ) throws GraphStateException {
        log.info("加载OpenAI - 任务助手Graph状态图");

        // 定义数据处理策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("prompt", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("userId", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("maxStep", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("currentStep", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("analysisResult", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("precisionResult", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("supervisionResult", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("summaryResult", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("isContinue", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("isPass", KeyStrategy.REPLACE);
            keyStrategyHashMap.put("history", KeyStrategy.APPEND);
            keyStrategyHashMap.put("isCompleted", KeyStrategy.REPLACE);
            return keyStrategyHashMap;
        };

        // 定义状态图
        StateGraph stateGraph = new StateGraph("taskAssistantGraph", keyStrategyFactory);

        // 新增处理节点
        stateGraph.addNode("taskAnalysisNodeAction", AsyncNodeAction.node_async(new TaskAnalysisNodeAction(taskAnalysisClient)));
        stateGraph.addNode("taskPrecisionNodeAction", AsyncNodeAction.node_async(new TaskPrecisionNodeAction(taskPrecisionClient)));
        stateGraph.addNode("qualitySupervisorNodeAction", AsyncNodeAction.node_async(new QualitySupervisorNodeAction(qualitySupervisorClient)));
        stateGraph.addNode("resultSummaryNodeAction", AsyncNodeAction.node_async(new ResultSummaryNodeAction(resultSummaryClient)));

        // 新增边
        // 起始边 - 任务分析
        stateGraph.addEdge(StateGraph.START, "taskAnalysisNodeAction");
        // 任务分析 - 可继续（任务执行）、不可继续（结果总结）
        stateGraph.addConditionalEdges("taskAnalysisNodeAction",
                AsyncEdgeAction.edge_async(state -> state.value("isContinue", "NO")),
                Map.of(
                        "YES", "taskPrecisionNodeAction",
                        "NO", "resultSummaryNodeAction"
                ));
        // 任务执行 - 可继续（质量监督）、不可继续（结果总结）
        stateGraph.addConditionalEdges("taskPrecisionNodeAction",
                AsyncEdgeAction.edge_async(state -> state.value("isContinue", "NO")),
                Map.of(
                        "YES", "qualitySupervisorNodeAction",
                        "NO", "resultSummaryNodeAction"
                ));
        // 质量监督 - 通过（结果总结）、不通过（任务分析）、再优化（任务执行）、已达最大步数（结果总结）
        stateGraph.addConditionalEdges("qualitySupervisorNodeAction",
                AsyncEdgeAction.edge_async(state -> state.value("isPass", "YES")),
                Map.of(
                        "YES", "resultSummaryNodeAction",
                        "NO", "taskAnalysisNodeAction",
                        "REOPTIMIZATION", "taskPrecisionNodeAction",
                        "MAXSTEP", "resultSummaryNodeAction"
                ));
        // 结果总结 - 结束边
        stateGraph.addEdge("resultSummaryNodeAction", StateGraph.END);

        // 返回编译后的状态图
        return stateGraph.compile();
    }

}
