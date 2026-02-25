package com.innowise.userService.mapper;

import com.innowise.userService.model.dto.PaymentCardDto;
import com.innowise.userService.model.entity.PaymentCard;
import com.innowise.userService.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

    @Mapping(source = "user.id", target = "userId")
    PaymentCardDto toDTO(PaymentCard paymentCard);

    @Mapping(source = "userId", target = "user")
    PaymentCard toEntity(PaymentCardDto paymentCardDTO);

    default User mapUserIdToUser(Integer userId) {
        if (userId == null) return null;
        User user = new User();
        user.setId(userId);
        return user;
    }
}
