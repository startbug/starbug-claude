package com.ggs.starbugclaude.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.RecursiveSummarizationCompactionStrategy;
import org.springframework.ai.session.compaction.SlidingWindowCompactionStrategy;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(chatMemoryRepository).build();
    }

    @Bean
    public SessionMemoryAdvisor sessionMemoryAdvisor() {
        SessionService service = DefaultSessionService.builder()
                .sessionRepository(InMemorySessionRepository.builder().build())
                .build();
        return SessionMemoryAdvisor.builder(service)
                .defaultUserId("xiaohui")
                // 触发器 超过1轮对话 对花钱：
                // 你好 -- 获取event 轮次0>1) --> LLM --> 你好，有什么能帮助的吗？  turn:0+1不会触发
                // 我叫什么 -- (获取event 轮次1>1) LLM 不会触发
                // 我叫什么 -- (获取event 轮次2>1) LLM 触发压缩
                .compactionTrigger(new TurnCountTrigger(1))
                // 压缩策略
                // 硬截断
                // SlidingWindowCompactionStrategy  按消息数
                // TurnWindowCompactionStrategy  按回合数
                // TokenCountCompactionStrategy  按Token大小

//                采用LLM提取摘要
//                RecursiveSummarizationCompactionStrategy

                .compactionStrategy(SlidingWindowCompactionStrategy.builder().maxEvents(1).build())
                .build();
    }

    @Bean
    public SessionMemoryAdvisor sessionMemorySummarizationAdvisor(DeepSeekChatModel deepSeekChatModel) {
        SessionService service = DefaultSessionService.builder()
                .sessionRepository(InMemorySessionRepository.builder().build())
                .build();
        return SessionMemoryAdvisor.builder(service)
                .defaultUserId("xiaohui")
                // 触发器 超过1轮对话 对花钱：
                // 你好 -- 获取event 轮次0>1) --> LLM --> 你好，有什么能帮助的吗？  turn:0+1不会触发
                // 我叫什么 -- (获取event 轮次1>1) LLM 不会触发
                // 我叫什么 -- (获取event 轮次2>1) LLM 触发压缩
                .compactionTrigger(new TurnCountTrigger(1))
                .compactionStrategy(RecursiveSummarizationCompactionStrategy.builder(ChatClient.builder(deepSeekChatModel).build())
                        // maxEventsToKeep:保留不压缩的条数
                        // 作用：确保LLM在处理当前最新对话时，能直接读取最近10次最完成、最精确的交互细节。早于这10个事件的历史消息内容则会被打包归档摘要当中。
                        .maxEventsToKeep(2)
                        // overlapSize:除压缩外需要提供给LLM的额外条数
                        // 作用：提供平滑过度与语义缓冲。如果不设置重叠（即硬切断），摘要与最新活动窗口交界处的上下文容易产生断层；加上重叠事件，能让LLM在总结时更好地承上启下，避免临界处的关键信息遗漏
                        .overlapSize(0)
                        .build())
                .build();
    }

}
