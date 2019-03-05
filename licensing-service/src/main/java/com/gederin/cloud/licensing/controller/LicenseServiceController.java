package com.gederin.cloud.licensing.controller;

import com.gederin.cloud.licensing.model.License;
import com.gederin.cloud.licensing.service.LicenseService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("v1/licenses/")
@RequiredArgsConstructor
@Slf4j
public class LicenseServiceController {

    private final LicenseService licenseService;

    @GetMapping("/")
    public String echo() {
        return "licensingservice echo";
    }

    @GetMapping("/all")
    public List<License> licenses() {
        return licenseService.getAllLicenses();
    }

    @GetMapping("/{organizationId}")
    public List<License> licensesByOrganisationId(@PathVariable String organizationId) {
        return licenseService.getLicensesByOrg(organizationId);
    }

    @GetMapping("license/{licenseId}")
    public License licenseById(@PathVariable String licenseId) {
        return licenseService.getLicense(licenseId);
    }

    @GetMapping("license/{licenseId}/{clientType}")
    public License licenseByIdWithOrganizationInfo(@PathVariable String licenseId, @PathVariable String clientType) {
        return licenseService.getLicenseWithOrganizationInfo(licenseId, clientType);
    }
}