package stew6.ui.fx;

import java.util.*;

final class CommandHistory {

    private List<String> histories;
    private int currentIndex;

    CommandHistory() {
        this.histories = new ArrayList<>();
        this.currentIndex = 0;
    }

    void add(String cmd) {
        histories.add(cmd);
        resetIndex();
    }

    String current() {
        return histories.get(currentIndex);
    }

    String prev() {
        if (histories.isEmpty()) {
            return "";
        }
        --currentIndex;
        if (currentIndex < 0) {
            resetIndex();
        }
        return histories.get(currentIndex);
    }

    String next() {
        if (histories.isEmpty()) {
            return "";
        }
        ++currentIndex;
        if (currentIndex >= histories.size()) {
            currentIndex = 0;
        }
        return histories.get(currentIndex);
    }

    private void resetIndex() {
        currentIndex = histories.isEmpty() ? 0 : histories.size() - 1;
    }

}
