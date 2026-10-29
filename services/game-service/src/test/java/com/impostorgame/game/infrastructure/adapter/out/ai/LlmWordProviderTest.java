package com.impostorgame.game.infrastructure.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.impostorgame.game.domain.exception.InvalidWordPairException;
import com.impostorgame.game.domain.exception.WordPairUnavailableException;
import com.impostorgame.game.domain.model.Word;
import com.impostorgame.game.domain.model.WordPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.impostorgame.game.infrastructure.config.LlmWordProviderProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.TransientAiException;

@Tag("unit")
class LlmWordProviderTest {

    private static final LlmWordProviderProperties DEFAULT_PROPERTIES =
            new LlmWordProviderProperties(3, "español");

    @Test
    void returnsNormalizedPairFromValidResponse() {
        FakeChatModel model = FakeChatModel.replying(json("Gato", "Perro"));
        LlmWordProvider provider = providerWith(model, DEFAULT_PROPERTIES);

        WordPair pair = provider.obtainWords(Set.of());

        assertThat(pair).isEqualTo(new WordPair(new Word("gato"), new Word("perro")));
        assertThat(model.calls()).isEqualTo(1);
    }

    @Test
    void retriesWhenResponseIsNotParseable() {
        FakeChatModel model = FakeChatModel.replying("esto no es json", json("gato", "perro"));
        LlmWordProvider provider = providerWith(model, DEFAULT_PROPERTIES);

        WordPair pair = provider.obtainWords(Set.of());

        assertThat(pair).isEqualTo(new WordPair(new Word("gato"), new Word("perro")));
        assertThat(model.calls()).isEqualTo(2);
    }

    @Test
    void retriesWhenResponseViolatesPairInvariant() {
        FakeChatModel model = FakeChatModel.replying(json("gato", "GATO"), json("gato", "perro"));
        LlmWordProvider provider = providerWith(model, DEFAULT_PROPERTIES);

        WordPair pair = provider.obtainWords(Set.of());

        assertThat(pair).isEqualTo(new WordPair(new Word("gato"), new Word("perro")));
        assertThat(model.calls()).isEqualTo(2);
    }

    @Test
    void retriesWhenResponseHasBlankWord() {
        FakeChatModel model = FakeChatModel.replying(json(" ", "perro"), json("gato", "perro"));
        LlmWordProvider provider = providerWith(model, DEFAULT_PROPERTIES);

        WordPair pair = provider.obtainWords(Set.of());

        assertThat(pair).isEqualTo(new WordPair(new Word("gato"), new Word("perro")));
        assertThat(model.calls()).isEqualTo(2);
    }

    @Test
    void retriesWhenWordIsExcluded() {
        Set<Word> usedWords = Set.of(new Word("gato"));
        FakeChatModel model = FakeChatModel.replying(json("gato", "perro"), json("león", "tigre"));
        LlmWordProvider provider = providerWith(model, DEFAULT_PROPERTIES);

        WordPair pair = provider.obtainWords(usedWords);

        assertThat(pair).isEqualTo(new WordPair(new Word("león"), new Word("tigre")));
        assertThat(model.calls()).isEqualTo(2);
    }

    @Test
    void retriesWhenHintIsExcluded() {
        Set<Word> usedWords = Set.of(new Word("perro"));
        FakeChatModel model = FakeChatModel.replying(json("gato", "perro"), json("león", "tigre"));
        LlmWordProvider provider = providerWith(model, DEFAULT_PROPERTIES);

        WordPair pair = provider.obtainWords(usedWords);

        assertThat(pair).isEqualTo(new WordPair(new Word("león"), new Word("tigre")));
        assertThat(model.calls()).isEqualTo(2);
    }

    @Test
    void sendsExcludedWordsToTheModel() {
        Set<Word> usedWords = Set.of(new Word("ornitorrinco"), new Word("equidna"));
        FakeChatModel model = FakeChatModel.replying(json("gato", "perro"));
        LlmWordProvider provider = providerWith(model, DEFAULT_PROPERTIES);

        provider.obtainWords(usedWords);

        assertThat(model.promptText(0)).contains("ornitorrinco").contains("equidna");
    }

    @Test
    void addsRejectedWordsToExclusionOnRetry() {
        Set<Word> usedWords = Set.of(new Word("gato"));
        FakeChatModel model = FakeChatModel.replying(json("gato", "ornitorrinco"), json("león", "tigre"));
        LlmWordProvider provider = providerWith(model, DEFAULT_PROPERTIES);

        provider.obtainWords(usedWords);

        assertThat(model.promptText(1)).contains("ornitorrinco");
    }

    @Test
    void throwsUnavailableAfterMaxAttempts() {
        FakeChatModel model = FakeChatModel.replying(json("gato", "gato"));
        LlmWordProvider provider = providerWith(model, DEFAULT_PROPERTIES);
        Set<Word> usedWords = Set.of();

        assertThatThrownBy(() -> provider.obtainWords(usedWords))
                .isInstanceOf(WordPairUnavailableException.class);
        assertThat(model.calls()).isEqualTo(3);
    }

    @Test
    void chainsLastCauseWhenAttemptsAreExhausted() {
        FakeChatModel model = FakeChatModel.replying(json("gato", "gato"));
        LlmWordProvider provider = providerWith(model, DEFAULT_PROPERTIES);
        Set<Word> usedWords = Set.of();

        assertThatThrownBy(() -> provider.obtainWords(usedWords))
                .isInstanceOf(WordPairUnavailableException.class)
                .hasCauseInstanceOf(InvalidWordPairException.class);
    }

    @Test
    void respectsConfiguredMaxAttempts() {
        LlmWordProviderProperties properties = new LlmWordProviderProperties(5, "español");
        FakeChatModel model = FakeChatModel.replying(json("gato", "gato"));
        LlmWordProvider provider = providerWith(model, properties);
        Set<Word> usedWords = Set.of();

        assertThatThrownBy(() -> provider.obtainWords(usedWords))
                .isInstanceOf(WordPairUnavailableException.class);
        assertThat(model.calls()).isEqualTo(5);
    }

    @Test
    void doesNotRetryWhenModelCallFails() {
        TransientAiException failure = new TransientAiException("timeout");
        FakeChatModel model = FakeChatModel.failingWith(failure);
        LlmWordProvider provider = providerWith(model, DEFAULT_PROPERTIES);
        Set<Word> usedWords = Set.of();

        assertThatThrownBy(() -> provider.obtainWords(usedWords))
                .isInstanceOf(WordPairUnavailableException.class)
                .hasRootCause(failure);
        assertThat(model.calls()).isEqualTo(1);
    }

    private static LlmWordProvider providerWith(FakeChatModel model, LlmWordProviderProperties properties) {
        return new LlmWordProvider(ChatClient.builder(model), properties);
    }

    private static String json(String word, String hint) {
        return """
                {"word": "%s", "hint": "%s"}
                """.formatted(word, hint);
    }

    private static final class FakeChatModel implements ChatModel {

        private final List<String> replies;
        private final RuntimeException failure;
        private final List<Prompt> prompts = new ArrayList<>();

        private FakeChatModel(List<String> replies, RuntimeException failure) {
            this.replies = replies;
            this.failure = failure;
        }

        static FakeChatModel replying(String... replies) {
            return new FakeChatModel(List.of(replies), null);
        }

        static FakeChatModel failingWith(RuntimeException failure) {
            return new FakeChatModel(List.of(), failure);
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            if (failure != null) {
                throw failure;
            }
            int index = Math.min(prompts.size() - 1, replies.size() - 1);
            return response(replies.get(index));
        }

        int calls() {
            return prompts.size();
        }

        String promptText(int callIndex) {
            return prompts.get(callIndex).getContents();
        }

        private static ChatResponse response(String text) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }
    }
}