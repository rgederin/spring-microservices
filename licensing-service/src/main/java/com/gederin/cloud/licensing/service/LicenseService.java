
package com.gederin.cloud.licensing.service;

import com.gederin.cloud.licensing.model.License;
import com.gederin.cloud.licensing.repository.LicenseRepository;

import org.springframework.stereotype.Service;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LicenseService {
    private final LicenseRepository licenseRepository;

    public List<License> getAllLicenses (){
        return (List<License>) licenseRepository.findAll();
    }

    public License getLicense(String licenseId) {
        return licenseRepository.findById(licenseId).get();
    }

    public List<License> getLicensesByOrg(String organizationId) {
        return licenseRepository.findByOrganizationId(organizationId);
    }
}