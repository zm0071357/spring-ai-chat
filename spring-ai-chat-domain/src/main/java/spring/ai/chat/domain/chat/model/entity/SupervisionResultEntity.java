package spring.ai.chat.domain.chat.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 质量监督结果实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupervisionResultEntity {

    /**
     * 质量监督结果
     */
    private String supervisionResult;

    /**
     * 当前执行步数
     */
    private Integer currentStep;

    /**
     * 历史改进建议
     */
    private String history;

}
