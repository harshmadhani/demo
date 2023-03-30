package com.example.demo.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

@Service
public class FileMonitoringService {
    private static final Logger log = LoggerFactory.getLogger(FileMonitoringService.class);
    private final AtomicBoolean listen = new AtomicBoolean(false);

    private final WatchKey key;
    private final Path monitoringDirectory;
    private final Path file;
    private final List<Consumer<Path>> callbacks = new CopyOnWriteArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public FileMonitoringService(@Value("${logging.file.path}") String directory, @Value("${logging.file.name:test.txt}") String fileName) throws IOException {

        this.monitoringDirectory = new FileSystemResource(directory).getFile().toPath();

        this.file = monitoringDirectory.resolve(fileName);

        final WatchService ws = FileSystems.getDefault().newWatchService();

        key = monitoringDirectory.register(ws, ENTRY_MODIFY);

        executorService.submit(() -> eventMonitor());
    }

    public void listen(Consumer<Path> consumer) {
        callbacks.add(consumer);
    }

    void eventMonitor() {
        listen.set(true);

        while (listen.get()) {
            try {
                Thread.sleep(50);
                for (final WatchEvent<?> event : key.pollEvents()) {
                    final Path changed = monitoringDirectory.resolve((Path) event.context());

                    if (event.kind() == ENTRY_MODIFY && changed.equals(file)) {
                        log.trace("monitoring - ENTRY_MODIFY event: " + changed);
                        callbacks.forEach(c -> c.accept(changed));
                    }
                }

                boolean isKeyStillValid = key.reset();
                if (!isKeyStillValid) {
                    log.trace("eventMonitor - key is no longer valid: " + key);
                    listen.set(false);
                }
            } catch (InterruptedException ex) {
                listen.set(false);
            }
        }
    }
}
