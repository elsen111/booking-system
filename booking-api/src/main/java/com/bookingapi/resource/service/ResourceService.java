package com.bookingapi.resource.service;

import com.bookingapi.common.dto.PageResponse;
import com.bookingapi.resource.dto.CreateResourceRequest;
import com.bookingapi.resource.dto.ResourceFilter;
import com.bookingapi.resource.dto.ResourceResponse;
import com.bookingapi.resource.dto.UpdateResourceRequest;
import com.bookingapi.security.AuthenticatedUser;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ResourceService {

    PageResponse<ResourceResponse> search(ResourceFilter filter, Pageable pageable, AuthenticatedUser currentUser);

    ResourceResponse getById(UUID id, AuthenticatedUser currentUser);

    ResourceResponse create(CreateResourceRequest request);

    ResourceResponse update(UUID id, UpdateResourceRequest request);
}