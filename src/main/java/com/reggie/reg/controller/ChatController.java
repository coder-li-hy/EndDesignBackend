package com.reggie.reg.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.reg.common.R;
import com.reggie.reg.dto.LateReasonAnalyzeRequest;
import com.reggie.reg.dto.LateReasonAnalyzeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 对话接口
 * <p>
 * 对外只暴露一个接口：GET /api/chat/stream
 * 前端用 EventSource 连接，后端把大模型的回答一个字一个字地"推"过去
 * 也就是常见的"打字机效果"
 */
@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    // ChatClient 是 Spring AI 提供的"大模型客户端"
    // 所有跟大模型的交互都通过它来完成
    private final ChatClient chatClient;

    // 默认提示词 prompt
    private static final String BASE_SYSTEM = """
            【角色设定】
            你是"课程辅助教学系统"的智能学习助手，专门帮助学生和教师使用本系统。
                
            【你能做的】
            - 解答系统功能使用问题（如：如何选课、提交作业、下载资源、向老师提问等）
            - 尝试解答有关学习上的问题，记得加上“由ai生成，仅供参考”的注释
            - 引导用户完成常见操作流程，用数字步骤清晰列出
            - 解释页面上各字段、按钮的含义
            - 遇到系统故障或账号问题，引导用户联系人工客服
                
            【你不能做的】
            - 不回答与课程教学无关的问题（如：天气、娱乐、政治等），礼貌拒绝即可
            - 不能承诺系统未来会有某某功能
            - 不能替用户执行任何操作（只能指导步骤）
            - 不能泄露其他用户的信息或系统内部逻辑
            - 聊天时不能使用像”\uD83D\uDC4B“这样的表情符号
                
            【回答风格】
            - 简洁友好，口语化，单次回复不超过 200 字
            - 如果过有关相关操作，操作步骤用 1. 2. 3. 列出，清晰易懂
            - 遇到不确定的问题，诚实说"这个我暂时不清楚"，并提供人工客服联系方式
                
            【人工客服】
            如遇系统问题，请联系：邮件 1778362942@qq.com 
            """;


    // 临时加一个测试接口
    @GetMapping("/test-ai")
    public R<String> testAi() {
        try {
            String response = chatClient.prompt()
                    .user("只回复：OK")
                    .call()
                    .content();
            return R.success(response);
        } catch (Exception e) {
            return R.error("AI 调用失败: " + e.getMessage());
        }
    }


    /**
     * 构造方法：初始化 ChatClient
     * Spring Boot 启动时会自动把 ChatClient.Builder 注入进来
     * Builder 会读取 application.yml 里配置的 api-key、model 等参数
     */
    public ChatController(ChatClient.Builder builder) {

        // ---- 第一步：配置对话记忆 ----
        // 大模型本身是"无状态"的，每次请求它都不知道之前说过什么
        // 所以需要我们自己把历史对话存起来，每次请求时一起带过去
        // InMemoryChatMemoryRepository = 把历史记录存在内存里（重启后清空）
        // MessageWindowChatMemory = 滑动窗口，只保留最近 N 条，避免超出 token 限制
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)   // 最多记住最近 20 条对话（10问10答）
                .build();

        // ---- 第二步：构建 ChatClient ----
        // MessageChatMemoryAdvisor 是一个"拦截器"
        // 它会在每次发请求前，自动把历史记录塞进去
        // 在收到回复后，自动把这轮对话存进记忆里
        // 你不需要手动管理消息列表，它全帮你做了
        this.chatClient = builder
                .defaultSystem(BASE_SYSTEM)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * 流式对话接口
     * 请求示例：GET /api/chat/stream?message=你好&sessionId=abc123
     *
     * @param message   用户发送的消息内容
     * @param sessionId 会话ID，用来区分不同用户的对话记忆
     *                  同一个 sessionId = 同一个对话上下文
     *                  前端可以用 crypto.randomUUID() 生成，每个用户一个
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    // produces = TEXT_EVENT_STREAM_VALUE 告诉浏览器：
    // 这个接口返回的是 SSE 格式（Server-Sent Events），不是普通 JSON
    // 浏览器的 EventSource 就是专门接收这种格式的
    public SseEmitter stream(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String sessionId) {

        // SseEmitter 是 Spring MVC 提供的 SSE 推送工具
        // 可以理解为一根"水管"，后端往里写数据，前端实时收到
        // 180_000L = 超时时间 180秒，超时后连接自动断开
        SseEmitter emitter = new SseEmitter(180_000L);

        chatClient.prompt()
                .user(message)    // 用户这次发送的消息内容
                // 把本次请求绑定到对应的会话 ID
                // MessageChatMemoryAdvisor 会根据这个 ID：
                //   1. 发请求前：自动把该会话的历史记录拼进去
                //   2. 收到回复后：自动把本轮对话存进记忆
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                // 开启流式模式
                // 大模型每生成一个 token 就立刻返回，而不是等全部生成完再返回
                // 这样前端才能实现"打字机"效果
                .stream()
                .content()
                // subscribe 订阅这个数据流，类似于"监听"
                // 大模型每吐出一段文字，就会触发下面对应的回调
                .subscribe(
                        // ① 每收到一个 token（一个字或几个字）就立刻推给前端
                        token -> {
                            try {
                                log.info("收到 token: {}", token);
                                // 正确写法：明确指定纯文本，避免被序列化成 "xxx"
                                emitter.send(
                                        token
                                );
                            } catch (Exception e) {
                                // 发送失败（比如用户关闭了页面），终止推送
                                emitter.completeWithError(e);
                            }
                        },
                        // ② 大模型返回报错时，把错误通知前端并关闭连接
                        emitter::completeWithError,
                        // ③ 大模型全部回答完毕，正常关闭连接
                        emitter::complete
                );

        // 立刻返回 emitter，连接保持打开
        // 后续数据会通过上面的 subscribe 回调异步推送
        return emitter;
    }


    /**
     * 迟交理由智能分类分析
     * POST /api/teacher/assignments/{assignmentId}/late-reasons/analyze
     *
     * @param assignmentId 作业 ID
     * @param request 包含迟交理由列表的请求体
     * @return 分类统计结果 + AI 分析建议
     */
    @PostMapping(value = "/teacher/assignments/{assignmentId}/late-reasons/analyze",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public R<LateReasonAnalyzeResponse> analyzeLateReasons(
            @PathVariable Integer assignmentId,
            @RequestBody LateReasonAnalyzeRequest request) {

        try {
            // 1. 参数校验
            if (request.getReasons() == null || request.getReasons().isEmpty()) {
                return R.error("请提供迟交理由列表");
            }

            // 2. 构建 AI 分析 Prompt
            String analysisPrompt = buildLateReasonAnalysisPrompt(request);

            // 3. 调用大模型进行分析（非流式，等待完整响应）
            String aiResponse = chatClient.prompt()
                    .user(analysisPrompt)
                    .call()
                    .content();

            // 4. 解析 AI 返回的 JSON 结果
            LateReasonAnalyzeResponse result = parseAiAnalysisResult(aiResponse);

            // 5. 返回标准响应
            return R.success(result);

        } catch (Exception e) {
            log.error("Late reason analysis failed for assignmentId={}", assignmentId, e);

            // 降级方案：返回基础分类（避免接口完全不可用）
            return R.success(getFallbackAnalysis(request.getReasons()));
        }
    }

    /**
     * 构建迟交理由分析的 Prompt
     */
    private String buildLateReasonAnalysisPrompt(LateReasonAnalyzeRequest request) {
        // 默认分类标签（如果前端未提供）
        List<String> categories = request.getExpectedCategories();
        if (categories == null || categories.isEmpty()) {
            categories = List.of("时间管理", "技术困难", "理解偏差", "个人事务", "其他原因");
        }

        // 格式化理由列表
        String reasonsJson = request.getReasons().stream()
                .map(r -> "  - \"" + r.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining("\n"));

        return """
            【任务】
            你是一名教学数据分析专家，请对学生提交作业迟交的理由进行智能分类和统计。
            
            【分类标签】（请严格使用以下分类，不要新增）
            %s
            
            【待分析的理由列表】（共 %d 条）
            %s
            
            【输出要求】
            1. 请输出严格的 JSON 格式，不要包含任何额外文字或 Markdown 标记
            2. JSON 结构如下：
            {
              "categories": [
                {
                  "name": "分类名称（必须来自上述分类标签）",
                  "value": 该分类的数量（整数）,
                  "color": "建议的图表颜色（从 #667eea, #764ba2, #909399, #c0c4cc, #e6a23c 中选择）",
                  "examples": ["该分类下的 1-2 个典型理由示例"]
                }
              ],
              "summary": "用 1-2 句话总结迟交的主要原因，并给出 1 条教学改进建议"
            }
            3. 确保所有分类的 value 之和等于总理由数
            4. summary 要简洁、实用，避免空话
            
            【开始分析】
            """.formatted(
                categories.stream().map(c -> "- " + c).collect(Collectors.joining("\n")),
                request.getReasons().size(),
                reasonsJson
        );
    }

    /**
     * 解析 AI 返回的 JSON 结果
     */
    private LateReasonAnalyzeResponse parseAiAnalysisResult(String aiResponse) {
        try {
            // 清理可能的 Markdown 标记
            String json = aiResponse.trim()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("\\s*```$", "")
                    .trim();

            // 使用 Jackson 解析（Spring Boot 默认已配置）
            ObjectMapper mapper = new ObjectMapper();
            // 先解析为中间结构
            JsonNode root = mapper.readTree(json);

            // 解析 categories
            List<LateReasonAnalyzeResponse.CategoryStat> categories = new ArrayList<>();
            if (root.has("categories")) {
                for (JsonNode cat : root.get("categories")) {
                    categories.add(new LateReasonAnalyzeResponse.CategoryStat(
                            cat.get("name").asText(),
                            cat.get("value").asInt(),
                            cat.has("color") ? cat.get("color").asText() : "#909399",
                            cat.has("examples") ?
                                    mapper.convertValue(cat.get("examples"), List.class) :
                                    List.of()
                    ));
                }
            }

            // 解析 summary
            String summary = root.has("summary") ? root.get("summary").asText() : "暂无分析建议";

            return new LateReasonAnalyzeResponse(categories, summary);

        } catch (Exception e) {
            log.warn("Failed to parse AI response: {}", aiResponse, e);
            throw new RuntimeException("AI 响应解析失败", e);
        }
    }

    /**
     * 降级方案：当 AI 调用失败时返回基础分类
     */
    private LateReasonAnalyzeResponse getFallbackAnalysis(List<String> reasons) {
        // 简单关键词匹配分类
        Map<String, Integer> stats = new LinkedHashMap<>();
        Map<String, List<String>> examples = new LinkedHashMap<>();

        // 预定义分类关键词
        Map<String, List<String>> keywords = Map.of(
                "时间管理", List.of("忘记", "迟到", "时间", "截止", "安排", "拖延"),
                "技术困难", List.of("电脑", "网络", "软件", "系统", "故障", "坏了", "打不开"),
                "理解偏差", List.of("不懂", "不会", "没看懂", "题目", "要求", "不明白"),
                "个人事务", List.of("生病", "家里", "有事", "请假", "冲突", "紧急"),
                "其他原因", List.of()  // 兜底
        );

        // 统计分类
        for (String reason : reasons) {
            String lower = reason.toLowerCase();
            boolean matched = false;

            for (Map.Entry<String, List<String>> entry : keywords.entrySet()) {
                if (entry.getValue().stream().anyMatch(kw -> lower.contains(kw))) {
                    stats.merge(entry.getKey(), 1, Integer::sum);
                    examples.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(reason);
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                stats.merge("其他原因", 1, Integer::sum);
                examples.computeIfAbsent("其他原因", k -> new ArrayList<>()).add(reason);
            }
        }

        // 构建响应
        List<String> colors = List.of("#667eea", "#764ba2", "#909399", "#c0c4cc", "#e6a23c");
        List<LateReasonAnalyzeResponse.CategoryStat> categories = new ArrayList<>();

        int colorIdx = 0;
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            categories.add(new LateReasonAnalyzeResponse.CategoryStat(
                    entry.getKey(),
                    entry.getValue(),
                    colors.get(colorIdx % colors.size()),
                    examples.getOrDefault(entry.getKey(), List.of()).stream()
                            .limit(2)
                            .collect(Collectors.toList())
            ));
            colorIdx++;
        }

        return new LateReasonAnalyzeResponse(
                categories,
                "⚠️ 智能分析暂时不可用，已使用基础规则分类。建议检查网络连接或稍后重试。"
        );
    }
}