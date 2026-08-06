package com.chatbox.chatbox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class KnowledgeLoaderService {

    static final int DEFAULT_MAX_SELECTED_KNOWLEDGE_CHARS = 12_000;

    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeLoaderService.class);
    private static final String KNOWLEDGE_ROOT = "templates/knowledge/";
    private static final String INDEX_PATH = KNOWLEDGE_ROOT + "danh-sach.txt";
    private static final String CONTEXT_HEADER = "=== TRI THỨC GREENSHIELD LIÊN QUAN ===\n";
    private static final Pattern SECTION_SPLIT_PATTERN = Pattern.compile("(?m)(?=^#{1,5}\\s+)");
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[^a-z0-9]+", Pattern.CASE_INSENSITIVE);
    private static final Set<String> STOP_WORDS = Set.of(
            "about", "and", "are", "ban", "cho", "cua", "default", "duoc", "general",
            "green", "greenshield", "hay", "information", "la", "mekong", "mot", "nhu",
            "nhung", "the", "thong", "tin", "toi", "trong", "va", "ve", "voi", "what", "your"
    );

    private final List<KnowledgeChunk> chunks;
    private final Map<String, Integer> tokenDocumentFrequency;

    @Value("${app.ai.chat.max-knowledge-chars:12000}")
    private int maxSelectedKnowledgeChars = DEFAULT_MAX_SELECTED_KNOWLEDGE_CHARS;

    @Value("${app.ai.chat.max-knowledge-chunks:8}")
    private int maxSelectedChunks = 8;

    @Value("${app.ai.chat.max-knowledge-chunk-chars:4500}")
    private int maxChunkChars = 4_500;

    public KnowledgeLoaderService() {
        this.chunks = loadChunks();
        this.tokenDocumentFrequency = buildDocumentFrequency(chunks);
    }

    public String loadKnowledge() {
        return loadKnowledge("thong tin tong quan du an");
    }

    public String loadKnowledge(String query) {
        String normalizedQuery = normalize(query);
        Set<String> directTokens = tokenize(normalizedQuery);
        String expandedQuery = expandQuery(normalizedQuery);
        Set<String> expandedTokens = tokenize(expandedQuery);
        boolean linkRequest = containsAny(
                expandedQuery,
                "link", "url", "website", "truy cap", "duong dan", "web"
        );

        List<ScoredChunk> rankedChunks = chunks.stream()
                .map(chunk -> new ScoredChunk(
                        chunk,
                        score(chunk, normalizedQuery, directTokens, expandedTokens, linkRequest)
                ))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator
                        .comparingInt(ScoredChunk::score)
                        .reversed()
                        .thenComparingInt(scored -> scored.chunk().content().length()))
                .toList();

        if (rankedChunks.isEmpty()) {
            rankedChunks = fallbackChunks();
        }

        int knowledgeLimit = Math.max(CONTEXT_HEADER.length() + 500, maxSelectedKnowledgeChars);
        int chunkLimit = Math.max(500, maxChunkChars);
        int selectedChunkLimit = Math.max(1, maxSelectedChunks);
        StringBuilder selected = new StringBuilder(CONTEXT_HEADER);
        Map<String, Integer> chunksPerFile = new HashMap<>();
        int selectedCount = 0;

        for (ScoredChunk scored : rankedChunks) {
            KnowledgeChunk chunk = scored.chunk();
            int sameFileCount = chunksPerFile.getOrDefault(chunk.source(), 0);
            if (sameFileCount >= 4) {
                continue;
            }
            if (appendWithinLimit(selected, chunk, knowledgeLimit, chunkLimit)) {
                selectedCount++;
                chunksPerFile.put(chunk.source(), sameFileCount + 1);
            }
            if (selectedCount >= selectedChunkLimit || selected.length() >= knowledgeLimit) {
                break;
            }
        }

        return selected.toString();
    }

    private List<KnowledgeChunk> loadChunks() {
        List<KnowledgeChunk> loadedChunks = new ArrayList<>();
        try {
            for (String fileName : readResource(INDEX_PATH).lines().toList()) {
                String trimmedFileName = fileName.trim();
                if (trimmedFileName.isBlank() || trimmedFileName.startsWith("#")) {
                    continue;
                }
                String content = readResource(KNOWLEDGE_ROOT + trimmedFileName);
                addFileChunks(loadedChunks, trimmedFileName, content);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not load GreenShield knowledge resources", exception);
        }

        if (loadedChunks.isEmpty()) {
            loadedChunks.add(new KnowledgeChunk(
                    "knowledge-unavailable",
                    "Không thể tải kho tri thức",
                    "Không thể tải các tài liệu tri thức của GreenShield Mekong.",
                    "knowledge unavailable",
                    "khong the tai cac tai lieu tri thuc cua greenshield mekong"
            ));
        }

        LOGGER.info("Loaded {} GreenShield knowledge chunks", loadedChunks.size());
        return List.copyOf(loadedChunks);
    }

    private void addFileChunks(List<KnowledgeChunk> target, String source, String fileContent) {
        for (String rawSection : SECTION_SPLIT_PATTERN.split(fileContent)) {
            String content = rawSection.trim();
            if (content.isBlank()) {
                continue;
            }
            String title = extractTitle(content);
            target.add(new KnowledgeChunk(
                    source,
                    title,
                    content,
                    normalize(title + " " + source),
                    normalize(content)
            ));
        }
    }

    private String readResource(String path) throws IOException {
        try (var inputStream = new ClassPathResource(path).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private int score(
            KnowledgeChunk chunk,
            String normalizedQuery,
            Set<String> directTokens,
            Set<String> expandedTokens,
            boolean linkRequest) {
        int score = 0;

        for (String token : directTokens) {
            int tokenWeight = tokenWeight(token);
            if (chunk.normalizedTitle().contains(token)) {
                score += 15 * tokenWeight;
            }
            score += Math.min(countOccurrences(chunk.normalizedContent(), token), 3) * tokenWeight;
        }

        for (String token : expandedTokens) {
            if (directTokens.contains(token)) {
                continue;
            }
            int tokenWeight = tokenWeight(token);
            if (chunk.normalizedTitle().contains(token)) {
                score += 3 * tokenWeight;
            }
            score += Math.min(countOccurrences(chunk.normalizedContent(), token), 3)
                    * Math.max(1, tokenWeight / 3);
        }

        score += scoreAdjacentQueryTokens(chunk.normalizedTitle(), directTokens);

        if (normalizedQuery.length() >= 8 && chunk.normalizedContent().contains(normalizedQuery)) {
            score += 80;
        }
        if (linkRequest && chunk.normalizedContent().contains("https://greenshieldmekong.com")) {
            score += 80;
        }

        return score;
    }

    private Map<String, Integer> buildDocumentFrequency(List<KnowledgeChunk> knowledgeChunks) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (KnowledgeChunk chunk : knowledgeChunks) {
            Set<String> uniqueTokens = tokenize(chunk.normalizedTitle() + " " + chunk.normalizedContent());
            for (String token : uniqueTokens) {
                frequencies.merge(token, 1, Integer::sum);
            }
        }
        return Map.copyOf(frequencies);
    }

    private int tokenWeight(String token) {
        int frequency = tokenDocumentFrequency.getOrDefault(token, 0);
        double inverseFrequency = Math.log((chunks.size() + 1.0) / (frequency + 1.0));
        return Math.max(1, 1 + (int) Math.round(inverseFrequency * 2));
    }

    private int scoreAdjacentQueryTokens(String normalizedTitle, Set<String> directTokens) {
        int matches = 0;
        for (String token : directTokens) {
            if (normalizedTitle.contains(token)) {
                matches++;
            }
        }
        return matches >= 2 ? matches * 10 : 0;
    }

    private List<ScoredChunk> fallbackChunks() {
        List<ScoredChunk> fallback = chunks.stream()
                .filter(chunk -> containsAny(
                        chunk.normalizedTitle(),
                        "tom luoc ve du an", "y tuong va qua trinh", "san pham chinh"
                ))
                .map(chunk -> new ScoredChunk(chunk, 1))
                .toList();
        return fallback.isEmpty()
                ? List.of(new ScoredChunk(chunks.get(0), 1))
                : fallback;
    }

    private boolean appendWithinLimit(
            StringBuilder target,
            KnowledgeChunk chunk,
            int knowledgeLimit,
            int chunkLimit) {
        String sourceHeader = "\n\n[Nguồn: " + chunk.source() + " | Mục: " + chunk.title() + "]\n";
        int remaining = knowledgeLimit - target.length() - sourceHeader.length();
        if (remaining <= 0) {
            return false;
        }

        int allowed = Math.min(remaining, chunkLimit);
        String content = truncateAtLineBoundary(chunk.content(), allowed);
        if (content.isBlank()) {
            return false;
        }

        target.append(sourceHeader).append(content);
        return true;
    }

    private String truncateAtLineBoundary(String value, int maxChars) {
        if (value.length() <= maxChars) {
            return value;
        }
        int lastLineBreak = value.lastIndexOf('\n', maxChars);
        int end = lastLineBreak > 0 ? lastLineBreak : maxChars;
        return value.substring(0, end);
    }

    private String expandQuery(String query) {
        StringBuilder expanded = new StringBuilder(query);

        if (containsAny(query, "link", "url", "website", "truy cap", "duong dan", "web")) {
            expanded.append(" lien ket truy cap chinh thuc trang chu");
        }
        if (containsAny(query, "san pham", "product", "hop", "dia", "chen", "lot ly", "tui dan")) {
            expanded.append(" san pham gia ban vat lieu danh muc");
        }
        if (containsAny(query, "gia", "chi phi", "doanh thu", "tai chinh", "von", "loi nhuan")) {
            expanded.append(" gia ban chi phi du bao tai chinh nguon von doanh thu loi nhuan");
        }
        if (containsAny(query, "lien he", "contact", "email", "facebook", "tiktok", "dien thoai")) {
            expanded.append(" lien he email dien thoai mang xa hoi");
        }
        if (containsAny(query, "dat hang", "order", "shipping", "giao hang", "payment", "thanh toan")) {
            expanded.append(" ordering shipping payment dat hang giao hang thanh toan");
        }
        if (containsAny(query, "esg", "ben vung", "moi truong", "sustainability", "cong dong")) {
            expanded.append(" esg ben vung moi truong cong dong");
        }

        return expanded.toString();
    }

    private Set<String> tokenize(String value) {
        Set<String> tokens = new HashSet<>();
        for (String token : TOKEN_SPLIT_PATTERN.split(value)) {
            if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private int countOccurrences(String content, String token) {
        int count = 0;
        int index = 0;
        while (count < 3 && (index = content.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private boolean containsAny(String value, String... candidates) {
        return Arrays.stream(candidates).anyMatch(value::contains);
    }

    private String extractTitle(String section) {
        int lineBreak = section.indexOf('\n');
        return lineBreak >= 0 ? section.substring(0, lineBreak).trim() : section.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(decomposed)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private record KnowledgeChunk(
            String source,
            String title,
            String content,
            String normalizedTitle,
            String normalizedContent) {
    }

    private record ScoredChunk(KnowledgeChunk chunk, int score) {
    }
}
