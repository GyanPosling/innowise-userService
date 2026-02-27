package com.innowise.userservice.repository.specification;

import com.innowise.userservice.model.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

public interface PaymentCardSpecification {

    Specification<PaymentCard> hasHolder(String holder);
}