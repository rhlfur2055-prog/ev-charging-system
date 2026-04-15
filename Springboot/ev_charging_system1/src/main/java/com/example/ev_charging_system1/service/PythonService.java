package com.example.ev_charging_system1.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PythonService {

    @Value("${ev.python.exe}")
    private String pythonExe;

    @Value("${ev.python.script-dir}")
    private String scriptDir;

    @Value("${ev.python.log-dir}")
    private String pythonLogDir;

    @Value("${ev.python.start-timeout-seconds:15}")
    private long startTimeoutSeconds;

    /** Track running PIDs per script so we don't spawn duplicates. */
    private final java.util.concurrent.ConcurrentHashMap<String, Process> running = new java.util.concurrent.ConcurrentHashMap<>();

    private static final List<String> SCRIPTS = Arrays.asList(
            "Platenumber_A01.py",
            "Platenumber_A02.py",
            "Platenumber_B01.py",
            "Platenumber_B02.py"
    );

    @PostConstruct
    public void init() {
        log.info("[PythonService] python={} scriptDir={} logDir={}",
                pythonExe, scriptDir, pythonLogDir);
    }

    /**
     * Launch all 4 zone detectors. Returns names of scripts that failed to
     * start so the controller can surface a meaningful error to the UI.
     */
    public java.util.List<String> runIdentification() {
        File workDir = new File(scriptDir).getAbsoluteFile();
        if (!workDir.isDirectory()) {
            log.error("[PythonService] scriptDir not found: {}", workDir);
            return new java.util.ArrayList<>(SCRIPTS);
        }

        try {
            Files.createDirectories(Path.of(pythonLogDir));
        } catch (Exception e) {
            log.warn("[PythonService] cannot create log dir {}: {}", pythonLogDir, e.getMessage());
        }

        java.util.List<String> failed = new java.util.ArrayList<>();
        for (String fileName : SCRIPTS) {
            Process existing = running.get(fileName);
            if (existing != null && existing.isAlive()) {
                log.info("[PythonService] {} already running (pid={})", fileName, existing.pid());
                continue;
            }
            try {
                File logFile = new File(pythonLogDir,
                        fileName.replace(".py", ".log"));
                ProcessBuilder pb = new ProcessBuilder(pythonExe, "-u", fileName)
                        .directory(workDir)
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
                Process p = pb.start();
                running.put(fileName, p);

                // Quick liveness probe: if it dies within startup window, mark failed.
                if (p.waitFor(Math.min(2, startTimeoutSeconds), TimeUnit.SECONDS)) {
                    int code = p.exitValue();
                    log.error("[PythonService] {} exited early code={} (see {})", fileName, code, logFile);
                    failed.add(fileName);
                } else {
                    log.info("[PythonService] started {} pid={} log={}", fileName, p.pid(), logFile);
                }
            } catch (Exception e) {
                log.error("[PythonService] launch failed {}: {}", fileName, e.getMessage());
                failed.add(fileName);
            }
        }
        return failed;
    }

    public Map<String, Boolean> status() {
        Map<String, Boolean> m = new ConcurrentHashMap<>();
        for (String s : SCRIPTS) {
            Process p = running.get(s);
            m.put(s, p != null && p.isAlive());
        }
        return m;
    }
}
