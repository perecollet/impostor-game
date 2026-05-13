package com.impostorgame.auth.service;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;

@Component
public class GuestNameGenerator {

    private List<String> nouns;
    private List<String> adjectives;
    private final Random random = new Random();

    @PostConstruct
    public void init() {
        adjectives = loadFile("usernames/adjectives.txt");
        nouns = loadFile("usernames/nouns.txt");
    }

    public String generate() {
        String adjective = adjectives.get(random.nextInt(adjectives.size()));
        String noun = nouns.get(random.nextInt(nouns.size()));
        int number = random.nextInt(9000) + 1000;
        return adjective + noun + "#" + number;
    }

    private List<String> loadFile(String path) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new ClassPathResource(path).getInputStream()))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Could not load file: " + path, e);
        }
    }
}
