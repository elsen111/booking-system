package com.bookingapi.domain.resource.dto;

public record ResourceFilter(
        String name,
        Integer minCapacity,
        Boolean active
) {

    public ResourceFilter onlyActive() {
        return new ResourceFilter(name, minCapacity, true);
    }
}