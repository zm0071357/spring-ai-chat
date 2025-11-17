package spring.ai.chat.domain.chat.service.nodeaction.task;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static spring.ai.chat.types.common.Constants.*;
import static spring.ai.chat.types.common.Constants.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * 任务助手状态图 - 结果总结节点
 */
@Slf4j
public class ResultSummaryNodeAction implements NodeAction {

    private final ChatClient resultSummaryClient;

    public ResultSummaryNodeAction(ChatClient resultSummaryClient) {
        this.resultSummaryClient = resultSummaryClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("任务助手状态图 - 结果总结节点");
        String prompt = state.value("prompt", "null");
        String userId = state.value("userId", "null");
        int maxStep = state.value("maxStep", DEFAULT_MAX_STEP);
        int currentStep = state.value("currentStep", DEFAULT_CURRENT_STEP);
        String isCompleted = state.value("isCompleted", "NO");
        String analysisResult = state.value("analysisResult", "null");
        String precisionResult = state.value("precisionResult", "null");
        String supervisionResult = state.value("supervisionResult", "null");
        log.info("结果总结节点 - 任务：{}，用户：{}，最大执行步数：{}，当前执行步数：{}，是否执行完成：{}", prompt, userId, maxStep, currentStep, isCompleted);

        String summaryPrompt = getSummaryPrompt(isCompleted, prompt, analysisResult, precisionResult, supervisionResult);
        // 大模型调用
        Flux<String> summaryFluxResult = resultSummaryClient.prompt(summaryPrompt)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, userId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1024))
                .stream()
                .content();

        // 收集片段组合成监督结果
        Mono<String> completeText = summaryFluxResult
                .collectList()
                .map(list -> String.join("", list));
        String summaryResult = completeText.block();
        assert summaryResult != null;
        log.info("任务分析节点 - 任务：{}，用户：{}，结果总结结果：\n{}", prompt, userId, supervisionResult);

        // 执行步数增加
        currentStep ++;
        log.info("结果总结节点 - 任务：{}，用户：{}，最大执行步数：{}，当前执行步数：{}，任务处理完成", prompt, userId, maxStep, currentStep);

        return Map.of(
                "prompt", prompt,
                "userId", userId,
                "isCompleted", isCompleted,
                "analysisResult", analysisResult,
                "precisionResult", precisionResult,
                "supervisionResult", supervisionResult,
                "summaryResult", summaryResult
        );
    }

    /**
     * 获取提示词
     * @param isCompleted 是否完成
     * @param prompt 提示词
     * @param analysisResult 任务分析结果
     * @param precisionResult 任务执行结果
     * @param supervisionResult 质量监督结果
     * @return
     */
    private static String getSummaryPrompt(String isCompleted, String prompt, String analysisResult, String precisionResult, String supervisionResult) {
        String summaryPrompt;
        if (isCompleted.equals("YES")) {
            summaryPrompt = String.format("""
                    基于以下执行过程，请直接回答用户的原始问题，提供最终的答案和结果：
                    
                    **用户原始问题:** %s
                    
                    **任务分析结果:**
                    %s
                    
                    **任务执行结果:**
                    %s
                    
                    **质量结果:**
                    %s
                    
                    **要求:**
                    1. 直接回答用户的原始问题
                    2. 基于执行过程中获得的信息和结果
                    3. 提供具体、实用的最终答案
                    4. 如果是要求制定计划、列表等，请直接给出完整的内容
                    5. 避免只描述执行过程，重点是最终答案
                    
                    请直接给出用户问题的最终答案：
                    """,
                    prompt,
                    analysisResult,
                    precisionResult,
                    supervisionResult);
        } else {
            summaryPrompt = String.format("""
                    虽然任务未完全执行完成，但请基于已有的执行过程，尽力回答用户的原始问题：
                    
                    **用户原始问题:** %s
                    
                    **任务分析结果:**
                    %s
                    
                    **任务执行结果:**
                    %s
                    
                    **质量结果:**
                    %s
                    
                    **要求:**
                    1. 基于已有信息，尽力回答用户的原始问题
                    2. 如果信息不足，说明哪些部分无法完成并给出原因
                    3. 提供已能确定的部分答案
                    4. 给出完成剩余部分的具体建议
                    
                    请基于现有信息给出用户问题的答案：
                    """,
                    prompt,
                    analysisResult,
                    precisionResult,
                    supervisionResult);
        }
        return summaryPrompt;
    }

}
