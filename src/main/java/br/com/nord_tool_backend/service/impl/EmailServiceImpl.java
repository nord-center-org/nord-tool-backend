package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> sender;
    private final String usuario;
    private final String remetente;

    public EmailServiceImpl(ObjectProvider<JavaMailSender> sender,
                            @Value("${spring.mail.username:}") String usuario,
                            @Value("${nord-tool.mail.from:}") String remetente) {
        this.sender = sender;
        this.usuario = usuario == null ? "" : usuario.trim();
        this.remetente = remetente == null || remetente.isBlank() ? this.usuario : remetente.trim();
    }

    @Override
    public boolean configurado() {
        return !usuario.isEmpty() && sender.getIfAvailable() != null;
    }

    @Override
    public boolean enviar(String para, String assunto, String texto) {
        if (!configurado()) {
            log.warn("E-mail não enviado (SMTP não configurado): assunto '{}'", assunto);
            return false;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom("NordTool <" + remetente + ">");
            msg.setTo(para);
            msg.setSubject(assunto);
            msg.setText(texto);
            sender.getObject().send(msg);
            return true;
        } catch (RuntimeException ex) {
            // Falha de envio não pode revelar se o e-mail existe nem derrubar a requisição.
            log.warn("Falha ao enviar e-mail '{}': {}", assunto, ex.getClass().getSimpleName());
            return false;
        }
    }
}
