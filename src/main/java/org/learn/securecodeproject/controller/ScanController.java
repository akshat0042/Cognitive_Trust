package org.learn.securecodeproject.controller;

import org.learn.securecodeproject.model.ScanResult;
import org.learn.securecodeproject.service.SemgrepService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScanController {

    @Autowired
    private SemgrepService semgrepService;

    // Accepts JSON: { "filename": "...", "content": "..." }
    @PostMapping("/scan")
    public ScanResult scan(@RequestBody Map<String,String> payload) throws IOException, InterruptedException {
        String filename = payload.getOrDefault("filename", "file.txt");
        String content = payload.getOrDefault("content", "");
        return semgrepService.scan(filename, content);
    }

    // Autofix endpoint: returns { "fixed": "file contents after fixes" }
    @PostMapping("/autofix")
    public Map<String,String> autofix(@RequestBody Map<String,String> payload) {
        String filename = payload.getOrDefault("filename","file.txt");
        String content = payload.getOrDefault("content","");
        // For demo, only implement secret replacement autofix
        String fixed = semgrepService.autofixReplaceSecrets(filename, content);
        return Map.of("fixed", fixed);
    }
}
