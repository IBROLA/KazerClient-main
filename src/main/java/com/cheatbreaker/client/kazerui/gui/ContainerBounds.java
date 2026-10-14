package com.cheatbreaker.client.kazerui.gui;

/** Typed native GUI bounds: no reflective field reads in a draw loop. */
public interface ContainerBounds {
    int kazerLeft();
    int kazerTop();
    int kazerWidth();
}
