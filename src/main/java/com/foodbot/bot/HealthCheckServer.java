package com.foodbot.bot;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class HealthCheckServer {

    public static void startIfConfigured() {
        String addr = System.getenv("HEALTH_ADDR");
        if (addr == null || addr.isBlank()) {
            return;
        }
        int port = Integer.parseInt(addr.substring(addr.lastIndexOf(':') + 1));
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/healthz", exchange -> {
                byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HealthCheckServer() {
    }
}
