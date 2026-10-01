package com.ggs.starbugclaude;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.util.Scanner;

@SpringBootApplication
public class StarbugClaudeApplication {

    public static void main(String[] args) {
        SpringApplication.run(StarbugClaudeApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(DeepSeekChatModel deepSeekChatModel,
                                        ChatClient.Builder chatClientBuilder,
                                        ChatMemory chatMemory,
//                                        SessionMemoryAdvisor sessionMemoryAdvisor,
                                        SessionMemoryAdvisor sessionMemorySummarizationAdvisor) {
        return args -> {
            ChatClient chatClient = ChatClient.builder(deepSeekChatModel)
                    .defaultAdvisors(
                            // 这个memory不带压缩功能
//                            MessageChatMemoryAdvisor.builder(chatMemory).build(),
                            // 这个memory带压缩功能,sessionMemoryAdvisor硬切断
//                            sessionMemoryAdvisor,
                            // 模型抽取摘要
                            sessionMemorySummarizationAdvisor,
                            SimpleLoggerAdvisor.builder().build()
                    )
                    .build();

            System.out.println("\n我是 starbug:.\n");
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\n> 你：");
                    String userMessage = scanner.nextLine();
//                    String content = chatClient.prompt()
//                            .user(userMessage)
//                            .call()
//                            .content();
                    Flux<String> response = chatClient.prompt()
                            .user(userMessage)
                            .advisors(a -> a.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, "abc"))
                            .stream()
                            .content();
                    System.out.print("\n> AI：");
                    response.doOnNext(System.out::print)
                            .blockLast();
//                    System.out.println("\n> AI：" + flux);
                }
            }
        };
    }

}
