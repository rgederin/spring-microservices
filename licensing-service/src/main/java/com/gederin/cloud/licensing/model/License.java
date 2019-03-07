package com.gederin.cloud.licensing.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Data;

@Entity
@Table(name = "licenses")
@Data
public class License {
    @Id
    @Column(name = "license_id", nullable = false)
    private String licenseId;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "license_type", nullable = false)
    private String licenseType;

    @Column(name = "license_max", nullable = false)
    private Integer licenseMax;

    @Column(name = "license_allocated", nullable = false)
    private Integer licenseAllocated;

    @Column(name = "comment")
    private String comment;

    @Transient
    private String organizationName = "";

    @Transient
    private String contactName = "";

    @Transient
    private String contactPhone = "";

    @Transient
    private String contactEmail = "";

    public License withId(String licenseId){
        this.setLicenseId(licenseId);
        return this;
    }

    public License withOrganizationId(String organizationId){
        this.setOrganizationId(organizationId);
        return this;
    }

    public License withProductName(String productName){
        this.setProductName(productName);
        return this;
    }

    public License withLicenseType(String licenseType){
        this.setLicenseType(licenseType);
        return this;
    }

    public License withLicenseMax(Integer licenseMax){
        this.setLicenseMax(licenseMax);
        return this;
    }

    public License withLicenseAllocated(Integer licenseAllocated){
        this.setLicenseAllocated(licenseAllocated);
        return this;
    }

    public License withComment(String comment){
        this.setComment(comment);
        return this;
    }

    public License withOrganizationName(String organizationName){
        this.setOrganizationName(organizationName);
        return this;
    }

    public License withContactName(String contactName){
        this.setContactName(contactName);
        return this;
    }

    public License withContactPhone(String contactPhone){
        this.setContactPhone(contactPhone);
        return this;
    }

    public License withContactEmail(String contactEmail){
        this.setContactEmail(contactEmail);
        return this;
    }
}