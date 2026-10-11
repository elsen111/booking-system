package com.bookingapi.resource.repository;

import com.bookingapi.resource.domain.Resource;
import com.bookingapi.resource.dto.ResourceFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ResourceSpecifications {

    private ResourceSpecifications() {
    }

    public static Specification<Resource> from(ResourceFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.name() != null && !filter.name().isBlank()) {
                String pattern = "%" + filter.name().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.<String>get("name")), pattern));
            }
            if (filter.minCapacity() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Integer>get("capacity"), filter.minCapacity()));
            }
            if (filter.active() != null) {
                predicates.add(cb.equal(root.get("active"), filter.active()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}