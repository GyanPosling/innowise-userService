package com.innowise.userService.repository.specification.impl;



import com.innowise.userService.model.entity.PaymentCard;
import com.innowise.userService.repository.specification.PaymentCardSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class PaymentCardSpecificationImpl implements PaymentCardSpecification {

    @Override
    public Specification<PaymentCard> hasHolder(String holder) {
        return (root, query, criteriaBuilder) ->
                holder == null ? null : criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("holder")),
                        "%" + holder.toLowerCase() + "%"
                );
    }
}
