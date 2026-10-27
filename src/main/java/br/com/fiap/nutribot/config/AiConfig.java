package br.com.fiap.nutribot.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import br.com.fiap.nutribot.tool.NutriBotTools;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    private static final String PERSONA_SYSTEM_PROMPT = """
            Você é a NutriBot, assistente virtual da Clínica Equilíbrio.
            - Responda sempre em português, com tom acolhedor, respostas curtas e sem julgamento.
            - Recuse educadamente assuntos que não sejam sobre nutrição ou sobre a Clínica Equilíbrio.
            - Você apoia o paciente entre as consultas, mas nunca substitui o nutricionista.
            - Não prescreva dietas, medicamentos nem suplementos; não faça diagnósticos clínicos.
            - Nunca use termos como "dieta da fome", "proibido" ou "dia do lixo".
            - Use as tools registradas apenas para buscar alimentos na tabela, consultar o resumo do dia ou registrar refeições consumidas.
            - Para saudações, conversas gerais ou perguntas sobre mensagens anteriores (como saber o nome do paciente), responda diretamente em texto natural, sem chamar nenhuma tool.
            """;

    private static final String RAG_PROMPT_TEMPLATE = """
            Mensagem do usuário:
            {query}

            ---------------------
            Documentos de referência da Clínica Equilíbrio (use se a pergunta for sobre regras da clínica, horários, hidratação/água ou guia alimentar; ignore se for apenas saudação, nome do usuário ou assunto fora de nutrição):
            {question_answer_context}
            ---------------------
            """;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public ApplicationRunner initRAG(VectorStore vectorStore, @Value("classpath*:docs/*.*") Resource[] docs) {
        return args -> {
            for (Resource doc : docs) {
                if (doc.exists()) {
                    TextReader reader = new TextReader(doc);
                    TokenTextSplitter splitter = TokenTextSplitter.builder().build();
                    List<Document> documents = splitter.split(reader.read());
                    vectorStore.add(documents);
                }
            }
            log.info("Documentos do RAG carregados com sucesso!");
        };
    }

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            VectorStore vectorStore,
            NutriBotTools nutriBotTools) {

        QuestionAnswerAdvisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(2)
                        .similarityThreshold(0.5)
                        .build())
                .promptTemplate(new PromptTemplate(RAG_PROMPT_TEMPLATE))
                .build();

        return builder
                .defaultSystem(PERSONA_SYSTEM_PROMPT)
                .defaultTools(nutriBotTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        ragAdvisor
                )
                .build();
    }
}