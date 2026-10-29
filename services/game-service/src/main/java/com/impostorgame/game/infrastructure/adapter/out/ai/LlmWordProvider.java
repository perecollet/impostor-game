package com.impostorgame.game.infrastructure.adapter.out.ai;

import com.impostorgame.game.domain.exception.WordPairUnavailableException;
import com.impostorgame.game.domain.model.Word;
import com.impostorgame.game.domain.model.WordPair;
import com.impostorgame.game.domain.port.out.WordProvider;
import com.impostorgame.game.infrastructure.config.LlmWordProviderProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LlmWordProvider implements WordProvider {

    private static final String PROMPT_TEMPLATE = """
            Estás generando palabras para un juego social de deducción llamado "Impostor".
            Todos los jugadores reciben la misma palabra secreta, excepto el impostor, que recibe
            una pista: una palabra relacionada pero distinta, para que pueda disimular sin conocer
            la palabra real.
            
            Genera un único par:
            - "word": la palabra secreta.
            - "hint": la pista para el impostor.
            
            Reglas:
            - Idioma de ambas palabras: %s.
            - Cada una es un único sustantivo común, en singular y sin espacios.
            - Vocabulario cotidiano, conocido por cualquier adulto. Ni nombres propios ni marcas.
            - La pista pertenece al mismo ámbito que la palabra, pero no puede ser la misma palabra,
              un sinónimo, un diminutivo ni una palabra derivada de ella.
            - Ninguna de las dos puede ser una de estas palabras ya usadas: %s.
            
            %s
            """;

    private final LlmWordProviderProperties properties;
    private final ChatClient chatClient;
    private final BeanOutputConverter<LlmWordPairResponse> converter;
    private final String format;


    public LlmWordProvider(ChatClient.Builder chatClientBuilder, LlmWordProviderProperties properties) {
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
        this.converter = new BeanOutputConverter<>(LlmWordPairResponse.class);
        this.format = converter.getFormat();
    }

    @Override
    public WordPair obtainWords(Set<Word> usedWords) {
        Set<Word> excludedWords = new HashSet<>(usedWords);
        RuntimeException lastCause = null;

        for (int i = 0; i < properties.maxAttempts(); i++) {
            String prompt = PROMPT_TEMPLATE.formatted(properties.language(), toPromptList(excludedWords), format);
            String rawResponse = callModel(prompt);
            WordPair pair;
            try {
                pair = interpret(rawResponse);
            } catch (RuntimeException e) {
                lastCause = e;
                continue;
            }

            if (!usesExcludedWord(pair, excludedWords)) {
                return pair;
            }
            excludedWords.add(pair.word());
            excludedWords.add(pair.hint());
        }

        throw new WordPairUnavailableException(
                "No valid word pair after %d attempts".formatted(properties.maxAttempts()),
                lastCause);
    }

    private String callModel(String prompt) {
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (RuntimeException e) {
            throw new WordPairUnavailableException("Word model call failed", e);
        }
    }

    private WordPair interpret(String rawResponse) {
        LlmWordPairResponse response = converter.convert(rawResponse);
        return new WordPair(new Word(response.word()), new Word(response.hint()));
    }

    private boolean usesExcludedWord(WordPair pair, Set<Word> excludedWords) {
        return excludedWords.contains(pair.word()) || excludedWords.contains(pair.hint());
    }

    private static String toPromptList(Set<Word> usedWords) {
        if (usedWords.isEmpty()) {
            return "ninguna";
        }

        return usedWords.stream()
                .map(Word::value)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
