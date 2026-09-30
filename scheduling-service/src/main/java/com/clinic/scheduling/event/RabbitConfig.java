package com.clinic.scheduling.event;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange patientExchange() {
        return new TopicExchange(Messaging.PATIENT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(Messaging.DEAD_LETTER_EXCHANGE, true, false);
    }

    // Tin nhắn xử lý lỗi bị chuyển sang DLQ để xem lại, không mất và không lặp vô hạn
    @Bean
    public Queue patientEventsQueue() {
        return QueueBuilder.durable(Messaging.PATIENT_EVENTS_QUEUE)
            .deadLetterExchange(Messaging.DEAD_LETTER_EXCHANGE)
            .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(Messaging.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding patientEventsBinding() {
        return BindingBuilder.bind(patientEventsQueue()).to(patientExchange()).with("patient.*");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
