package com.ppm.integration.agilesdk.connector.agilecentral.model;

import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class User extends Entity {

    private List<HierarchicalRequirement> hierarchicalRequirements = new ArrayList<HierarchicalRequirement>();

    public User(JSONObject jsonObject) {
        super(jsonObject);
    }

    public void addHierarchicalRequirement(HierarchicalRequirement hierarchicalRequirement) {
        if (hierarchicalRequirement.getOwnerUUID() != null && this.getUUID()
                .equals(hierarchicalRequirement.getOwnerUUID())) {
            hierarchicalRequirement.setUser(this);
            this.hierarchicalRequirements.add(hierarchicalRequirement);
        }
    }

    public String getEmailAddress() {
        return (check("EmailAddress") ? jsonObject.getString("EmailAddress") : null);
    }

    public String getRole() {
        return (check("Role") ? jsonObject.getString("Role") : null);
    }

}