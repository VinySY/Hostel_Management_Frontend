package com.hostelsync.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for public read-only Showcase Demo Mode.
 * Both demo login and demo sandbox seeding default to disabled.
 */
@Component
@ConfigurationProperties(prefix = "app.demo")
public class DemoProperties {

    private boolean enabled = false;
    private boolean seedEnabled = false;
    private String hostelName = "HostelSync Showcase Hostel (Demo)";
    private String hostelAddress = "Showcase Demo Wing, University Campus";
    private String studentEmail = "demo.student@demo.hostelsync.local";
    private String wardenEmail = "demo.warden@demo.hostelsync.local";
    private String dgmEmail = "demo.dgm@demo.hostelsync.local";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSeedEnabled() {
        return seedEnabled;
    }

    public void setSeedEnabled(boolean seedEnabled) {
        this.seedEnabled = seedEnabled;
    }

    public String getHostelName() {
        return hostelName;
    }

    public void setHostelName(String hostelName) {
        this.hostelName = hostelName;
    }

    public String getHostelAddress() {
        return hostelAddress;
    }

    public void setHostelAddress(String hostelAddress) {
        this.hostelAddress = hostelAddress;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getWardenEmail() {
        return wardenEmail;
    }

    public void setWardenEmail(String wardenEmail) {
        this.wardenEmail = wardenEmail;
    }

    public String getDgmEmail() {
        return dgmEmail;
    }

    public void setDgmEmail(String dgmEmail) {
        this.dgmEmail = dgmEmail;
    }
}

