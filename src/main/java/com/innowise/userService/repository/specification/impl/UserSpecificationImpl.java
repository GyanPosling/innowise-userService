package com.innowise.userservice.repository.specification.impl;

import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.repository.specification.UserSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class UserSpecificationImpl implements UserSpecification {

    @Override
    public Specification<User> hasFirstName(String firstName) {
        return (root, query, criteriaBuilder) ->
                firstName == null ? null : criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + firstName.toLowerCase() + "%"
                );
    }

    @Override
    public Specification<User> hasLastName(String lastName) {
        return (root, query, criteriaBuilder) ->
                lastName == null ? null : criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("surname")),
                        "%" + lastName.toLowerCase() + "%"
                );
    }
}
