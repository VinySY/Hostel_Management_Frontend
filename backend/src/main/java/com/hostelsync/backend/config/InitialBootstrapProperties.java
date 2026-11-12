package com.hostelsync.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for one-time production bootstrap.
 * Disabled by default; enabled explicitly via ENABLE_INITIAL_BOOTSTRAP=true.
 */
@Component
@ConfigurationProperties(prefix = "app.bootstrap")
public class InitialBootstrapProperties {

    private boolean enabled = false;
    private HostelProps hostel = new HostelProps();
    private DgmProps dgm = new DgmProps();
    private WardenProps warden = new WardenProps();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public HostelProps getHostel() {
        return hostel;
    }

    public void setHostel(HostelProps hostel) {
        this.hostel = hostel != null ? hostel : new HostelProps();
    }

    public DgmProps getDgm() {
        return dgm;
    }

    public void setDgm(DgmProps dgm) {
        this.dgm = dgm != null ? dgm : new DgmProps();
    }

    public WardenProps getWarden() {
        return warden;
    }

    public void setWarden(WardenProps warden) {
        this.warden = warden != null ? warden : new WardenProps();
    }

    public static class HostelProps {
        private String name;
        private String address;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

    public static class DgmProps {
        private String name;
        private String email;
        private String password;
        private String employeeId;
        private String designation;
        private String officeLocation;
        private String contactExtension;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }

        public String getDesignation() {
            return designation;
        }

        public void setDesignation(String designation) {
            this.designation = designation;
        }

        public String getOfficeLocation() {
            return officeLocation;
        }

        public void setOfficeLocation(String officeLocation) {
            this.officeLocation = officeLocation;
        }

        public String getContactExtension() {
            return contactExtension;
        }

        public void setContactExtension(String contactExtension) {
            this.contactExtension = contactExtension;
        }
    }

    public static class WardenProps {
        private String name;
        private String email;
        private String password;
        private String employeeId;
        private String phone;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }
}

