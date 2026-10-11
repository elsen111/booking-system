package com.bookingapi.resource.service;

import com.bookingapi.common.exception.ConflictException;
import com.bookingapi.common.exception.NotFoundException;
import com.bookingapi.common.pagination.PageableSanitizer;
import com.bookingapi.common.dto.PageResponse;
import com.bookingapi.resource.domain.Resource;
import com.bookingapi.resource.dto.CreateResourceRequest;
import com.bookingapi.resource.dto.ResourceFilter;
import com.bookingapi.resource.dto.ResourceResponse;
import com.bookingapi.resource.dto.UpdateResourceRequest;
import com.bookingapi.resource.mapper.ResourceMapper;
import com.bookingapi.resource.repository.ResourceRepository;
import com.bookingapi.resource.repository.ResourceSpecifications;
import com.bookingapi.resource.service.ResourceService;
import com.bookingapi.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService {

    private static final Set<String> SORTABLE = Set.of("name", "capacity", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by("name");
    private static final String NAME_TAKEN = "A resource with this name already exists";

    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;

    @Override
    public PageResponse<ResourceResponse> search(ResourceFilter filter, Pageable pageable, AuthenticatedUser currentUser) {
        ResourceFilter effective = currentUser.isAdmin() ? filter : filter.onlyActive();
        Pageable safePageable = PageableSanitizer.sanitize(pageable, SORTABLE, DEFAULT_SORT);

        Page<Resource> page = resourceRepository.findAll(ResourceSpecifications.from(effective), safePageable);
        return PageResponse.from(page.map(resourceMapper::toResponse));
    }

    @Override
    public ResourceResponse getById(UUID id, AuthenticatedUser currentUser) {
        Resource resource = resourceRepository.findById(id)
                .filter(r -> r.isActive() || currentUser.isAdmin())
                .orElseThrow(() -> new NotFoundException("Resource not found"));
        return resourceMapper.toResponse(resource);
    }

    @Override
    @Transactional
    public ResourceResponse create(CreateResourceRequest request) {
        if (resourceRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException(NAME_TAKEN);
        }
        Resource resource = resourceMapper.toEntity(request);
        return resourceMapper.toResponse(resourceRepository.save(resource));
    }

    @Override
    @Transactional
    public ResourceResponse update(UUID id, UpdateResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found"));
        if (resourceRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new ConflictException(NAME_TAKEN);
        }

        resourceMapper.updateEntity(request, resource);

        return resourceMapper.toResponse(resourceRepository.saveAndFlush(resource));
    }
}