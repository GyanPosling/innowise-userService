package com.innowise.userService.repository.specification;

import com.innowise.userService.model.entity.User;
import org.springframework.data.jpa.domain.Specification;

public interface UserSpecification {

    Specification<User> hasFirstName(String firstName);

    Specification<User> hasLastName(String lastName);
}
