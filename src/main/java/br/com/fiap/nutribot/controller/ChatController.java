package br.com.fiap.nutribot.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.nutribot.dto.ChatRequest;
import br.com.fiap.nutribot.dto.ChatResponse;
import br.com.fiap.nutribot.dto.ErrorResponse;
import br.com.fiap.nutribot.dto.SummaryResponse;
import br.com.fiap.nutribot.tool.NutriBotTools;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chatClient;
    private final NutriBotTools nutriBotTools;

    public ChatController(ChatClient chatClient, NutriBotTools nutriBotTools) {
        this.chatClient = chatClient;
        this.nutriBotTools = nutriBotTools;
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()
                || request.conversationId() == null || request.conversationId().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("conversationId e message são obrigatórios"));
        }

        try {
            String msgLower = request.message().toLowerCase();

            boolean isRegistro = msgLower.contains("comi") || msgLower.contains("registr")
                    || msgLower.contains("anot") || msgLower.contains("bebi")
                    || msgLower.contains("tomei") || msgLower.contains("jantei")
                    || msgLower.contains("almocei");

            boolean isResumo = msgLower.contains("ainda posso") || msgLower.contains("resumo")
                    || msgLower.contains("restante") || msgLower.contains("meta")
                    || msgLower.contains("consumi hoje") || msgLower.contains("comer hoje");

            boolean isBusca = !isRegistro && !isResumo && (
                    msgLower.contains("quantas caloria") || msgLower.contains("calorias tem")
                            || msgLower.contains("100 g") || msgLower.contains("100g")
                            || msgLower.contains("tabela") || msgLower.contains("tapioca")
                            || msgLower.contains("proteína") || msgLower.contains("carboidrato"));

            var promptSpec = chatClient.prompt()
                    .user(request.message())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.conversationId()));

            if (isRegistro) {
                promptSpec = promptSpec.tools(new OnlyRegisterTool(nutriBotTools));
            } else if (isResumo) {
                promptSpec = promptSpec.tools(new OnlySummaryTool(nutriBotTools));
            } else if (isBusca) {
                promptSpec = promptSpec.tools(new OnlySearchTool(nutriBotTools));
            }

            String answer = promptSpec.call().content();

            return ResponseEntity.ok(new ChatResponse(answer));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    static class OnlySearchTool {
        private final NutriBotTools delegate;
        OnlySearchTool(NutriBotTools delegate) { this.delegate = delegate; }

        @Tool(description = "Busca as informações nutricionais e calóricas (por 100g) de um alimento pelo nome na tabela da clínica.")
        public Object buscarAlimento(@ToolParam(description = "Nome do alimento no singular, ex: tapioca, ovo") String nomeAlimento) {
            return delegate.buscarAlimento(nomeAlimento);
        }
    }

    static class OnlySummaryTool {
        private final NutriBotTools delegate;
        OnlySummaryTool(NutriBotTools delegate) { this.delegate = delegate; }

        @Tool(description = "Consulta o resumo do dia de hoje do paciente: calorias consumidas, meta diária e quantas calorias ainda pode comer hoje.")
        public SummaryResponse resumoDoDia(@ToolParam(description = "Envie sempre 'hoje'") String data) {
            return delegate.resumoDoDia(data);
        }
    }

    static class OnlyRegisterTool {
        private final NutriBotTools delegate;
        OnlyRegisterTool(NutriBotTools delegate) { this.delegate = delegate; }

        @Tool(description = "Registra uma refeição consumida pelo paciente no diário alimentar de hoje.")
        public String registrarRefeicao(
                @ToolParam(description = "Nome do alimento no singular, ex: ovo, tapioca") String nomeAlimento,
                @ToolParam(description = "Quantidade em gramas, ex: 100.0") Double gramas,
                @ToolParam(description = "Refeição: BREAKFAST, LUNCH, DINNER ou SNACK") String tipoRefeicao) {
            return delegate.registrarRefeicao(nomeAlimento, gramas, tipoRefeicao);
        }
    }
}