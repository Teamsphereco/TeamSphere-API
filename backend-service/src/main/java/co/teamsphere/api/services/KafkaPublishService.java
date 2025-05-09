package co.teamsphere.api.services;

import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public interface KafkaPublishService {

    SendResult<String, String> sendMessage(String msg);

}
