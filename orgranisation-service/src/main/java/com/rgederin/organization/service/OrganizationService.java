package com.rgederin.organization.service;

import com.rgederin.organization.model.Organization;
import com.rgederin.organization.repository.OrganizationRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public Optional<Organization> getOrganizationById(String organizationId) {
        return organizationRepository.findById(organizationId);
    }

    public List<Organization> getAllOrganisations() {
        return (List<Organization>) organizationRepository.findAll();
    }
}
