package org.learn.securecodeproject.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.learn.securecodeproject.model.ScanFinding;
import org.learn.securecodeproject.model.ScanResult;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class SemgrepService {

    private final ObjectMapper mapper = new ObjectMapper();

    private final String SEMGREP_RULES = "src/main/resources/semgrep-rules";

    public ScanResult scan(String filename, String content) throws IOException, InterruptedException {
        File temp = File.createTempFile("scanfile", getExtensionFromFilename(filename));
        Files.writeString(temp.toPath(), content);

        ProcessBuilder pb = new ProcessBuilder(
                "semgrep",
                "--config", SEMGREP_RULES,
                "--json",
                temp.getAbsolutePath()
        );

        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line);
            }
        }
        int rc = p.waitFor();

        String jsonOut = out.toString();
        ScanResult result = new ScanResult();
        result.success = (rc == 0 || rc == 1); // semgrep returns 1 when findings exist

        List<ScanFinding> findings = new ArrayList<>();
        if (jsonOut == null || jsonOut.isBlank()) {
            result.findings = findings;
            return result;
        }

        JsonNode root = mapper.readTree(jsonOut);
        JsonNode results = root.get("results");
        if (results != null && results.isArray()) {
            for (JsonNode r : results) {
                ScanFinding f = new ScanFinding();
                f.check_id = r.path("check_id").asText();
                f.path = r.path("path").asText(temp.getName());
                JsonNode start = r.path("start");
                f.start_line = start.path("line").asInt(-1);
                f.end_line = r.path("end").path("line").asInt(f.start_line);
                f.message = r.path("extra").path("message").asText();
                String cid = f.check_id == null ? "" : f.check_id.toLowerCase();
                if (cid.contains("secret") || cid.contains("hardcoded")) {
                    f.suggestion = "Replace hardcoded secret with an environment variable (example: process.env.MY_SECRET).";
                    f.extra = "auto_fix_replace_secret";
                } else if (cid.contains("auth") || cid.contains("authorization")) {
                    f.suggestion = "Add authorization/role checks to this endpoint or function.";
                } else {
                    f.suggestion = "Review the reported finding and apply secure coding best practices.";
                }
                findings.add(f);
            }
        }
        result.findings = findings;
        return result;
    }

    private String getExtensionFromFilename(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx == -1) return ".txt";
        return filename.substring(idx);
    }

    public String autofixReplaceSecrets(String filename, String content) {
        try {

            String replaced = content.replaceAll("(?i)(api[_-]?key|apikey|secret)[\\s]*[:=][\\s]*[\"']([^\"']+)[\"']", "$1: process.env.MY_SECRET");
            replaced = replaced.replaceAll("(?i)(const|let|var)\\s+(api[_-]?key|apikey|secret)\\s*=\\s*[\"']([^\"']+)[\"']", "$1 $2 = process.env.MY_SECRET");
            return replaced;
        } catch (Exception e) {
            return content;
        }
    }
}