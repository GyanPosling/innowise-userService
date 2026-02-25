package com.innowise.userservice.repository.specification.impl;



import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.repository.specification.PaymentCardSpecification;
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
