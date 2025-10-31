package spring.ai.chat.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 返回码枚举
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ResponseCodeEnum {

    SUCCESS("0000", "成功"),
    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    INDEX_EXCEPTION("0003", "唯一索引冲突"),
    UPDATE_ZERO("0004", "更新记录为0"),
    HTTP_EXCEPTION("0005", "HTTP接口异常"),
    ;
    private String code;
    private String info;
}
