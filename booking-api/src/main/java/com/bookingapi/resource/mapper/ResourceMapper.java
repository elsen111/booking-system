package com.bookingapi.resource.mapper;

import com.bookingapi.resource.domain.Resource;
import com.bookingapi.resource.dto.CreateResourceRequest;
import com.bookingapi.resource.dto.ResourceResponse;
import com.bookingapi.resource.dto.UpdateResourceRequest;
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