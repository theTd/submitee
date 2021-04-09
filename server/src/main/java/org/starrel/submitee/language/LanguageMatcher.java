package org.starrel.submitee.language;

import lombok.Builder;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

class LanguageMatcher {
    private final List<LanguageMatcherEntry> matchers = new LinkedList<>();
    private final Comparator<Map<String, String>> priorityComparator = new Comparator<Map<String, String>>() {
        @Override
        public int compare(Map<String, String> o1, Map<String, String> o2) {
            return getPriority(o2) - getPriority(o1);
        }

        int getPriority(Map<String, String> p) {
            String val = p.get("priority");
            if (val == null) return -1;
            int intVal;
            try {
                intVal = Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return -1;
            }
            return intVal;
        }
    };

    private int highestHit = 0;

    public LanguageMatcher(Map<String, Map<String, String>> loaded) {
        for (Map.Entry<String, Map<String, String>> entry : loaded.entrySet()) {
            matchers.add(LanguageMatcherEntry.builder()
                    .pathIterator(Arrays.stream(entry.getKey().split("-")).iterator())
                    .path(entry.getKey())
                    .entires(entry.getValue()).build());
        }
    }

    public void next(String match) {
        for (LanguageMatcherEntry matcher : matchers) {
            if (matcher.pathIterator.hasNext() && matcher.pathIterator.next().equals(match)) {
                matcher.hit++;
                this.highestHit = Math.max(this.highestHit, matcher.hit);
            }
        }
    }

    public List<String> end() {
        matchers.sort((o1, o2) -> {
            int diff = o2.hit - o1.hit;
            if (diff == 0) {
                return priorityComparator.compare(o1.entires, o2.entires);
            } else {
                return diff;
            }
        });
        return matchers.stream().map(LanguageMatcherEntry::getPath).collect(Collectors.toList());
    }

    @Data
    @Builder
    private static class LanguageMatcherEntry {
        Iterator<String> pathIterator;
        String path;
        Map<String, String> entires;
        @Builder.Default
        int hit = 0;
    }
}
