# NutriBot API — Projeto Diamante (Java Advanced)

## Sobre o projeto

O **NutriBot API** é uma API desenvolvida em Java para um assistente de nutrição com recursos de Inteligência Artificial. O projeto utiliza o Spring AI para integrar um modelo de linguagem local, executar ferramentas e recuperar informações relevantes por meio de RAG (*Retrieval-Augmented Generation*).

A aplicação também permite registrar e consultar refeições, utilizando o banco de dados H2.

## Equipe

- **Luna de Carvalho Guimarães** — RM: **562290**



## Tecnologias utilizadas

- **Java** — linguagem de programação.
- **Spring Boot** — framework para desenvolvimento da API.
- **Spring AI** — integração com modelos de Inteligência Artificial.
- **Ollama** — execução local dos modelos de IA.
- **Llama 3.2** — modelo de linguagem utilizado no chat.
- **nomic-embed-text** — modelo utilizado para gerar embeddings.
- **SimpleVectorStore** — armazenamento vetorial utilizado no RAG.
- **H2 Database** — banco de dados utilizado para armazenar informações.
- **REST API** — comunicação por meio de requisições HTTP.

## Modelos de Inteligência Artificial

| Componente | Tecnologia utilizada |
|---|---|
| Provedor de IA | Ollama, com API compatível com OpenAI |
| Modelo de chat | `llama3.2` |
| Modelo de embeddings | `nomic-embed-text` |
| Recuperação de informações | RAG |
| Armazenamento vetorial | `SimpleVectorStore` |

## Funcionalidades

- **Chat com IA:** interação com o assistente por meio de linguagem natural.
- **Tool Calling:** utilização de ferramentas para executar operações na aplicação.
- **Memória:** suporte ao gerenciamento de contexto das conversas, conforme implementado no projeto.
- **RAG:** recuperação de informações relevantes para auxiliar na geração das respostas.
- **Structured Output:** utilização de respostas estruturadas para facilitar o processamento dos dados.
- **Registro de refeições:** armazenamento de refeições e consulta dos registros por meio da API.

## Configuração do ambiente

### Pré-requisitos

- Java na versão exigida pela configuração do projeto.
- Ollama instalado e em execução.
- Modelos de IA baixados no Ollama.
- IDE compatível com projetos Spring Boot, como IntelliJ IDEA.

### 1. Baixar os modelos

Execute os comandos no terminal:

```bash
ollama pull llama3.2
ollama pull nomic-embed-text
```

Verifique se o serviço do Ollama está em execução antes de iniciar a aplicação.

### 2. Configurar as variáveis de ambiente

A aplicação utiliza variáveis de ambiente para configurar a integração com o Ollama, evitando deixar configurações sensíveis diretamente no código-fonte.

Configure as seguintes variáveis de ambiente na configuração de execução da aplicação:

```bash
OPENAI_API_KEY=ollama
OPENAI_BASE_URL=http://localhost:11434/v1
OPENAI_MODEL=llama3.2
OPENAI_EMBEDDING_MODEL=nomic-embed-text
OPENAI_COMPLETIONS_PATH=/chat/completions
OPENAI_EMBEDDINGS_PATH=/embeddings
```

**Observação:** `ollama` é utilizado como valor de configuração para a API local compatível com OpenAI, não como uma chave real de acesso. Os nomes das variáveis devem corresponder aos utilizados pela configuração da aplicação.

### 3. Executar a aplicação

1. Abra o projeto na IDE.
2. Verifique se o Ollama está em execução e se os modelos necessários foram baixados.
3. Configure as variáveis de ambiente.
4. Execute a classe principal do Spring Boot.
5. Aguarde a inicialização da API e utilize os endpoints implementados para testar as funcionalidades.

## Exemplo de consulta

A aplicação disponibiliza a consulta dos registros de refeições pelo endpoint:

```http
GET /meals
```

A resposta apresenta os registros de refeições armazenados, conforme a estrutura de dados definida na aplicação.

## Segurança e boas práticas

- Não incluir chaves reais de API diretamente no código-fonte.
- Utilizar variáveis de ambiente para configurar integrações.
- Não versionar credenciais ou arquivos que contenham segredos.
- Manter as configurações necessárias para a execução documentadas neste README.

## Objetivo acadêmico

Este projeto foi desenvolvido no contexto da disciplina **Java Advanced**, com o objetivo de aplicar conceitos de desenvolvimento de APIs, integração com Inteligência Artificial, Tool Calling, memória, RAG e processamento de respostas estruturadas utilizando Java e Spring AI.