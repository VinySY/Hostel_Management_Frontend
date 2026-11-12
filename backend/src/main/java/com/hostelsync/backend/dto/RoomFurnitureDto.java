package com.hostelsync.backend.dto;

public class RoomFurnitureDto {

    private String type;         // "BED", "STUDY_TABLE", "LOCKER"
    private int required;
    private int serviceable;
    private int damaged;
    private int missing;
    private int underRepair;
    private int shortage;

    public RoomFurnitureDto() {}

    public RoomFurnitureDto(String type, int required, int serviceable, int damaged, int missing, int underRepair, int shortage) {
        this.type = type;
        this.required = required;
        this.serviceable = serviceable;
        this.damaged = damaged;
        this.missing = missing;
        this.underRepair = underRepair;
        this.shortage = shortage;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getRequired() { return required; }
    public void setRequired(int required) { this.required = required; }

    public int getServiceable() { return serviceable; }
    public void setServiceable(int serviceable) { this.serviceable = serviceable; }

    public int getDamaged() { return damaged; }
    public void setDamaged(int damagedCount) { this.damaged = damagedCount; }

    public int getMissing() { return missing; }
    public void setMissing(int missingCount) { this.missing = missingCount; }

    public int getUnderRepair() { return underRepair; }
    public void setUnderRepair(int underRepair) { this.underRepair = underRepair; }

    public int getShortage() { return shortage; }
    public void setShortage(int shortage) { this.shortage = shortage; }
}
