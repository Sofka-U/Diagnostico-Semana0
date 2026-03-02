package com.example.usuarioservice.messaging;

import com.example.usuarioservice.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceProducer - RabbitMQ Messaging")
class UserServiceProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserServiceProducer userServiceProducer;

    private UserResponse testResponse;

    @BeforeEach
    void setUp() {
        // Create test response object
        testResponse = new UserResponse();
        testResponse.setId(1);
        testResponse.setName("Juan");
        testResponse.setMail("juan@test.com");
        testResponse.setActive(true);
    }

    @Test
    @DisplayName("sendUserResponse with valid message should send successfully")
    // TEST_PLAN: Envío exitoso de mensaje - Line ~14
    void sendUserResponse_mensajeValido_envioExitoso() {
        // Given
        doNothing().when(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.USER_EXCHANGE),
            eq(RabbitMQConfig.USER_RESPONSE_ROUTING_KEY),
            any(UserResponse.class)
        );

        // When
        userServiceProducer.sendUserResponse(testResponse);

        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(
            eq(RabbitMQConfig.USER_EXCHANGE),
            eq(RabbitMQConfig.USER_RESPONSE_ROUTING_KEY),
            eq(testResponse)
        );
    }

    @Test
    @DisplayName("sendUserResponse should verify correct exchange and routing key")
    // TEST_PLAN: Verificar exchange y routing key - Line ~14
    void sendUserResponse_verificaExchangeYRoutingKey() {
        // Given
        doNothing().when(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.USER_EXCHANGE),
            eq(RabbitMQConfig.USER_RESPONSE_ROUTING_KEY),
            any(UserResponse.class)
        );

        // When
        userServiceProducer.sendUserResponse(testResponse);

        // Then
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.USER_EXCHANGE),
            eq(RabbitMQConfig.USER_RESPONSE_ROUTING_KEY),
            eq(testResponse)
        );
    }

    @Test
    @DisplayName("sendUserResponse with multiple messages should send each one")
    // TEST_PLAN: Envío múltiple de mensajes - Edge case
    void sendUserResponse_multiplesMessages_enviaTodasExitosamente() {
        // Given
        UserResponse response1 = new UserResponse(1, "Juan", "juan@test.com", true);
        UserResponse response2 = new UserResponse(2, "Pedro", "pedro@test.com", true);

        doNothing().when(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.USER_EXCHANGE),
            eq(RabbitMQConfig.USER_RESPONSE_ROUTING_KEY),
            any(UserResponse.class)
        );

        // When
        userServiceProducer.sendUserResponse(response1);
        userServiceProducer.sendUserResponse(response2);

        // Then
        verify(rabbitTemplate, times(2)).convertAndSend(
            eq(RabbitMQConfig.USER_EXCHANGE),
            eq(RabbitMQConfig.USER_RESPONSE_ROUTING_KEY),
            any(UserResponse.class)
        );
    }
}


