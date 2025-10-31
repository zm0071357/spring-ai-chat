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

}
