package co.teamsphere.api.helpers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class TestAppender extends AppenderBase<ILoggingEvent> {
    private final List<ILoggingEvent> logEvents = new CopyOnWriteArrayList<>();

    @Override
    protected void append(ILoggingEvent eventObject) {
        logEvents.add(eventObject);
    }

    public void clearEvents() {
        logEvents.clear();
    }
}
