package com.gederin.cloud.licensing.controller;

import com.gederin.cloud.licensing.model.License;
import com.gederin.cloud.licensing.service.LicenseService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("v1/licenses/")
@RequiredArgsConstructor
public class LicenseServiceController {

    private final LicenseService licenseService;

    @GetMapping("/")
    public String echo() {
        return "echo";
    }

    @GetMapping("/all")
    public List<License> licenses() {
        return licenseService.getAllLicenses();
    }

    @GetMapping("/{organizationId}")
    public List<License> licensesByOrgId(@PathVariable String organizationId) {
        return licenseService.getLicensesByOrg(organizationId);
    }

    @GetMapping("license/{licenseId}")
    public License licenseById(@PathVariable String licenseId) {
        return licenseService.getLicense(licenseId);
    }
}