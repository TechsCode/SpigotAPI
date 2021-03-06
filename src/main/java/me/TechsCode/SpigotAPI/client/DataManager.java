package me.TechsCode.SpigotAPI.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import me.TechsCode.SpigotAPI.data.Dataset;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class DataManager implements Runnable {

    private static final long REFRESH_DELAY = TimeUnit.MINUTES.toMillis(1);
    private long lastParsed = 0L;

    private final String url, token;

    private final Thread thread;

    private Dataset dataset;

    public DataManager(String url, String token) {
        this.url = url;
        this.token = token;

        this.thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        while(true) {
            try {
                String json = IOUtils.toString(new URI(url + "/?token=" + token), StandardCharsets.UTF_8);

                JsonParser parser = new JsonParser();

                try {
                    JsonObject jsonObject = (JsonObject) parser.parse(json);
                    this.dataset = new Dataset(jsonObject);
                    this.lastParsed = System.currentTimeMillis();
                } catch (JsonParseException e) {
                    System.err.println("Server responded with '" + json + "'");
                }

            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(REFRESH_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Dataset getData(){
        return dataset;
    }

    public long getLastParsed() {
        return lastParsed;
    }
}


