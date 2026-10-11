package com.bookingapi.resource.controller;

import com.bookingapi.common.dto.ApiResponse;
import com.bookingapi.common.dto.PageResponse;
import com.bookingapi.resource.dto.CreateResourceRequest;
import com.bookingapi.resource.dto.ResourceFilter;
import com.bookingapi.resource.dto.ResourceResponse;
import com.bookingapi.resource.dto.UpdateResourceRequest;
import com.bookingapi.resource.service.ResourceService;
import com.bookingapi.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Resources")
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @Operation(summary = "List resources (paginated, filterable). Non-admins see active resources only.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ResourceResponse>>> search(
            @ParameterObject ResourceFilter filter,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(resourceService.search(filter, pageable, currentUser)).toResponseEntity();
    }

    @Operation(summary = "Get a resource by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceResponse>> getById(@PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(resourceService.getById(id, currentUser)).toResponseEntity();
    }

    @Operation(summary = "Create a resource (ADMIN)")
    @PostMapping
    public ResponseEntity<ApiResponse<ResourceResponse>> create(@Valid @RequestBody CreateResourceRequest request) {
        return ApiResponse.created("Resource created", resourceService.create(request)).toResponseEntity();
    }

    @Operation(summary = "Update a resource, or deactivate it with active=false (ADMIN)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateResourceRequest request) {
        return ApiResponse.ok("Resource updated", resourceService.update(id, request)).toResponseEntity();
    }
}