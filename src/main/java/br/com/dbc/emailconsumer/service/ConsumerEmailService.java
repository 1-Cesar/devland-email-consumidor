package br.com.dbc.emailconsumer.service;

import br.com.dbc.emailconsumer.dto.EmailDto;
import br.com.dbc.emailconsumer.enums.TipoMensagem;
import br.com.dbc.emailconsumer.exceptions.RegraDeNegocioException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumerEmailService {

    @Autowired
    private ObjectMapper objectMapper;

    private final freemarker.template.Configuration fmConfiguration;

    @Value("${spring.mail.username}")
    private String from;

    private final JavaMailSender emailSender;

    @KafkaListener(
            topics = "${kafka.email-devland}",
            groupId = "${kafka.user}",
            containerFactory = "listenerContainerFactory",
            clientIdPrefix = "email-devland"
    )
    public void consumir(@Payload String mensagem,
                         @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
                         @Header(KafkaHeaders.OFFSET) Long offset) throws JsonProcessingException {

        EmailDto emailDto = objectMapper.readValue(mensagem, EmailDto.class);

        sendEmailUsuario(emailDto);

        log.info("Mensagem enviada: {}", emailDto);
    }

    public void sendEmailUsuario(EmailDto emailDto)  {
        try {
            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setFrom(from);
            mimeMessageHelper.setTo(emailDto.getEmail());
            if (emailDto.getTipoMensagem().getTipo().equals(TipoMensagem.CREATE.getTipo())) {
                mimeMessageHelper.setSubject("Olá, " + emailDto.getNome() + "! Seja bem-vindo(a) na DevLand!");
            } else if (emailDto.getTipoMensagem().getTipo().equals(TipoMensagem.UPDATE.getTipo())) {
                mimeMessageHelper.setSubject(emailDto.getNome() + ", seus dados foram atualizados!");
            } else if (emailDto.getTipoMensagem().getTipo().equals(TipoMensagem.DELETE.getTipo())) {
                mimeMessageHelper.setSubject(emailDto.getNome() + ", sentiremos sua falta na DevLand!");
            } else if (emailDto.getTipoMensagem().getTipo().equals(TipoMensagem.CADASTROINCOMPLETO.getTipo())) {
                mimeMessageHelper.setSubject(emailDto.getNome() + ", sentiremos sua falta na DevLand!");
            } else {
                throw new RegraDeNegocioException("Falha no envio de e-mail");
            }
            mimeMessageHelper.setText(getContentFromTemplatePessoa(emailDto), true);
            emailSender.send(mimeMessageHelper.getMimeMessage());
        } catch (RegraDeNegocioException | MessagingException | IOException | TemplateException e) {
            log.info("Erro no envio de email");
        }
    }

    public String getContentFromTemplatePessoa(EmailDto emailDto) throws IOException, TemplateException {
        Map<String, Object> dados = new HashMap<>();

        Template template;

        if (emailDto.getTipoMensagem().getTipo().equals(TipoMensagem.CREATE.getTipo())) {
            dados.put("nome", "Olá, " + emailDto.getNome() + "! Seja bem-vindo(a) na DevLand!");
            dados.put("mensagem", "Seu cadastro foi realizado com sucesso, seu código de identificação é " + emailDto.getIdUsuario());
            dados.put("email", "Qualquer dúvida, entre em contato com o suporte pelo e-mail " + from);
            template = fmConfiguration.getTemplate("email-template.html");

        } else if (emailDto.getTipoMensagem().getTipo().equals(TipoMensagem.UPDATE.getTipo())) {
            dados.put("nome", "Olá, " + emailDto.getNome() + "! Seus dados foram atualizados!");
            dados.put("mensagem", "Seus dados foram atualizados com sucesso e já podem ser encontrados por empresas e talentos.");
            dados.put("email", "Qualquer dúvida, entre em contato com o suporte pelo e-mail " + from);
            template = fmConfiguration.getTemplate("email-template.html");

        } else {
            dados.put("nome", "Olá, " + emailDto.getNome() + "! Sentiremos sua falta na DevLand");
            dados.put("mensagem", "Seu cadastro foi removido da nossa rede, mas você pode voltar quando quiser!");
            dados.put("email", "Qualquer dúvida, entre em contato com o suporte pelo e-mail " + from);
            template = fmConfiguration.getTemplate("email-template.html");
        }
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, dados);
        return html;
    }
}
