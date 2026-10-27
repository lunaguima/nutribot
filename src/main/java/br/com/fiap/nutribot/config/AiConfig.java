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
            Regras de comportamento:
            1. Responda SEMPRE em português, em texto natural (NUNCA responda com blocos JSON ou {"name":...}).
            2. Se o usuário disser o nome dele ou perguntar qual é o nome dele, responda diretamente usando a memória da conversa. NÃO chame nenhuma tool para nomes ou saudações.
            3. Se a pergunta for sobre programação, esportes, política ou qualquer assunto fora de nutrição e da Clínica Equilíbrio, recuse educadamente dizendo que só pode ajudar com nutrição.
            4. Nunca prescreva dietas clínicas nem dê diagnósticos, e lembre que você não substitui o nutricionista.
            5. Use as ferramentas (tools) APENAS nestes 3 casos:
               - Buscar calorias/nutrientes de um alimento na tabela;
               - Consultar o resumo calórico do dia;
               - Registrar uma refeição que o paciente comeu.
            """;

    private static final String RAG_PROMPT_TEMPLATE = """
            Mensagem do usuário:
            {query}

            ---------------------
            Documentos de referência da Clínica Equilíbrio (use APENAS se a mensagem do usuário for uma dúvida sobre regras da clínica, horários, hidratação/água ou orientações nutricionais; ignore completamente estes documentos se o usuário estiver apenas conversando, dizendo o nome ou falando de assuntos fora de nutrição):
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
            try {
                for (Resource doc : docs) {
                    if (doc.exists()) {
                        TextReader reader = new TextReader(doc);
                        TokenTextSplitter splitter = TokenTextSplitter.builder().build();
                        List<Document> documents = splitter.split(reader.read());
                        vectorStore.add(documents);
                    }
                }
                log.info("Documentos do RAG carregados com sucesso!");
            } catch (Exception e) {
                log.error("Erro ao carregar embeddings no RAG: {}", e.getMessage());
            }
        };
    }

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            VectorStore vectorStore) {

        QuestionAnswerAdvisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(2)
                        .similarityThreshold(0.5)
                        .build())
                .promptTemplate(new PromptTemplate(RAG_PROMPT_TEMPLATE))
                .build();

        return builder
                .defaultSystem(PERSONA_SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        ragAdvisor
                )
                .build();
    }
}