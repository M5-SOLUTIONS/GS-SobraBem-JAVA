package br.com.sobrabem.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "sb.events";
    public static final String FILA_EMAILS = "sb.emails";
    public static final String RK_RESERVA_CRIADA = "reserva.criada";
    public static final String RK_RESERVA_CANCELADA = "reserva.cancelada";

    @Bean
    DirectExchange eventsExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    Queue filaEmails() {
        return QueueBuilder.durable(FILA_EMAILS).build();
    }

    @Bean
    Binding bindingReservaCriada(Queue filaEmails, DirectExchange eventsExchange) {
        return BindingBuilder.bind(filaEmails).to(eventsExchange).with(RK_RESERVA_CRIADA);
    }

    @Bean
    Binding bindingReservaCancelada(Queue filaEmails, DirectExchange eventsExchange) {
        return BindingBuilder.bind(filaEmails).to(eventsExchange).with(RK_RESERVA_CANCELADA);
    }

    @Bean
    MessageConverter messageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}