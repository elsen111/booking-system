package com.bookingapi.domain.resource.service;

import com.bookingapi.common.dto.PageResponse;
import com.bookingapi.domain.resource.dto.CreateResourceRequest;
import com.bookingapi.domain.resource.dto.ResourceFilter;
import com.bookingapi.domain.resource.dto.ResourceResponse;
import com.bookingapi.domain.resource.dto.UpdateResourceRequest;
import com.bookingapi.security.AuthenticatedUser;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ResourceService {

    PageResponse<ResourceResponse> search(ResourceFilter filter, Pageable pageable, AuthenticatedUser currentUser);

    ResourceResponse getById(UUID id, AuthenticatedUser currentUser);

    ResourceResponse create(CreateResourceRequest request);

    ResourceResponse update(UUID id, UpdateResourceRequest request);
}