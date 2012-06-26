package com.dolfdijkstra.dab.snmp;

public class NVP {
    private final String name;
    private final double value;

    public NVP(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public NVP(String name, double value) {
        this.name = name;
        this.value = value;
    }

    public NVP(String name, long value) {
        this.name = name;
        this.value = value;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the value
     */
    public double getValue() {
        return value;
    }

}
