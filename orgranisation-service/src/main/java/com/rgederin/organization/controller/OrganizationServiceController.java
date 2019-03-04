package com.rgederin.organization.controller;

import com.rgederin.organization.model.Organization;
import com.rgederin.organization.service.OrganizationService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("v1/organizations")
@RequiredArgsConstructor
public class OrganizationServiceController {

    private final OrganizationService organizationService;

    @GetMapping("/echo")
    public String echo() {
        return "organisations echo";
    }

    @GetMapping("/{organizationId}")
    public Organization getOrganizationbyId(@PathVariable("organizationId") String organizationId) {
        return organizationService.getOrganizationById(organizationId).get();
    }

    @GetMapping("/all")
    public List<Organization> getAllOrganisations() {
        return organizationService.getAllOrganisations();
    }
}