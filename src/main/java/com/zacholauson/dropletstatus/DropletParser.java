package com.zacholauson.dropletstatus;

import org.json.JSONException;
import org.json.JSONObject;

public class DropletParser {
    private JSONObject dropletObject;

    public DropletParser(JSONObject dropletObject) {
        this.dropletObject = dropletObject;
    }

    public String getName() throws JSONException {
        return dropletObject.getString("name");
    }

    public String getStatus() throws JSONException {
        return dropletObject.getString("status");
    }
}
