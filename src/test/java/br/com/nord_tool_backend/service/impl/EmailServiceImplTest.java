package br.com.nord_tool_backend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceImplTest {

    private JavaMailSender mailSender;
    private ObjectProvider<JavaMailSender> sender;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        sender = mock(ObjectProvider.class);
        when(sender.getIfAvailable()).thenReturn(mailSender);
        when(sender.getObject()).thenReturn(mailSender);
    }

    @Test
    void naoEstaConfiguradoSemUsuarioOuSemSmtp() {
        assertFalse(new EmailServiceImpl(sender, null, null).configurado());
        assertFalse(new EmailServiceImpl(sender, "  ", "x@nord.com").configurado());

        when(sender.getIfAvailable()).thenReturn(null);
        assertFalse(new EmailServiceImpl(sender, "smtp@nord.com", null).configurado());
    }

    @Test
    void semConfiguracaoNaoEnviaENaoFalha() {
        EmailServiceImpl service = new EmailServiceImpl(sender, "", "");

        assertFalse(service.enviar("a@b.com", "Assunto", "Texto"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void enviaUsandoOUsuarioComoRemetentePadrao() {
        EmailServiceImpl service = new EmailServiceImpl(sender, " smtp@nord.com ", " ");

        assertTrue(service.configurado());
        assertTrue(service.enviar("a@b.com", "Assunto", "Texto"));

        ArgumentCaptor<SimpleMailMessage> msg = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(msg.capture());
        assertEquals("NordTool <smtp@nord.com>", msg.getValue().getFrom());
        assertArrayEquals(new String[]{"a@b.com"}, msg.getValue().getTo());
        assertEquals("Assunto", msg.getValue().getSubject());
        assertEquals("Texto", msg.getValue().getText());
    }

    @Test
    void enviaComRemetenteConfigurado() {
        EmailServiceImpl service = new EmailServiceImpl(sender, "smtp@nord.com", " nao-responda@nord.com ");

        assertTrue(service.enviar("a@b.com", "Assunto", "Texto"));

        ArgumentCaptor<SimpleMailMessage> msg = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(msg.capture());
        assertEquals("NordTool <nao-responda@nord.com>", msg.getValue().getFrom());
    }

    @Test
    void falhaDeEnvioDevolveFalseSemPropagar() {
        doThrow(new MailSendException("smtp fora")).when(mailSender).send(any(SimpleMailMessage.class));
        EmailServiceImpl service = new EmailServiceImpl(sender, "smtp@nord.com", null);

        assertFalse(service.enviar("a@b.com", "Assunto", "Texto"));
    }
}
