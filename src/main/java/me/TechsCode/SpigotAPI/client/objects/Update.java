package me.TechsCode.SpigotAPI.client.objects;

import me.TechsCode.SpigotAPI.client.SpigotAPIClient;
import org.json.simple.JSONObject;

import java.util.Optional;

public class Update extends APIObject {

    public Update(SpigotAPIClient client, JSONObject jsonObject) {
        super(client, jsonObject);
    }

    public Optional<Resource> getResource(){
        return client.getResources().id(getResourceId());
    }

    public String getUpdateId() {
        return getStringProperty("updateId");
    }

    public String getResourceId() {
        return getStringProperty("resourceId");
    }

    public String getResourceName() {
        return getStringProperty("resourceName");
    }

    public String getTitle() {
        return getStringProperty("title");
    }

    public String getDescription() {
        return getStringProperty("description");
    }

    public Time getTime() {
        return new Time(getStringProperty("time"), getLongProperty("unixTime"));
    }

}
