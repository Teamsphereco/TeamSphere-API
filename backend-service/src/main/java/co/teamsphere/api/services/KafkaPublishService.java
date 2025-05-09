package co.teamsphere.api.services;

import org.springframework.stereotype.Service;

@Service
public interface KafkaPublishService {
    void sendMessage(String msg);
}
