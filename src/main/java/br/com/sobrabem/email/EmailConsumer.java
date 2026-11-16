package br.com.sobrabem.email;

import br.com.sobrabem.config.RabbitConfig;
import br.com.sobrabem.domain.enums.Papel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class EmailConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailConsumer.class);

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

    private final JavaMailSender mailSender;
    private final String remetente;
    private final boolean envioHabilitado;

    public EmailConsumer(JavaMailSender mailSender,
                         @Value("${sobrabem.mail.remetente:}") String remetente,
                         @Value("${sobrabem.mail.envio-habilitado:false}") boolean envioHabilitado) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.envioHabilitado = envioHabilitado;
    }

    @RabbitListener(queues = RabbitConfig.FILA_EMAILS)
    public void consumir(ReservaEmailEvent e) {
        String quantidade = e.quantidade() + " " + e.doacao().unidade().rotulo();
        String prazo = FORMATO.format(e.doacao().prazoRetirada());
        String titulo = e.doacao().titulo();

        switch (e.tipo()) {
            case RESERVA_CRIADA -> {
                enviar(e.doador().email(), "Nova reserva: " + titulo, """
                        Olá, %s!

                        %s reservou %s de "%s".
                        A retirada deve acontecer até %s.
                        Na hora, peça o código de retirada e confirme no app.

                        Equipe SobraBem""".formatted(e.doador().nome(), e.receptor().nome(), quantidade, titulo, prazo));

                enviar(e.receptor().email(), "Comprovante de reserva: " + titulo, """
                        Olá, %s!

                        Sua reserva de %s de "%s" foi registrada.

                        Código de retirada: %s
                        Local: %s
                        Retire até %s.

                        Equipe SobraBem""".formatted(e.receptor().nome(), quantidade, titulo,
                        e.codigoRetirada(), e.doacao().endereco(), prazo));
            }
            case RESERVA_CANCELADA -> {
                if (e.canceladoPor() == Papel.RECEPTOR) {
                    enviar(e.doador().email(), "Reserva cancelada: " + titulo, """
                            Olá, %s!

                            %s cancelou a reserva de %s de "%s". O saldo voltou para a doação.

                            Equipe SobraBem""".formatted(e.doador().nome(), e.receptor().nome(), quantidade, titulo));
                } else {
                    enviar(e.receptor().email(), "Doação cancelada: " + titulo, """
                            Olá, %s!

                            A doação "%s" foi cancelada pelo doador, e sua reserva de %s também.

                            Equipe SobraBem""".formatted(e.receptor().nome(), titulo, quantidade));
                }
            }
        }
    }

    private void enviar(String para, String assunto, String texto) {
        if (!envioHabilitado) {
            log.info("[e-mail desligado] para={} | assunto={}\n{}", para, assunto, texto);
            return;
        }
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(para);
        mensagem.setSubject(assunto);
        mensagem.setText(texto);
        mailSender.send(mensagem);
    }
}