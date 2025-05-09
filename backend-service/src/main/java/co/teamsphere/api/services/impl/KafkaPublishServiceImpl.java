package co.teamsphere.api.services.impl;

import co.teamsphere.api.config.KafkaTopicConfig;
import co.teamsphere.api.services.KafkaPublishService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class KafkaPublishServiceImpl implements KafkaPublishService {

    private final KafkaTopicConfig kafkaTopicConfig;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void sendMessage(String payload) {
        kafkaTemplate
            .send(kafkaTopicConfig.topic().name(), payload)
            .thenApplyAsync(result -> {
                log.info("{} with message=[{}]", result.getRecordMetadata(), payload);
                return result;
            })
            .exceptionallyAsync(err -> {
                log.error("Unable to send message=[{}] due to: {}", payload, err.getMessage());
                return null;
            })
            .join();
    }

}
