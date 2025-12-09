🍕 Chatbot de Pizzaria com IA (Spring Boot + Gemini + WhatsApp)

Este projeto é um Assistente Virtual para Pizzaria altamente capaz, desenvolvido em Java. Diferente de bots tradicionais baseados apenas em regras rígidas (if/else), este projeto utiliza Inteligência Artificial (Google Gemini) para entender a intenção do usuário e Redis para gerenciar o contexto da conversa (State Machine), proporcionando uma experiência fluida e natural.

🚀 Funcionalidades Principais

🧠 Inteligência Artificial (NLU): Utiliza a API do Google Gemini 1.5 Flash para classificar as mensagens do usuário em intenções (ex: SAUDACAO, PEDIR_PIZZA, DUVIDA_CARDAPIO), permitindo que o bot entenda variações de linguagem natural.

💾 Gestão de Estado e Contexto: Utiliza Redis com TTL (Time-to-Live) para manter a "memória" da conversa. O bot sabe se o usuário está escolhendo um sabor, um tamanho ou pagando, evitando confusões lógicas.

📱 Integração WhatsApp: Conectado via Evolution API (rodando em Docker) para envio e recebimento de mensagens em tempo real.

🚚 Cálculo de Frete Inteligente: Integração com a API ViaCEP para validar o endereço e lógica interna para calcular o frete baseado na cidade (ex: Jandira, Barueri, Carapicuíba).

📄 Envio de Mídia: Capacidade de enviar o cardápio em formato PDF automaticamente.

🛒 Carrinho de Compras: Armazena o pedido (Sabor, Tamanho, Bebida, Frete) temporariamente no Redis até a finalização.

🛡️ Anti-Loop: Proteção contra respostas recursivas (o bot não responde a si mesmo).

🛠️ Arquitetura e Tecnologias

O projeto segue uma arquitetura de microsserviços simplificada:

Bot Core (Este Repositório): Aplicação Java Spring Boot que contém a regra de negócio, serviços de IA (LlmService), serviços de Estado (RedisService) e Repositórios JPA.

Infraestrutura (Docker Compose):

Evolution API: Gateway para o WhatsApp.

PostgreSQL: Banco de dados principal para persistência da API e do Bot.

Redis: Banco em memória para gestão de sessão do usuário.

📋 Pré-requisitos

Java 17 ou superior

Maven

Docker e Docker Compose

Conta no Google AI Studio (para a chave da API Gemini)

Uma instância da Evolution API configurada

🔧 Configuração e Instalação

Clone o repositório:

git clone [https://github.com/SEU-USUARIO/NOME-DO-REPO.git](https://github.com/SEU-USUARIO/NOME-DO-REPO.git)


Suba a infraestrutura Docker:
Certifique-se de que o arquivo docker-compose.yml está configurado e execute:

docker-compose up -d


Configure as Variáveis de Ambiente:
Edite o arquivo src/main/resources/application.properties com suas credenciais:

# Evolution API
evolution.api.url=http://localhost:8080
evolution.api.key=SUA_CHAVE_EVOLUTION
evolution.instance.name=NOME_DA_SUA_INSTANCIA

# Google Gemini AI
gemini.api.key=SUA_CHAVE_GEMINI_AI

# Banco de Dados (Bot)
spring.datasource.url=jdbc:postgresql://localhost:5433/bot_db
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379


Execute a aplicação:

mvn spring-boot:run


🧠 Exemplo de Fluxo (State Machine)

Usuário: "Quero pedir uma pizza"

IA: Identifica intenção INICIAR_PEDIDO.

Bot: "Qual o sabor?"

Redis: Define estado AGUARDANDO_ESCOLHA_SABOR.

Usuário: "Calabresa"

IA: Identifica sabor.

Bot: Valida no Banco de Dados (PizzaRepository), retorna preço e pergunta o tamanho.

Redis: Salva sabor no carrinho e muda estado para AGUARDANDO_TAMANHO.

Usuário: "Grande"

... (Fluxo continua até o pagamento e endereço) ...