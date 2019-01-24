package com.gederin.cloud.licensing.repository;


import com.gederin.cloud.licensing.model.License;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LicenseRepository extends CrudRepository<License,String>  {
    List<License> findByOrganizationId(String organizationId);

    License findByLicenseId(String licenseId);
}