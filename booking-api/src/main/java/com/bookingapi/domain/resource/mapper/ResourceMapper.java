package com.bookingapi.domain.resource.mapper;

import com.bookingapi.domain.resource.entity.Resource;
import com.bookingapi.domain.resource.dto.CreateResourceRequest;
import com.bookingapi.domain.resource.dto.ResourceResponse;
import com.bookingapi.domain.resource.dto.UpdateResourceRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface ResourceMapper {

    ResourceResponse toResponse(Resource resource);

    @Mapping(target = "active", constant = "true")
    Resource toEntity(CreateResourceRequest request);

    void updateEntity(UpdateResourceRequest request, @MappingTarget Resource resource);
}