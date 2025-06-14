package ch.epfl.rechor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * An index of stop names that allows searching for stops based on queries.
 * The search is tolerant to variations in accents, letter case, and allows partial matching.
 */
public final class StopIndex {
    private final List<String> stopNames;
    private final Map<String, String> alternativeNames;

    /**
     * Constructs a new stop index with the given stop names.
     *
     * @param stopNames The list of all stop names to index
     * @param alternativeNames Map of alternative names to their canonical counterparts
     */
    public StopIndex(List<String> stopNames, Map<String, String> alternativeNames) {
        this.stopNames = List.copyOf(stopNames);
        this.alternativeNames = Map.copyOf(alternativeNames);
    }

    /**
     * Returns the list of stop names matching the given query, sorted by relevance.
     *
     * @param query The search query
     * @param maxResults The maximum number of results to return
     * @return A list of stop names matching the query, sorted by relevance
     */
    public List<String> stopsMatching(String query, int maxResults) {
        // Split the query into sub-queries
        String[] subQueries = query.trim().split("\\s+");

        // Collect all matches with their scores
        Map<String, Integer> matchesWithScores = new TreeMap<>();

        // Process all stop names and alternative names
        Set<String> allNames = new HashSet<>(stopNames);
        allNames.addAll(alternativeNames.keySet());

        for (String name : allNames) {
            boolean allSubQueriesMatch = true;
            int totalScore = 0;

            for (String subQuery : subQueries) {
                // Create pattern for this sub-query
                Pattern pattern = createPatternForSubQuery(subQuery);
                Matcher matcher = pattern.matcher(name);

                // Check if this sub-query matches the name
                if (matcher.find()) {
                    int score = calculateScore(subQuery, name, matcher.start(), matcher.end());
                    totalScore += score;
                } else {
                    allSubQueriesMatch = false;
                    break;
                }
            }

            // Only add when all subqueries match
            if (allSubQueriesMatch) {
                // If it's an alternative name, map it to its canonical form
                String canonicalName = alternativeNames.getOrDefault(name, name);

                // Merge to avoid duplicates. Keep the highest score.
                matchesWithScores.merge(canonicalName, totalScore, Integer::max);
            }
        }

        // Sort matches by score and return top results
        return matchesWithScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * Creates a regexp pattern for a given sub-query.
     */
    private Pattern createPatternForSubQuery(String subQuery) {
        StringBuilder patternBuilder = new StringBuilder();
        Pattern specialChars = Pattern.compile("[caeiou]", Pattern.UNICODE_CASE);
        String[] parts = specialChars.splitWithDelimiters(subQuery, 0);

        for (String part: parts) {
            // Find a special char add append the pattern to the builder or add the "constant"
            // part with quotes
            if (part.length() == 1 && specialChars.matcher(part).find()) {
                addMatchingPattern(part.charAt(0), patternBuilder);
            } else if (!part.isEmpty()) {
                patternBuilder.append(Pattern.quote(part));
            }
        }

        int flags = Pattern.UNICODE_CASE;
        if (!containsUppercase(subQuery)) {
            flags |= Pattern.CASE_INSENSITIVE;
        }

        return Pattern.compile(patternBuilder.toString(), flags);
    }

    private void addMatchingPattern (char c, StringBuilder patternBuilder) {
        switch (c) {
            case 'c':
                patternBuilder.append("[cç]");
                break;
            case 'a':
                patternBuilder.append("[aáàâä]");
                break;
            case 'e':
                patternBuilder.append("[eéèêë]");
                break;
            case 'i':
                patternBuilder.append("[iíìîï]");
                break;
            case 'o':
                patternBuilder.append("[oóòôö]");
                break;
            case 'u':
                patternBuilder.append("[uúùûü]");
                break;
            default:
                patternBuilder.append(Pattern.quote(String.valueOf(c)));
                break;
        }
    }

    /**
     * Checks if a string contains any uppercase characters.
     */
    private boolean containsUppercase(String s) {
        return s.chars().anyMatch(Character::isUpperCase);
    }

    /**
     * Calculates the score for a sub-query match in a stop name.
     */
    private int calculateScore(String subQuery, String stopName, int matchStart, int matchEnd) {
        // Calculate percentage of characters matching
        int baseScore = Math.max(1, (subQuery.length() * 100) / stopName.length());

        // Check if sub-query is at the beginning of a word
        boolean isWordStart = matchStart == 0 || !Character.isLetter(stopName.charAt(matchStart - 1));

        // Check if sub-query is at the end of a word
        boolean isWordEnd = matchEnd >= stopName.length() ||
                !Character.isLetter(stopName.charAt(matchEnd));

        // Apply multipliers
        int multiplier = 1;
        if (isWordStart) multiplier *= 4;
        if (isWordEnd) multiplier *= 2;

        return baseScore * multiplier;
    }
}
