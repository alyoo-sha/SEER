package dev.seer.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class SearchQuery {
    private enum Field { NAME, MOD, TOOLTIP, TAG }

    private record Term(Field field, boolean negate, Pattern regex, String text) {
        boolean matches(SeerStack s) {
            boolean hit = switch (field) {
                case NAME -> test(s.searchName());
                case MOD -> test(s.searchMod());
                case TOOLTIP -> test(s.searchTooltip());
                case TAG -> s.searchTags().stream().anyMatch(this::test);
            };
            return hit != negate;
        }

        private boolean test(String hay) {
            return regex != null ? regex.matcher(hay).find() : hay.contains(text);
        }
    }

    private final List<List<Term>> clauses; // И между группами, ИЛИ внутри группы

    private SearchQuery(List<List<Term>> clauses) { this.clauses = clauses; }

    public boolean isEmpty() { return clauses.isEmpty(); }

    public boolean test(SeerStack s) {
        outer:
        for (List<Term> alternatives : clauses) {
            for (Term t : alternatives) {
                if (t.matches(s)) continue outer;
            }
            return false;
        }
        return true;
    }

    public static SearchQuery parse(String input) {
        List<List<Term>> clauses = new ArrayList<>();
        if (input != null) {
            for (String token : input.trim().split("\\s+")) {
                if (token.isEmpty()) continue;
                List<Term> alts = new ArrayList<>();
                // regex-токены (/.../) не делим по '|'
                String[] parts = isRegexToken(token) ? new String[] { token } : token.split("\\|");
                for (String part : parts) {
                    Term t = parseTerm(part);
                    if (t != null) alts.add(t);
                }
                if (!alts.isEmpty()) clauses.add(alts);
            }
        }
        return new SearchQuery(clauses);
    }

    private static boolean isRegexToken(String token) {
        String s = token.startsWith("-") ? token.substring(1) : token;
        if (!s.isEmpty() && "@$#".indexOf(s.charAt(0)) >= 0) s = s.substring(1);
        return s.length() >= 2 && s.startsWith("/") && s.endsWith("/");
    }

    private static Term parseTerm(String raw) {
        boolean neg = raw.length() > 1 && raw.startsWith("-");
        if (neg) raw = raw.substring(1);
        Field field = Field.NAME;
        if (!raw.isEmpty()) {
            switch (raw.charAt(0)) {
                case '@' -> { field = Field.MOD; raw = raw.substring(1); }
                case '$' -> { field = Field.TOOLTIP; raw = raw.substring(1); }
                case '#' -> { field = Field.TAG; raw = raw.substring(1); }
                default -> { }
            }
        }
        if (raw.isEmpty()) return null;
        if (raw.length() >= 2 && raw.startsWith("/") && raw.endsWith("/")) {
            try {
                return new Term(field, neg, Pattern.compile(raw.substring(1, raw.length() - 1),
                        Pattern.CASE_INSENSITIVE), null);
            } catch (PatternSyntaxException ignored) {
                // недописанный regex при вводе: ищем как обычный текст
            }
        }
        return new Term(field, neg, null, raw.toLowerCase(Locale.ROOT));
    }
}
