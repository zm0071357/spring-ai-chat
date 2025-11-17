package spring.ai.chat.domain.chat.service.nodeaction.task;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import spring.ai.chat.domain.chat.model.entity.SupervisionResultEntity;
import spring.ai.chat.domain.chat.model.valobj.SupervisionEnum;
import spring.ai.chat.domain.chat.model.valobj.SupervisionResultEnum;

import java.util.Map;

import static spring.ai.chat.types.common.Constants.*;
import static spring.ai.chat.types.common.Constants.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * 任务助手状态图 - 质量监督节点
 */
@Slf4j
public class QualitySupervisorNodeAction implements NodeAction {

    private final ChatClient qualitySupervisorClient;

    public QualitySupervisorNodeAction(ChatClient qualitySupervisorClient) {
        this.qualitySupervisorClient = qualitySupervisorClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("任务助手状态图 - 质量监督节点");
        String prompt = state.value("prompt", "null");
        String userId = state.value("userId", "null");
        int maxStep = state.value("maxStep", DEFAULT_MAX_STEP);
        int currentStep = state.value("currentStep", DEFAULT_CURRENT_STEP);
        log.info("质量监督节点 - 任务：{}，用户：{}，最大执行步数：{}，当前执行步数：{}", prompt, userId, maxStep, currentStep);

        // 提示词
        String precisionPrompt = state.value("precisionPrompt", "跳过当前节点，直接不通过");
        String supervisionPrompt = String.format("""
                    **用户原始需求:** %s
                    
                    **执行结果:** %s
                    
                    **监督要求:** 
                    请严格评估执行结果是否真正满足了用户的原始需求：
                    1. 检查是否直接回答了用户的问题
                    2. 评估内容的完整性和实用性
                    3. 确认是否提供了用户期望的具体结果（如学习计划、项目列表等）
                    4. 判断是否只是描述过程而没有给出实际答案
                    
                    **输出格式:**
                    需求匹配度: [执行结果与用户原始需求的匹配程度分析]
                    内容完整性: [内容是否完整、具体、实用]
                    问题识别: [发现的问题和不足，特别是是否偏离了用户真正的需求]
                    改进建议: [具体的改进建议，确保能直接满足用户需求]
                    质量评分: [1-10分的质量评分]
                    是否通过: [通过/不通过/再优化]
                    """,
                prompt,
                precisionPrompt);

        // 大模型调用
        Flux<String> supervisionFluxResult = qualitySupervisorClient.prompt(supervisionPrompt)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, userId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1024))
                .stream()
                .content();

        // 收集片段组合成监督结果
        Mono<String> completeText = supervisionFluxResult
                .collectList()
                .map(list -> String.join("", list));
        String supervisionResult = completeText.block();
        assert supervisionResult != null;
        log.info("任务分析节点 - 任务：{}，用户：{}，质量监督结果：\n{}", prompt, userId, supervisionResult);

        // 获取枚举
        SupervisionResultEnum supervisionResultEnum = SupervisionResultEnum.get(++ currentStep >= maxStep, SupervisionEnum.getSupervision(supervisionResult));
        return supervisionResultEnum.getResult(SupervisionResultEntity.builder()
                .supervisionResult(supervisionResult)
                .currentStep(currentStep)
                .history(supervisionResult)
                .build());
    }

}
