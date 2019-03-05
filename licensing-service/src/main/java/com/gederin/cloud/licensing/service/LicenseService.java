
package com.gederin.cloud.licensing.service;

import com.gederin.cloud.licensing.client.OrganizationDiscoveryClient;
import com.gederin.cloud.licensing.client.OrganizationFeignClient;
import com.gederin.cloud.licensing.client.OrganizationRestTemplateClient;
import com.gederin.cloud.licensing.model.License;
import com.gederin.cloud.licensing.model.Organization;
import com.gederin.cloud.licensing.repository.LicenseRepository;

import org.springframework.stereotype.Service;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LicenseService {
    private final LicenseRepository licenseRepository;

    private final OrganizationDiscoveryClient discoveryClient;

    private final OrganizationRestTemplateClient restTemplateClient;

    private final OrganizationFeignClient feignClient;

    public List<License> getAllLicenses() {
        return (List<License>) licenseRepository.findAll();
    }

    public License getLicense(String licenseId) {
        return licenseRepository.findById(licenseId).get();
    }

    public License getLicenseWithOrganizationInfo(String licenseId, String clientType) {
        License license = licenseRepository.findById(licenseId).get();

        Organization organization = retrieveOrgInfo(license.getOrganizationId(), clientType);

        return license
                .withOrganizationName(organization.getName())
                .withContactName(organization.getContactName())
                .withContactEmail(organization.getContactEmail())
                .withContactPhone(organization.getContactPhone());
    }

    public List<License> getLicensesByOrg(String organizationId) {
        return licenseRepository.findByOrganizationId(organizationId);
    }

    private Organization retrieveOrgInfo(String organizationId, String clientType) {
        Organization organization;

        switch (clientType) {
            case "feign":
                System.out.println("I am using the feign client");
                organization = feignClient.getOrganization(organizationId);
                break;
            case "rest":
                System.out.println("I am using the rest client");
                organization = restTemplateClient.getOrganization(organizationId);
                break;
            case "discovery":
                System.out.println("using the discovery client");

                organization = discoveryClient.getOrganization(organizationId).get();
                break;
            default:
                organization = discoveryClient.getOrganization(organizationId).get();
        }

        return organization;
    }
}