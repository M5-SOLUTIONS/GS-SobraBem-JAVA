package br.com.sobrabem.email;

import br.com.sobrabem.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmailPublisher {

    private static final Logger log = LoggerFactory.getLogger(EmailPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public EmailPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(ReservaEmailEvent evento) {
        String routingKey = evento.tipo() == ReservaEmailEvent.Tipo.RESERVA_CRIADA
                ? RabbitConfig.RK_RESERVA_CRIADA
                : RabbitConfig.RK_RESERVA_CANCELADA;
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, evento);
        } catch (AmqpException e) {
            log.warn("E-mail da reserva {} ({}) não foi publicado: {}", evento.reservaId(), evento.tipo(), e.getMessage());
        }
    }
}