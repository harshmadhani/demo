package com.example.demo.file;

import com.example.demo.SseTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LogsService {
    private static final Logger log = LoggerFactory.getLogger(LogsService.class);

    private static final String TOPIC = "logTopic";
    private final SseTemplate sseTemplate;
    private static final AtomicLong COUNTER = new AtomicLong(10);

    public LogsService(SseTemplate sseTemplate, FileMonitoringService fileMonitoringService) {
        this.sseTemplate = sseTemplate;
        fileMonitoringService.listen(file -> {
            try {
                Files.lines(file)
                        .skip(COUNTER.get())
                        .forEach(line ->
                                sseTemplate.broadcast(TOPIC, SseEmitter.event()
                                        .id(String.valueOf(COUNTER.incrementAndGet()))
                                        .data(line)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public SseEmitter newSseEmitter() {
        return sseTemplate.newSseEmitter(TOPIC);
    }
}
