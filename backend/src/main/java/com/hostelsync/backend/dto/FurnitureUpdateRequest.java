package com.hostelsync.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FurnitureUpdateRequest {

    @NotBlank(message = "Furniture type is required")
    private String furnitureType; // "BED", "STUDY_TABLE", "LOCKER"

    @NotNull
    @Min(0)
    private Integer serviceableCount;

    @NotNull
    @Min(0)
    private Integer damagedCount;

    @NotNull
    @Min(0)
    private Integer missingCount;

    @NotNull
    @Min(0)
    private Integer underRepairCount;

    /** "DAMAGE_REPORTED" | "REPAIRED" | "REPLACED" | "INVENTORY_AUDIT" */
    private String actionType = "INVENTORY_AUDIT";

    private String notes;

    public FurnitureUpdateRequest() {}

    public String getFurnitureType() { return furnitureType; }
    public void setFurnitureType(String furnitureType) { this.furnitureType = furnitureType; }

    public Integer getServiceableCount() { return serviceableCount; }
    public void setServiceableCount(Integer serviceableCount) { this.serviceableCount = serviceableCount; }

    public Integer getDamagedCount() { return damagedCount; }
    public void setDamagedCount(Integer damagedCount) { this.damagedCount = damagedCount; }

    public Integer getMissingCount() { return missingCount; }
    public void setMissingCount(Integer missingCount) { this.missingCount = missingCount; }

    public Integer getUnderRepairCount() { return underRepairCount; }
    public void setUnderRepairCount(Integer underRepairCount) { this.underRepairCount = underRepairCount; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
