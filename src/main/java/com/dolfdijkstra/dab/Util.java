package com.dolfdijkstra.dab;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;

import org.apache.commons.lang3.StringUtils;

public class Util {
    public static int toSecs(String s) {
        String[] p = s.split(":");
        if (p.length == 1)
            p = ("0:0:" + s).split(":");
        else if (p.length == 2)
            p = ("0:" + s).split(":");
        if (p.length == 3) {
            Duration d = Duration.ofSeconds(parseInt(p[2]))
                    .plusMinutes(parseInt(p[1])).plusHours(parseInt(p[0]));
            return new Long(d.getSeconds()).intValue();
        } else
            throw new IllegalArgumentException(s);

    }

    /**
     * @param i
     * @return zero if {@code i} is blank, otherwise the result of {@link Integer#parseInt(String)}
     */
    public static int parseInt(String i) {
        if (StringUtils.isBlank(i))
            return 0;
        return Integer.parseInt(i);
    }

    public static void closeSilent(Closeable c) {
        try {
            c.close();
        } catch (IOException e) {
            // ignore
        }
    
    }

}
