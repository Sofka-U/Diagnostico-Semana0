package com.example.usuarioservice.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceProducerTest {

    static class StubRabbitTemplate extends org.springframework.amqp.rabbit.core.RabbitTemplate {
        boolean sent = false;
        String lastExchange;
        String lastRouting;
        Object lastPayload;

        @Override
        public void convertAndSend(String exchange, String routingKey, Object message) {
            this.sent = true;
            this.lastExchange = exchange;
            this.lastRouting = routingKey;
            this.lastPayload = message;
        }
    }

    @Test
    @DisplayName("sendUserResponse invokes rabbitTemplate.convertAndSend")
    void sendUserResponse_invokesRabbitTemplate() throws Exception {
        UserServiceProducer producer = new UserServiceProducer();
        StubRabbitTemplate stub = new StubRabbitTemplate();

        Field f = UserServiceProducer.class.getDeclaredField("rabbitTemplate");
        f.setAccessible(true);
        f.set(producer, stub);

        UserResponse response = new UserResponse(10, "Test", "test@example.com", true);

        producer.sendUserResponse(response);

        assertThat(stub.sent).isTrue();
        assertThat(stub.lastExchange).isEqualTo("user-exchange");
        assertThat(stub.lastRouting).isEqualTo("user.response");
        assertThat(stub.lastPayload).isEqualTo(response);
    }

    @Test
    @DisplayName("sendUserResponse propagates exception when rabbitTemplate fails")
    void sendUserResponse_exceptionIsPropagated() throws Exception {
        UserServiceProducer producer = new UserServiceProducer();

        org.springframework.amqp.rabbit.core.RabbitTemplate stub = new org.springframework.amqp.rabbit.core.RabbitTemplate() {
            @Override
            public void convertAndSend(String exchange, String routingKey, Object message) {
                throw new RuntimeException("boom");
            }
        };

        Field f = UserServiceProducer.class.getDeclaredField("rabbitTemplate");
        f.setAccessible(true);
        f.set(producer, stub);

        UserResponse response = new UserResponse(10, "Test", "test@example.com", true);

        assertThrows(RuntimeException.class, () -> producer.sendUserResponse(response));
    }
}
