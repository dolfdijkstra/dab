package com.dolfdijkstra.dab.snmp;

public class DS {
    public enum Type {
        COUNTER, GAUGE
    };

    private final String name;
    private final Type type;

    /**
     * @param name
     * @param type
     */
    public DS(String name, Type type) {
        super();
        this.name = name;
        this.type = type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type.toString();
    }
}
