package spring.ai.chat.domain.chat.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.ai.chat.domain.chat.model.entity.SupervisionResultEntity;
import spring.ai.chat.types.enums.ResponseCodeEnum;
import spring.ai.chat.types.exception.AppException;

import java.util.Arrays;
import java.util.Map;

/**
 * 质量监督结果枚举
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum SupervisionResultEnum {

    PASS(false, "通过", "未达最大步数 - 通过") {
        @Override
        public Map<String, Object> getResult(SupervisionResultEntity supervisionResultEntity) {
            return Map.of(
                    "supervisionResult", supervisionResultEntity.getSupervisionResult(),
                    "currentStep", supervisionResultEntity.getCurrentStep(),
                    "isPass", "YES",
                    "isCompleted", "YES"
            );
        }
    },

    NOTPASS(false, "不通过", "未达最大步数 - 不通过") {
        @Override
        public Map<String, Object> getResult(SupervisionResultEntity supervisionResultEntity) {
            return Map.of(
                    "supervisionResult", supervisionResultEntity.getSupervisionResult(),
                    "currentStep", supervisionResultEntity.getCurrentStep(),
                    "history", supervisionResultEntity.getHistory(),
                    "isPass", "NO",
                    "isCompleted", "NO"
            );
        }
    },

    REOPTIMIZATION(false, "再优化", "未达最大步数 - 再优化") {
        @Override
        public Map<String, Object> getResult(SupervisionResultEntity supervisionResultEntity) {
            return Map.of(
                    "supervisionResult", supervisionResultEntity.getSupervisionResult(),
                    "currentStep", supervisionResultEntity.getCurrentStep(),
                    "history", supervisionResultEntity.getHistory(),
                    "isPass", "REOPTIMIZATION",
                    "isCompleted", "NO"
            );
        }
    },

    MAXSTEP_PASS(true, "通过", "已达最大步数 - 通过") {
        @Override
        public Map<String, Object> getResult(SupervisionResultEntity supervisionResultEntity) {
            return Map.of(
                    "supervisionResult", supervisionResultEntity.getSupervisionResult(),
                    "currentStep", supervisionResultEntity.getCurrentStep(),
                    "isPass", "MAXSTEP",
                    "isCompleted", "YES"
            );
        }
    },

    MAXSTEP_NOTPASS(true, "不通过", "已达最大步数 - 不通过") {
        @Override
        public Map<String, Object> getResult(SupervisionResultEntity supervisionResultEntity) {
            return Map.of(
                    "supervisionResult", supervisionResultEntity.getSupervisionResult(),
                    "currentStep", supervisionResultEntity.getCurrentStep(),
                    "isPass", "MAXSTEP",
                    "isCompleted", "NO"
            );
        }
    },

    MAXSTEP_REOPTIMIZATION(true, "再优化", "已达最大步数 - 再优化") {
        @Override
        public Map<String, Object> getResult(SupervisionResultEntity supervisionResultEntity) {
            return Map.of(
                    "supervisionResult", supervisionResultEntity.getSupervisionResult(),
                    "currentStep", supervisionResultEntity.getCurrentStep(),
                    "isPass", "MAXSTEP",
                    "isCompleted", "NO"
            );
        }
    },
    ;

    private Boolean isMaxStep;

    private String supervision;

    private String info;

    /**
     * 匹配
     * @param supervisionResultEntity 质量监督结果实体
     * @return
     */
    public abstract Map<String, Object> getResult(SupervisionResultEntity supervisionResultEntity);

    /**
     * 获取对应枚举
     * @param isMaxStep 是否到达最大步数
     * @param supervision 是否通过
     * @return
     */
    public static SupervisionResultEnum get(Boolean isMaxStep, String supervision) {
        return Arrays.stream(SupervisionResultEnum.values())
                .filter(e -> e.getIsMaxStep().equals(isMaxStep) && e.getSupervision().equals(supervision))
                .findFirst()
                .orElseThrow(() -> new AppException(ResponseCodeEnum.E00001.getCode(), ResponseCodeEnum.E00001.getInfo()));
    }

}
