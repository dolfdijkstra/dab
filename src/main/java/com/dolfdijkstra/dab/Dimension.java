package com.dolfdijkstra.dab;

public class Dimension {
    private final int height;
    private final int width;

    /**
     * @param width
     * @param height
     */
    public Dimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }
}