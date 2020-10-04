package me.TechsCode.SpigotAPI.data;

import com.google.gson.JsonObject;

import java.util.Optional;

public class Purchase extends JsonSerializable {

    private String resourceId;
    private User user;
    private Time time;
    private Cost cost;

    public Purchase(String resourceId, User user, Time time, Cost cost) {
        this.resourceId = resourceId;
        this.user = user;
        this.time = time;
        this.cost = cost;
    }

    public Purchase(JsonObject state){
        setState(state);
    }

    public Resource getResource(){
        return dataset.getResources().id(resourceId).orElse(null);
    }

    public User getUser() {
        return user;
    }

    public Time getTime() {
        return time;
    }

    public Optional<Cost> getCost() {
        return Optional.ofNullable(cost);
    }

    public boolean isPurchased(){
        return cost != null;
    }

    public boolean isGifted(){
        return cost == null;
    }

    @Override
    public void setState(JsonObject jsonObject) {
        this.resourceId = jsonObject.get("resourceId").getAsString();
        this.user = new User(jsonObject.getAsJsonObject("user"));
        this.time = new Time(jsonObject.getAsJsonObject("time"));
        this.cost = jsonObject.has("cost") ? new Cost(jsonObject.getAsJsonObject("cost")) : null;
    }

    @Override
    public JsonObject getState() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("resourceId", resourceId);
        jsonObject.add("user", user.toJsonObject());
        jsonObject.add("time", time.toJsonObject());
        if(cost != null) jsonObject.add("cost", cost.toJsonObject());
        return jsonObject;
    }
}