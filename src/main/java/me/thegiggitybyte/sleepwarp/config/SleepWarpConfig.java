package me.thegiggitybyte.sleepwarp.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class SleepWarpConfig extends MidnightConfig {
    private static final String FEATURES = "features";
    private static final String TUNING = "tuning";
    @Entry(category = TUNING, min = 0) public static int max_ticks_added = 40;
    @Entry(category = TUNING, min = 0, max = 2, isSlider = true) public static double player_multiplier = 0.6d;
    @Entry(category = FEATURES) public static boolean action_bar_messages = true;
    @Entry(category = FEATURES) public static boolean use_sleep_percentage = false;
    @Comment(category = FEATURES, centered = true) public static Comment tick_simulation;
    @Entry(category = FEATURES) public static boolean tick_game_time = true;
    @Entry(category = FEATURES) public static boolean tick_block_entities = true;
    @Entry(category = FEATURES) public static boolean tick_random_block = true;
    @Entry(category = FEATURES) public static boolean tick_snow_accumulation = true;
    @Entry(category = FEATURES) public static boolean tick_ice_freezing = true;
    @Entry(category = FEATURES) public static boolean tick_lightning = true;
    @Entry(category = FEATURES) public static boolean tick_monsters = false;
    @Entry(category = FEATURES) public static boolean tick_animals = false;
    @Entry(category = TUNING, min = 0, max = 2, isSlider = true) public static double monster_tick_multiplier = 0.25d;
    @Entry(category = TUNING, min = 0, max = 2, isSlider = true) public static double animal_tick_multiplier = 0.25d;
    @Entry(category = TUNING) public static boolean log_error_messages = false;
}
