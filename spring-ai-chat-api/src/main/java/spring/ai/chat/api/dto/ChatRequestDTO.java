package spring.ai.chat.api.dto;

import lombok.Getter;

/**
 * 对话请求体
 */
@Getter
public class ChatRequestDTO {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 提示词
     */
    private String prompt;

    /**
     * 标签
     */
    private String tag;

    /**
     * 检索文件数
     */
    private Integer topK;

    /**
     * 相似度
     */
    private Double similarityThreshold;

}
