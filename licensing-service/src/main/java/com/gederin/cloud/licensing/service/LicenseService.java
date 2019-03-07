
package com.gederin.cloud.licensing.service;

import com.gederin.cloud.licensing.client.OrganizationDiscoveryClient;
import com.gederin.cloud.licensing.client.OrganizationFeignClient;
import com.gederin.cloud.licensing.client.OrganizationRestTemplateClient;
import com.gederin.cloud.licensing.model.License;
import com.gederin.cloud.licensing.model.Organization;
import com.gederin.cloud.licensing.repository.LicenseRepository;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LicenseService {
    private final LicenseRepository licenseRepository;

    private final OrganizationDiscoveryClient discoveryClient;

    private final OrganizationRestTemplateClient restTemplateClient;

    private final OrganizationFeignClient feignClient;

    @HystrixCommand
    public List<License> getAllLicenses() {
        randomlyRunLong();

        return (List<License>) licenseRepository.findAll();
    }

    @HystrixCommand(fallbackMethod = "buildFallbackLicense")
    public License getLicense(String licenseId) {
        randomlyRunLong();

        return licenseRepository.findById(licenseId).get();
    }

    @HystrixCommand(commandProperties = @HystrixProperty(
            name = "execution.isolation.thread.timeoutInMilliseconds",
            value = "15000"))
    public License getLicenseWithOrganizationInfo(String licenseId, String clientType) {
        randomlyRunLong();

        License license = licenseRepository.findById(licenseId).get();

        Organization organization = retrieveOrgInfo(license.getOrganizationId(), clientType);

        return license
                .withOrganizationName(organization.getName())
                .withContactName(organization.getContactName())
                .withContactEmail(organization.getContactEmail())
                .withContactPhone(organization.getContactPhone());
    }

    @HystrixCommand(
            threadPoolKey = "licensesByOrgThreadPool",
            threadPoolProperties =
                    {@HystrixProperty(name = "coreSize", value = "30"),
                            @HystrixProperty(name = "maxQueueSize", value = "10")},
            commandProperties = {
                    @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10"),
                    @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "75"),
                    @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "7000"),
                    @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "15000"),
                    @HystrixProperty(name = "metrics.rollingStats.numBuckets", value = "5")}
    )
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

    private License buildFallbackLicense(String licenseId) {
        return new License()
                .withId(licenseId)
                .withProductName("Sorry no licensing information currently available");
    }

    private void randomlyRunLong() {
        Random rand = new Random();

        int randomNum = rand.nextInt(3 - 1 + 1) + 1;

        if (randomNum == 3) sleep();
    }

    private void sleep() {
        try {
            Thread.sleep(11000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}