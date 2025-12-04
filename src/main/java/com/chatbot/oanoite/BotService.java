package com.chatbot.oanoite;

import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import java.util.Map; // <-- IMPORT NOVO NECESSÁRIO PARA LISTAR
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BotService {

    private static final Logger log = LoggerFactory.getLogger(BotService.class);

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private LlmService llmService; 
    
    @Autowired
    private RedisService redisService; 

    @Autowired
    private PizzaRepository pizzaRepository; 

    @Autowired
    private BebidaRepository bebidaRepository; 
    
    @Autowired
    private ConsultaEnd consultaEnd; 

    @Value("${evolution.api.url}")
    private String apiUrl;
    @Value("${evolution.instance.name}")
    private String instanceName;
    @Value("${evolution.api.key}")
    private String apiKey;

    
    public void processarMensagem(Map<String, Object> payload) {

        try {
            String evento = (String) payload.get("event");
            if (!"messages.upsert".equals(evento)) {
                log.info("Ignorando evento: {}", evento);
                return;
            }

            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            Map<String, Object> key = (Map<String, Object>) data.get("key");
            Map<String, Object> message = (Map<String, Object>) data.get("message");

            if (data == null || key == null || message == null) {
                return;
            }

            // --- 2. FILTRAGEM ANTI-LOOP ---
            // Descomente para produção!
            // boolean fromMe = (boolean) key.get("fromMe");
            // if (fromMe) {
            //     log.info("Ignorando mensagem própria (fromMe=true)");
            //     return;
            // }

            String remetente = (String) key.get("remoteJid"); 
            String textoMensagem = (String) message.get("conversation");
            if (textoMensagem == null || textoMensagem.isEmpty()) {
                Map<String, Object> extendedText = (Map<String, Object>) message.get("extendedTextMessage");
                if (extendedText != null) {
                    textoMensagem = (String) extendedText.get("text");
                }
            }


            // --- 4. LÓGICA PRINCIPAL ---
            if (remetente != null && textoMensagem != null) {

                log.info("MENSAGEM RECEBIDA: De [{}], Texto [{}]", remetente, textoMensagem);

                String estadoAtual = redisService.getEstado(remetente);
                
                // --- BLOQUEIO PARA ATENDIMENTO HUMANO ---
                if ("EM_ATENDIMENTO_HUMANO".equals(estadoAtual)) {
                    if (textoMensagem.equalsIgnoreCase("voltar") || textoMensagem.equalsIgnoreCase("menu")) {
                        log.info("Usuário saiu do atendimento humano.");
                        redisService.limparPedido(remetente);
                    } else {
                        log.info("Usuário em atendimento humano. Bot silenciado.");
                        return; 
                    }
                }

                // --- Lógica de Timeout ---
                if (estadoAtual == null) {
                    String saborSalvo = redisService.getValor(remetente, "pedido_sabor");
                    if (saborSalvo != null) {
                        log.info("Lógica: Pedido expirou por inatividade para {}", remetente);
                        enviarResposta(remetente, "Seu atendimento expirou por inatividade. ⌛\n\nPor favor, digite 'oi' ou 'Menu' para começar de novo.");
                        redisService.limparPedido(remetente);
                        return; 
                    }
                }
                
                String intentId = null;
                String textoLimpo = textoMensagem.trim();
                
                if (estadoAtual == null || estadoAtual.equals("padrao") || estadoAtual.equals("VIU_CARDAPIO") || estadoAtual.equals("VIU_REGIAO")) {
                    if (textoLimpo.equals("1")) { intentId = "VER_LOCAIS"; } 
                    else if (textoLimpo.equals("2")) { intentId = "VER_CARDAPIO"; } 
                    else if (textoLimpo.equals("3")) { intentId = "INICIAR_PEDIDO"; } 
                    else if (textoLimpo.equals("4")) { intentId = "FALAR_ATENDENTE"; }
                }
                
                if (intentId == null) {
                    intentId = llmService.classificarIntencao(textoMensagem, estadoAtual);
                }

                // --- LÓGICA DA PIZZARIA ---

                if (intentId.equals("SAUDACAO")) {
                    log.info("Lógica: Respondendo 'SAUDACAO' para {}", remetente);
                    String saudacaoMsg = "Olá! Sou a Aline, assistente virtual da Pizzaria OANoite. 🍕\n" +
                                       "\n" +
                                       "Digite a opção desejada\n" +
                                       "\n" +
                                       "1 - Estamos em sua Região?\n" +
                                       "2 - Cardápio\n" +
                                       "3 - Pedir\n" +
                                       "4 - Falar com atendente";
                    enviarResposta(remetente, saudacaoMsg);
                    redisService.limparPedido(remetente); 

                } else if (intentId.equals("VER_CARDAPIO")) {
                    log.info("Lógica: Enviando PDF do cardápio para {}", remetente);
                    String urlDoCardapio = "https://files.catbox.moe/uld6vk.pdf"; 
                    enviarResposta(remetente,"Aguarde só um momento seu cardápio está carregando🍕🍕");
                    String legenda = "Aqui está nosso cardápio completo! 🍕\n\nQuando estiver pronto, é só digitar 'Pedir'.";
                    enviarRespostaPdf(remetente, legenda, urlDoCardapio, "CardapioPizzaria.pdf");
                    redisService.setEstado(remetente, "VIU_CARDAPIO");

                } else if (intentId.equals("VER_LOCAIS")) {
                    log.info("Lógica: Verificando região. Pedindo CEP.");
                    enviarResposta(remetente, "Para verificar se atendemos sua região, por favor, digite seu CEP (apenas números).");
                    redisService.setEstado(remetente, "AGUARDANDO_CEP_VERIFICACAO");
                    
                } else if (intentId.equals("ENVIOU_CEP_VERIFICACAO")) {
                    log.info("Lógica: Usuário enviou CEP para verificação.");
                    EnderecoResponse endereco = consultaEnd.consultarCep(textoMensagem); 
                      
                    if (endereco == null || endereco.getLocalidade() == null) {
                        enviarResposta(remetente, "Desculpe, não consegui encontrar esse CEP. Por favor, digite novamente.");
                        redisService.setEstado(remetente, "AGUARDANDO_CEP_VERIFICACAO");
                        return;
                    }

                    String cidade = removeAcentos(endereco.getLocalidade().toLowerCase());
                    double frete = 0.0;
                    String mensagemFrete = "";

                    if (cidade.contains("jandira")) {
                        frete = 8.00;
                        mensagemFrete = String.format("Boa notícia! Atendemos em Jandira. O frete é R$ %.2f.", frete);
                    } else if (cidade.contains("barueri")) {
                        frete = 6.00;
                        mensagemFrete = String.format("Boa notícia! Atendemos em Barueri. O frete é R$ %.2f.", frete);
                    } else if (cidade.contains("carapicuiba")) {
                        frete = 3.00;
                        mensagemFrete = String.format("Boa notícia! Atendemos em Carapicuíba. O frete é R$ %.2f.", frete);
                    } else {
                        enviarResposta(remetente, String.format("Que pena! No momento, ainda não atendemos na região de %s. Agradecemos o contato!", endereco.getLocalidade()));
                        redisService.limparPedido(remetente); 
                        return;
                    }

                    redisService.setValor(remetente, "pedido_frete", String.valueOf(frete));
                    enviarResposta(remetente, mensagemFrete + "\n\nDigite a opção desejada:\n2 - Cardápio\n3 - Pedir\n4 - Falar com atendente");
                    redisService.setEstado(remetente, "VIU_REGIAO"); 
                
                } else if (intentId.equals("FALAR_ATENDENTE")) {
                    log.info("Lógica: Transferindo para atendente {}", remetente);
                    enviarResposta(remetente, "Ok, um atendente humano já vai falar com você. Para voltar ao bot, digite 'Menu'.");
                    redisService.setEstado(remetente, "EM_ATENDIMENTO_HUMANO"); 
                
                } else if (intentId.equals("INICIAR_PEDIDO")) {
                    log.info("Lógica: Iniciando pedido para {}", remetente);
                    redisService.limparPedido(remetente); 
                    enviarResposta(remetente, "Vamos lá! Qual o sabor da pizza?");
                    redisService.setEstado(remetente, "AGUARDANDO_ESCOLHA_SABOR"); 

                } else if (intentId.equals("ESCOLHEU_SABOR")) {
                    log.info("Lógica: Intenção 'ESCOLHEU_SABOR' recebida.");
                    String saborDigitado = textoMensagem.trim();
                    Optional<Pizza> pizzaOptional = pizzaRepository.findByNomeContainingIgnoreCase(saborDigitado);
                    
                    if (pizzaOptional.isPresent()) {
                        Pizza pizzaEscolhida = pizzaOptional.get();
                        redisService.setValor(remetente, "pedido_sabor", pizzaEscolhida.getNome());
                        redisService.setValor(remetente, "pedido_preco_final", String.valueOf(pizzaEscolhida.getPreco())); 
                        String resposta = String.format("Ótima escolha! %s (Preço base: R$ %.2f).\nQual o tamanho? (Pequena, Média ou Grande)", pizzaEscolhida.getNome(), pizzaEscolhida.getPreco());
                        enviarResposta(remetente, resposta);
                        redisService.setEstado(remetente, "AGUARDANDO_TAMANHO");
                    } else {
                        enviarResposta(remetente, "Desculpe, não temos o sabor '" + saborDigitado + "'. Por favor, escolha um sabor do nosso cardápio (aquele do PDF).");
                        redisService.setEstado(remetente, "AGUARDANDO_ESCOLHA_SABOR");
                    }

                } else if (intentId.equals("ESCOLHEU_TAMANHO")) {
                    log.info("Lógica: Usuário escolheu tamanho.");
                    String tamanhoEscolhido = textoMensagem.trim().toLowerCase();
                    String precoBaseStr = redisService.getValor(remetente, "pedido_preco_final");
                    
                    if (precoBaseStr == null) {
                         enviarResposta(remetente, "Ops! Vamos começar de novo. Digite 'Pedir' para iniciar.");
                         redisService.limparPedido(remetente);
                         return;
                    }
                    
                    double precoBase = Double.parseDouble(precoBaseStr);
                    double precoFinal = precoBase;
                    String tamanhoFormatado = "Média";

                    if (tamanhoEscolhido.equals("grande")) {
                        precoFinal = precoBase * 1.30; 
                        tamanhoFormatado = "Grande";
                    } else if (tamanhoEscolhido.equals("pequena")) {
                        precoFinal = precoBase * 0.80; 
                        tamanhoFormatado = "Pequena";
                    } else if (!tamanhoEscolhido.equals("média")) {
                        enviarResposta(remetente, "Desculpe, não entendi o tamanho. Por favor, diga 'Pequena', 'Média' ou 'Grande'.");
                        redisService.setEstado(remetente, "AGUARDANDO_TAMANHO"); 
                        return;
                    }

                    redisService.setValor(remetente, "pedido_preco_final", String.valueOf(precoFinal));
                    redisService.setValor(remetente, "pedido_tamanho", tamanhoFormatado);

                    enviarResposta(remetente, String.format("Anotado! Tamanho %s. Seu total está em R$ %.2f.\nGostaria de adicionar uma bebida? (Ex: Coca-cola, Guaraná, ou 'não')", tamanhoFormatado, precoFinal));
                    redisService.setEstado(remetente, "AGUARDANDO_BEBIDA"); 

                } else if (intentId.equals("ESCOLHEU_BEBIDA")) {
                    log.info("Lógica: Intenção 'ESCOLHEU_BEBIDA' recebida.");
                    String bebidaDigitada = textoMensagem.trim().toLowerCase();
                    double precoAtual = Double.parseDouble(redisService.getValor(remetente, "pedido_preco_final"));

                    if (bebidaDigitada.equals("não") || bebidaDigitada.equals("nao") || bebidaDigitada.equals("sem bebida")) {
                        redisService.setValor(remetente, "pedido_bebida", "Nenhuma");
                        enviarResposta(remetente, String.format("Entendido. Sem bebida.\nSeu total é R$ %.2f.\nPara fechar, por favor, digite o seu CEP.", precoAtual));
                        redisService.setEstado(remetente, "AGUARDANDO_CEP");
                    
                    } else {
                        Optional<Bebida> bebidaOptional = bebidaRepository.findByNomeContainingIgnoreCase(bebidaDigitada);
                        if (bebidaOptional.isPresent()) {
                            Bebida bebidaEscolhida = bebidaOptional.get();
                            double precoFinal = precoAtual + bebidaEscolhida.getPreco();
                            redisService.setValor(remetente, "pedido_bebida", bebidaEscolhida.getNome());
                            redisService.setValor(remetente, "pedido_preco_final", String.valueOf(precoFinal));
                            enviarResposta(remetente, String.format("Anotado: %s (R$ %.2f).\nSeu novo total é R$ %.2f.\nAgora, por favor, digite o seu CEP.", bebidaEscolhida.getNome(), bebidaEscolhida.getPreco(), precoFinal));
                            redisService.setEstado(remetente, "AGUARDANDO_CEP");
                        } else {
                            log.warn("Bebida '{}' não encontrada no banco.", bebidaDigitada);
                            
                            // --- CRUD: READ (Listar as bebidas do banco para ajudar o usuário) ---
                            List<Bebida> todasBebidas = bebidaRepository.findAll();
                            StringBuilder opcoes = new StringBuilder();
                            for (Bebida b : todasBebidas) {
                                opcoes.append(String.format("- %s (R$ %.2f)\n", b.getNome(), b.getPreco()));
                            }
                            
                            enviarResposta(remetente, "Desculpe, não temos '" + bebidaDigitada + "'.\n\nNossas bebidas disponíveis são:\n" + opcoes.toString() + "\n(Ou digite 'não' se não quiser bebida.)");
                            
                            redisService.setEstado(remetente, "AGUARDANDO_BEBIDA");
                        }
                    }
                
                } else if (intentId.equals("ENVIOU_CEP")) {
                    log.info("Lógica: Usuário enviou CEP do pedido.");
                    EnderecoResponse endereco = consultaEnd.consultarCep(textoMensagem);
                    
                    if (endereco == null || endereco.getLocalidade() == null) {
                        enviarResposta(remetente, "Desculpe, não consegui encontrar esse CEP. Por favor, digite novamente.");
                        redisService.setEstado(remetente, "AGUARDANDO_CEP"); 
                        return; 
                    }

                    String cidade = removeAcentos(endereco.getLocalidade().toLowerCase());
                    double frete = 0.0;

                    if (cidade.contains("jandira")) { frete = 8.00; } 
                    else if (cidade.contains("barueri")) { frete = 6.00; } 
                    else if (cidade.contains("carapicuiba")) { frete = 3.00; } 
                    else {
                        enviarResposta(remetente, String.format("Que pena! No momento, ainda não atendemos na região de %s. Seu pedido foi cancelado.", endereco.getLocalidade()));
                        redisService.limparPedido(remetente); 
                        return; 
                    }
                    
                    redisService.setValor(remetente, "pedido_rua", endereco.getLogradouro());
                    redisService.setValor(remetente, "pedido_bairro", endereco.getBairro());
                    redisService.setValor(remetente, "pedido_frete", String.valueOf(frete));
                    
                    enviarResposta(remetente, String.format("Endereço: %s, %s. (Frete: R$ %.2f)\nPor favor, digite o número da casa:", endereco.getLogradouro(), endereco.getBairro(), frete));
                    redisService.setEstado(remetente, "AGUARDANDO_NUMERO_CASA");

                } else if (intentId.equals("ENVIOU_NUMERO_CASA")) {
                    log.info("Lógica: Usuário enviou número da casa.");
                    redisService.setValor(remetente, "pedido_numero", textoMensagem.trim());
                    enviarResposta(remetente, "Anotado. Você tem algum complemento? (Ex: Apto 101, ou digite 'não')");
                    redisService.setEstado(remetente, "AGUARDANDO_COMPLEMENTO");

                } else if (intentId.equals("ENVIOU_COMPLEMENTO")) {
                    log.info("Lógica: Usuário enviou complemento.");
                    String complemento = textoMensagem.trim().toLowerCase();
                    if (complemento.equals("não") || complemento.equals("nao")) {
                        redisService.setValor(remetente, "pedido_complemento", "Nenhum");
                    } else {
                        redisService.setValor(remetente, "pedido_complemento", textoMensagem.trim());
                    }
                    enviarResposta(remetente, "Endereço completo! Para fechar, qual a forma de pagamento? (Pix, Cartão ou Dinheiro)");
                    redisService.setEstado(remetente, "AGUARDANDO_PAGAMENTO");

                } else if (intentId.equals("ESCOLHEU_PAGAMENTO")) {
                    log.info("Lógica: Usuário escolheu pagamento. Finalizando.");
                    
                    String sabor = redisService.getValor(remetente, "pedido_sabor");
                    String tamanho = redisService.getValor(remetente, "pedido_tamanho");
                    String bebida = redisService.getValor(remetente, "pedido_bebida");
                    String precoFinalStr = redisService.getValor(remetente, "pedido_preco_final");
                    String pagamento = textoMensagem.trim();
                    
                    String rua = redisService.getValor(remetente, "pedido_rua");
                    String numero = redisService.getValor(remetente, "pedido_numero");
                    String bairro = redisService.getValor(remetente, "pedido_bairro");
                    String comp = redisService.getValor(remetente, "pedido_complemento");
                    
                    double frete = 0.0;
                    String freteStr = redisService.getValor(remetente, "pedido_frete");
                    if (freteStr != null) { frete = Double.parseDouble(freteStr); }
                    
                    double totalFinal = Double.parseDouble(precoFinalStr) + frete;
                    
                    String resumo = "Pedido Confirmado! ✅\n\nResumo do Pedido:\n" +
                                    String.format("- Pizza: %s (%s)\n", sabor, tamanho) +
                                    String.format("- Bebida: %s\n", bebida) +
                                    String.format("- Pagamento: %s\n", pagamento) +
                                    String.format("- Frete: R$ %.2f\n", frete) +
                                    String.format("\n*TOTAL: R$ %.2f*", totalFinal) +
                                    "\n\n--- Entrega ---\n" +
                                    String.format("%s, %s, %s\n", rua, numero, bairro) +
                                    String.format("Complemento: %s\n", comp) +
                                    "\n\nSua pizza está sendo preparada!";
                    
                    enviarResposta(remetente, resumo);
                    if (pagamento.equalsIgnoreCase("pix")) {
                         enviarResposta(remetente, "O pix é: 11952852966"); 
                    }
                    redisService.limparPedido(remetente);
                
                } else if (intentId.equals("DESPEDIDA")) {
                    enviarResposta(remetente, "Até logo! 👋");
                    redisService.limparPedido(remetente);

                } else { 
                    log.info("Lógica: Resposta padrão (NAO_ENTENDEU) para {}", remetente);
                    
                    if("AGUARDANDO_ESCOLHA_SABOR".equals(estadoAtual)) {
                        enviarResposta(remetente, "Desculpe, não entendi o sabor. Por favor, escolha um sabor do cardápio.");
                    } else if ("AGUARDANDO_TAMANHO".equals(estadoAtual)) {
                        enviarResposta(remetente, "Desculpe, não entendi o tamanho. (Pequena, Média ou Grande?)");
                    } else if ("AGUARDANDO_BEBIDA".equals(estadoAtual)) {
                        enviarResposta(remetente, "Desculpe, não entendi a bebida. (Ou digite 'não')");
                    } else if ("AGUARDANDO_PAGAMENTO".equals(estadoAtual)) {
                        enviarResposta(remetente, "Desculpe, não entendi a forma de pagamento. (Pix, Cartão ou Dinheiro?)");
                    } else if ("AGUARDANDO_CEP".equals(estadoAtual) || "AGUARDANDO_CEP_VERIFICACAO".equals(estadoAtual)) {
                        enviarResposta(remetente, "Desculpe, não entendi. Por favor, digite seu CEP (apenas números).");
                    } else {
                        enviarResposta(remetente, "Desculpe, não entendi. Digite 'Menu' ou 'Pedir'.");
                    }
                }

            } else {
                log.warn("Não foi possível extrair remetente ou texto da mensagem.");
            }

        } catch (Exception e) {
            log.error("Erro inesperado no processamento do bot: {}", e.getMessage(), e);
        }
    }

    private void enviarResposta(String para, String texto) {
        String urlEnvio = apiUrl + "/message/sendText/" + instanceName;
        Map<String, Object> body = Map.of("number", para, "text", texto);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            log.info("Enviando POST para [{}], com body: {}", urlEnvio, body);
            restTemplate.postForEntity(urlEnvio, entity, String.class);
        } catch (Exception e) {
            log.error("ERRO AO ENVIAR MENSAGEM para {}: {}", para, e.getMessage());
        }
    }

    private void enviarRespostaPdf(String para, String legenda, String urlDoPdf, String nomeArquivo) {
        String urlEnvio = apiUrl + "/message/sendMedia/" + instanceName;
        try {
            log.info("Baixando PDF de: {}", urlDoPdf);
            ResponseEntity<byte[]> response = restTemplate.getForEntity(urlDoPdf, byte[].class);
            byte[] pdfBytes = response.getBody();
            log.info("PDF baixado com sucesso ({} bytes).", pdfBytes.length);
            String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
            Map<String, Object> body = Map.of(
                    "number", para, 
                    "caption", legenda,
                    "mediatype", "document",
                    "media", pdfBase64, 
                    "fileName", nomeArquivo
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            log.info("Enviando PDF (Base64 puro) para [{}], com body: {{...base64 data...}}", urlEnvio);
            restTemplate.postForEntity(urlEnvio, entity, String.class);
            log.info("PDF (Base64) enviado com sucesso.");
        } catch (Exception e) {
            log.error("ERRO AO ENVIAR PDF (Base64) para {}: {}", para, e.getMessage());
        }
    }

    // Função auxiliar para remover acentos
    private String removeAcentos(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }
}