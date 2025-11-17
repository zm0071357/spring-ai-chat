package spring.ai.chat.domain.chat.service.nodeaction.task;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static spring.ai.chat.types.common.Constants.*;

/**
 * 任务助手状态图 - 任务执行节点
 */
@Slf4j
public class TaskPrecisionNodeAction implements NodeAction {

    private final ChatClient taskPrecisionClient;

    public TaskPrecisionNodeAction(ChatClient taskPrecisionClient) {
        this.taskPrecisionClient = taskPrecisionClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("任务助手状态图 - 任务执行节点");
        String prompt = state.value("prompt", "null");
        String userId = state.value("userId", "null");
        int maxStep = state.value("maxStep", DEFAULT_MAX_STEP);
        int currentStep = state.value("currentStep", maxStep - 1);
        log.info("任务执行节点 - 任务：{}，用户：{}，最大执行步数：{}，当前执行步数：{}", prompt, userId, maxStep, currentStep);

        // 提示词
        String analysisResult = state.value("analysisResult", "执行当前需求");
        String history = state.value("history", "null");
        String precisionPrompt = String.format("""
                    **用户原始需求:** %s
                    
                    **分析助手策略:** %s
                    
                    **历史质量监督:** %s
                    
                    **执行指令:** 你是一个精准任务执行器，需要根据用户需求和分析助手策略，实际执行具体的任务。
                    
                    **执行要求:**
                    1. 直接执行用户的具体需求（如搜索、检索、生成内容等）
                    2. 如果需要搜索信息，请实际进行搜索和检索
                    3. 如果需要生成计划、列表等，请直接生成完整内容
                    4. 提供具体的执行结果，而不只是描述过程
                    5. 确保执行结果能直接回答用户的问题
                    
                    **输出格式:**
                    执行目标: [明确的执行目标]
                    执行过程: [实际执行的步骤和调用的工具]
                    执行结果: [具体的执行成果和获得的信息/内容]
                    质量检查: [对执行结果的质量评估]
                    """,
                prompt,
                analysisResult,
                history);

        // 大模型调用
        Flux<String> precisionFluxResult = taskPrecisionClient.prompt(precisionPrompt)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, userId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1024))
                .stream()
                .content();

        // 收集片段组合成执行结果
        Mono<String> completeText = precisionFluxResult
                .collectList()
                .map(list -> String.join("", list));
        String precisionResult = completeText.block();
        assert precisionResult != null;
        log.info("任务执行节点 - 任务：{}，用户：{}，任务执行结果：\n{}", prompt, userId, precisionResult);

        // 执行步数增加
        currentStep ++;

        // 是否继续执行
        String isContinue;
        if (currentStep >= maxStep) {
            log.info("任务执行节点 - 任务：{}，用户：{}，最大执行步数：{}，当前执行步数：{}，已达最大执行步数，进入结果总结节点", prompt, userId, maxStep, currentStep);
            isContinue = "NO";
        } else {
            log.info("任务执行节点 - 任务：{}，用户：{}，最大执行步数：{}，当前执行步数：{}，继续执行，进入质量监督节点", prompt, userId, maxStep, currentStep);
            isContinue = "YES";
        }

        // 写入上下文
        return Map.of(
                "precisionResult", precisionResult,
                "currentStep", currentStep,
                "isContinue", isContinue,
                "isCompleted", "NO"
        );
    }

}
