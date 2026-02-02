package com.fireworksplus.plugin;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class DraftShow {
    public final String name;
    public final List<Location> points = new ArrayList<>();

    public int durationSeconds = 30;
    public int intervalTicks = 6;
    public double radius = 1.6;

    public int powerMin = 1;
    public int powerMax = 2;

    public List<String> fireworkTypes = new ArrayList<>(List.of(org.bukkit.FireworkEffect.Type.BALL.name()));

    public List<String> palette = new ArrayList<>();

    public List<String> trailParticles = new ArrayList<>();

    public boolean particleTrail = false;

    public DraftShow(String name) {
        this.name = name;
    }
}