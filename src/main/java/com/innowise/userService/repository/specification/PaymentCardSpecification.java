package com.innowise.userService.repository.specification;

import com.innowise.userService.model.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

public interface PaymentCardSpecification {

    Specification<PaymentCard> hasHolder(String holder);
}