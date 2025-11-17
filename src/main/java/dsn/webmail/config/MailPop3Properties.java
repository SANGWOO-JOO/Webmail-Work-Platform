package dsn.webmail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mail.pop3")
public class MailPop3Properties {
    
    private String host = "pop.whoisworks.com";
    private int port = 995;
    private boolean ssl = true;
    private int maxFetch = 10;
    private long pollIntervalMs = 30000;
    private int connectionTimeoutMs = 10000;
    private int readTimeoutMs = 10000;

    public String host() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int port() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean ssl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public int maxFetch() {
        return maxFetch;
    }

    public void setMaxFetch(int maxFetch) {
        this.maxFetch = maxFetch;
    }

    public long pollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int connectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int readTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}