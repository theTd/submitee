package org.starrel.submitee.language;

import lombok.Builder;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

class LanguageMatcher {
    private final List<LanguageMatcherEntry> matchers = new LinkedList<>();
    private final Comparator<Properties> priorityComparator = new Comparator<Properties>() {
        @Override
        public int compare(Properties o1, Properties o2) {
            return getPriority(o2) - getPriority(o1);
        }

        int getPriority(Properties p) {
            try {
                return Integer.parseInt(p.get("priority") + "");
            } catch (Exception e) {
                return 0;
            }
        }
    };

    private int highestHit = 0;

    public LanguageMatcher(Map<String, Properties> loaded) {
        for (Map.Entry<String, Properties> entry : loaded.entrySet()) {
            matchers.add(LanguageMatcherEntry.builder()
                    .pathIterator(Arrays.stream(entry.getKey().split("-")).iterator())
                    .path(entry.getKey())
                    .props(entry.getValue()).build());
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
                return priorityComparator.compare(o1.props, o2.props);
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
        Properties props;
        @Builder.Default
        int hit = 0;
    }
}
