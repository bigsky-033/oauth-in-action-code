package org.oauthinaction.ch15ex01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.Map;

/** Only HTTP setup is shared. Each controller contains its own protocol behavior. */
public final class WebServer implements AutoCloseable {
    private final ServletWebServerApplicationContext context;

    private WebServer(ServletWebServerApplicationContext context) { this.context = context; }

    public static WebServer start(int port, String role, Object controller) {
        var application = new SpringApplication(HttpConfiguration.class);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        application.setDefaultProperties(Map.of(
                "server.address", "localhost",
                "server.port", port,
                "server.servlet.session.cookie.name", "CH15EX01_" + role.toUpperCase(),
                "spring.web.resources.static-locations", "classpath:/web/ch15ex01/" + role + "/",
                "spring.application.name", "ch15ex01-" + role,
                "spring.main.banner-mode", "off"));
        // Register just this server's controller; scanning would mix the three roles.
        application.addInitializers(ctx -> ctx.getBeanFactory().registerSingleton("protocolController", controller));
        return new WebServer((ServletWebServerApplicationContext) application.run());
    }

    public URI uri() { return URI.create("http://localhost:" + context.getWebServer().getPort() + "/"); }

    @Override public void close() { context.close(); }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class HttpConfiguration {}
}
