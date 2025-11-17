package spring.ai.chat.domain.chat.service.nodeaction.task;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static spring.ai.chat.types.common.Constants.*;

/**
 * 任务助手状态图 - 任务分析节点
 */
@Slf4j
public class TaskAnalysisNodeAction implements NodeAction {

    private final ChatClient taskAnalysisClient;

    public TaskAnalysisNodeAction(ChatClient taskAnalysisClient) {
        this.taskAnalysisClient = taskAnalysisClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("任务助手状态图 - 任务分析节点");
        String prompt = state.value("prompt", "null");
        String userId = state.value("userId", "null");
        int maxStep = state.value("maxStep", DEFAULT_MAX_STEP);
        int currentStep = state.value("currentStep", DEFAULT_CURRENT_STEP);
        log.info("任务分析节点 - 任务：{}，用户：{}，最大执行步数：{}，当前执行步数：{}", prompt, userId, maxStep, currentStep);

        // 提示词
        List<String> historyList = state.value("history", List.of());
        String history = String.join("\n", historyList);
        String analysisPrompt = String.format("""
                    **原始用户需求:** %s
                    
                    **历史质量监督:** %s
                        
                    **分析要求:**
                    请深入分析用户的具体需求，制定明确的执行策略：
                    1. 理解用户真正想要什么（如：具体的学习计划、项目列表、技术方案等）
                    2. 分析需要哪些具体的执行步骤（如：搜索信息、检索项目、生成内容等）
                    3. 制定能够产生实际结果的执行策略
                    4. 确保策略能够直接回答用户的问题
                        
                    **输出格式要求:**
                    任务状态分析: [当前任务完成情况的详细分析]
                    下一步策略: [具体的执行计划，包括需要调用的工具和生成的内容]
                    """,
                prompt,
                history);

        // 大模型调用
        Flux<String> analysisFluxResult = taskAnalysisClient.prompt(analysisPrompt)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, userId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1024))
                .stream()
                .content();

        // 收集片段组合成分析结果
        Mono<String> completeText = analysisFluxResult
                .collectList()
                .map(list -> String.join("", list));
        String analysisResult = completeText.block();
        log.info("任务分析节点 - 任务：{}，用户：{}，任务分析结果：\n{}", prompt, userId, analysisResult);
        assert analysisResult != null;

        // 执行步数增加
        currentStep ++;

        // 是否继续执行
        String isContinue;
        if (currentStep >= maxStep) {
            log.info("任务分析节点 - 任务：{}，用户：{}，最大执行步数：{}，当前执行步数：{}，已达最大执行步数，进入结果总结节点", prompt, userId, maxStep, currentStep);
            isContinue = "NO";
        } else {
            log.info("任务分析节点 - 任务：{}，用户：{}，最大执行步数：{}，当前执行步数：{}，继续执行，进入任务执行节点", prompt, userId, maxStep, currentStep);
            isContinue = "YES";
        }

        // 写入上下文
        return Map.of(
                "analysisResult", analysisResult,
                "currentStep", currentStep,
                "isContinue", isContinue,
                "isCompleted", "NO"
        );
    }

}
