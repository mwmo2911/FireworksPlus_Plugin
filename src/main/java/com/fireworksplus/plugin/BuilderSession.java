package com.fireworksplus.plugin;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory GUI builder state for a single player.
 * This is intentionally mutable (unlike DraftShow).
 */
public class BuilderSession {

    public String name = "myshow";

    public final List<Location> points = new ArrayList<>();

    public int durationSeconds = 30;
    public int intervalTicks = 6;
    public double radius = 1.6;

    public int powerMin = 1;
    public int powerMax = 2;

    public List<String> palette = new ArrayList<>();
}
