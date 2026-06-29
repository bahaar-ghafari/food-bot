package com.foodbot.lang;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class Messages {
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final Map<String, String> EN = load("messages_en.json");
    private static final Map<String, String> FA = load("messages_fa.json");

    public static String get(Lang lang, String key, Object... args) {
        Map<String, String> catalog = lang == Lang.FA ? FA : EN;
        String template = catalog.getOrDefault(key, key);
        return args.length == 0 ? template : String.format(template, args);
    }

    public static Set<String> keys() {
        Set<String> all = new TreeSet<>(EN.keySet());
        all.addAll(FA.keySet());
        return all;
    }

    private static Map<String, String> load(String resourceName) {
        try (InputStream in = Messages.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + resourceName);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Map<String, String> map = new Gson().fromJson(reader, MAP_TYPE);
                return Collections.unmodifiableMap(map);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load " + resourceName, e);
        }
    }

    private Messages() {
    }
}
