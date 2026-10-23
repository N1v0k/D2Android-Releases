package com.winlator.d2;

import java.util.ArrayList;
import java.util.List;






public final class D2Changelog {
    public static final String ASSET = "news/CHANGELOG.md";

    private D2Changelog() {}

    public static final class Section {
        public final String version, date;
        public final List<String> items = new ArrayList<>();

        Section(String version, String date) {
            this.version = version;
            this.date = date;
        }
    }

    public static List<Section> parse(String text) {
        List<Section> sections = new ArrayList<>();
        Section current = null;
        boolean comment = false;
        for (String raw : text.split("\r?\n")) {
            String line = raw.trim();
            if (comment) {
                if (line.contains("-->")) comment = false;
                continue;
            }
            if (line.startsWith("<!--")) {
                comment = !line.contains("-->");
                continue;
            }
            if (line.startsWith("## ")) {
                String head = line.substring(3).trim(), date = "";
                int open = head.indexOf('(');
                if (open > 0 && head.endsWith(")")) {
                    date = head.substring(open + 1, head.length() - 1).trim();
                    head = head.substring(0, open).trim();
                }
                current = new Section(head, date);
                sections.add(current);
            }
            else if (current != null && (line.startsWith("- ") || line.startsWith("* "))) current.items.add(line.substring(2).trim());
            else if (current != null && !line.isEmpty() && !line.startsWith("#") && !current.items.isEmpty()) {

                int last = current.items.size() - 1;
                current.items.set(last, current.items.get(last) + " " + line);
            }
        }
        return sections;
    }
}
