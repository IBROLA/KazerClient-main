package com.cheatbreaker.client.util;

public class Vec2d {
    public double x;
    public double y;

    public Vec2d() {
        this(0.0, 0.0);
    }

    public Vec2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
