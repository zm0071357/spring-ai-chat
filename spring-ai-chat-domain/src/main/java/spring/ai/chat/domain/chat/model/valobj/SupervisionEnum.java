package spring.ai.chat.domain.chat.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 是否通过枚举
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum SupervisionEnum {

    PASS("通过"),

    NOTPASS("不通过"),

    REOPTIMIZATION("再优化"),
    ;

    private String supervision;

    /**
     * 获取通过情况
     * @param supervisionResult 质量监督结果
     * @return
     */
    public static String getSupervision(String supervisionResult) {
        if (supervisionResult.contains("是否通过: 通过")) {
            return PASS.supervision;
        } else if (supervisionResult.contains("是否通过: 再优化")) {
            return REOPTIMIZATION.supervision;
        } else {
            return NOTPASS.supervision;
        }
    }

}
