package br.com.nord_tool_backend.service;

/** Envio de e-mail em texto simples. Sem SMTP configurado, só registra no log (não falha a operação). */
public interface EmailService {

    /** Devolve true se a mensagem foi entregue ao servidor SMTP. */
    boolean enviar(String para, String assunto, String texto);

    boolean configurado();
}
