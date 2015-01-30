package com.dolfdijkstra.dab;

import java.time.Duration;

import org.apache.commons.lang.StringUtils;

public class DurationTest {

    public static void main(String[] args) {
        System.out.println(Duration.ofSeconds(62).toString());
        System.out.println(Duration.ofSeconds(620).toString());

        System.out.println(Duration.ofSeconds(3662).toString());
        toSecs("20:30:01");
        toSecs("2:30:01");
        toSecs("30:01");
        toSecs("0:01");
        toSecs("01");
        toSecs(":01");
        toSecs("01:");
    }

    public static long toSecs(String s) {
        String[] p = s.split(":");
        if (p.length == 1)
            p = ("0:0:" + s).split(":");
        else if (p.length == 2)
            p = ("0:" + s).split(":");
        if (p.length == 3) {
            Duration d = Duration.ofSeconds(parseInt(p[2]))
                    .plusMinutes(parseInt(p[1])).plusHours(parseInt(p[0]));
            System.out.println(d.toString());
            return d.getSeconds();
        } else
            throw new IllegalArgumentException(s);

    }

    public static int parseInt(String v) {
        if (StringUtils.isBlank(v))
            return 0;
        return Integer.parseInt(v);
    }
}
