package uz.syncoder.generator.read;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Problems {

    private final List<String> items = new ArrayList<>();

    public void add(String where, String message) {
        items.add(where + ": " + message);
    }

    public boolean any() {
        return !items.isEmpty();
    }

    public String report() {
        return items.stream()
                .sorted()
                .collect(Collectors.joining("\n  - ",
                        "Found " + items.size() + " content problem(s):\n  - ", ""));
    }
}
