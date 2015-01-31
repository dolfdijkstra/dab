package com.dolfdijkstra.dab;

import java.util.Formatter;
import java.util.Locale;

class StringAppender {
    private final StringBuilder b = new StringBuilder();
    private final static String EOL = "\n";
    private final Formatter f;

    public StringAppender() {
        f = new Formatter(b, Locale.US);
    }

    public StringAppender line(String format, Object... args) {
        f.format(format, args);
        return line();
    }

    public StringAppender line() {
        b.append(EOL);
        return this;
    }

    @Override
    public String toString() {
        return b.toString();
    }
}