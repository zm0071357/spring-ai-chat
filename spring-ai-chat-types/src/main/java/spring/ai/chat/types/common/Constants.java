package spring.ai.chat.types.common;

/**
 * 常量
 */
public class Constants {

    public static final String DEFAULT_SYSTEM_PROMPT = "你是一个有用的拼团服务AI助手";

    public static final String TASK_ANALYSIS_PROMPT = "你是一个专业的任务分析助手，名叫 AutoAgent Task Analyzer。";

    public static final String TASK_PRECISION_PROMPT = "你是一个精准任务执行器，名叫 AutoAgent Precision Executor。";

    public static final String QUALITY_SUPERVISOR_PROMPT = "你是一个专业的质量监督助手，名叫 AutoAgent Quality Supervisor。";

    public static final String RESULT_SUMMARY_PROMPT = "你是一个专业的结果总结助手，名叫 AutoAgent Result Summarizer。";

    public static final int DEFAULT_MAX_STEP = 4;

    public static final int DEFAULT_CURRENT_STEP = 0;

    public static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    public static final String CHAT_MEMORY_RETRIEVE_SIZE_KEY = "chat_memory_response_size";

}
