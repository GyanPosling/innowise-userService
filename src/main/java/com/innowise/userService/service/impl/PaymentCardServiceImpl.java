package com.innowise.userservice.service.impl;

import com.innowise.userservice.exception.LimitExceededException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.exception.ValidationException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.model.dto.PaymentCardDto;
import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.specification.PaymentCardSpecification;
import com.innowise.userservice.service.PaymentCardService;
import com.innowise.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class PaymentCardServiceImpl implements PaymentCardService {

    private static final String CARD_NOT_FOUND_MESSAGE = "Card not found with id: ";

    private final PaymentCardRepository paymentCardRepository;
    private final UserService userService;
    private final PaymentCardSpecification paymentCardSpecification;
    private final PaymentCardMapper paymentCardMapper;
    private final UserMapper userMapper;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "userCards", key = "#userId"),
                    @CacheEvict(value = "cardsPage", allEntries = true)
            }
    )
    public PaymentCardDto createCard(Integer userId, PaymentCardDto paymentCardDTO) {
        if (userService.getActiveCardCount(userId) >= 5) {
            throw new LimitExceededException("User cannot have more than 5 active cards");
        }

        if (paymentCardRepository.existsByUserIdAndCardNumber(userId, paymentCardDTO.getNumber())) {
            throw new ValidationException("Card with this number already exists for this user");
        }

        User user = userMapper.toEntity(userService.getUserById(userId));
        PaymentCard card = paymentCardMapper.toEntity(paymentCardDTO);
        card.setUser(user);
        card.setActive(true);
        PaymentCard savedCard = paymentCardRepository.save(card);

        return paymentCardMapper.toDTO(savedCard);
    }

    @Override
    @Cacheable(value = "cards", key = "#id", unless = "#result == null")
    public PaymentCardDto getCardById(Integer id) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND_MESSAGE + id));
        return paymentCardMapper.toDTO(card);
    }

    @Override
    @Cacheable(value = "cardsPage", key = "{#holder, #pageable.pageNumber, #pageable.pageSize, #pageable.sort}")
    public Page<PaymentCardDto> getAllCards(String holder, Pageable pageable) {
        Specification<PaymentCard> spec = Specification.where((Specification<PaymentCard>) null);

        if (holder != null && !holder.isBlank()) {
            spec = spec.and(paymentCardSpecification.hasHolder(holder));
        }

        Page<PaymentCard> cards = paymentCardRepository.findAll(spec, pageable);
        return cards.map(paymentCardMapper::toDTO);
    }

    @Override
    @Cacheable(value = "userCardsList", key = "#userId")
    public List<PaymentCardDto> getCardsByUserId(Integer userId) {
        List<PaymentCard> cards = paymentCardRepository.findByUserId(userId);
        return cards.stream()
                .map(paymentCardMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "cards", key = "#id"),
                    @CacheEvict(value = "cardsPage", allEntries = true),
                    @CacheEvict(value = "userCards", key = "#result?.userId"),
                    @CacheEvict(value = "userCardsList", key = "#result?.userId")
            }
    )
    public PaymentCardDto updateCard(Integer id, PaymentCardDto paymentCardDTO) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND_MESSAGE + id));

        card.setNumber(paymentCardDTO.getNumber());
        card.setHolder(paymentCardDTO.getHolder());
        card.setExpirationDate(paymentCardDTO.getExpirationDate());

        PaymentCard updatedCard = paymentCardRepository.save(card);
        return paymentCardMapper.toDTO(updatedCard);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "cards", key = "#id"),
                    @CacheEvict(value = "cardsPage", allEntries = true),
                    @CacheEvict(value = "userCards", key = "#result?.userId"),
                    @CacheEvict(value = "userCardsList", key = "#result?.userId")
            }
    )
    public PaymentCardDto toggleCardStatus(Integer id) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND_MESSAGE + id));
        boolean newStatus = !card.isActive();
        card.setActive(newStatus);
        PaymentCard updatedCard = paymentCardRepository.save(card);
        return paymentCardMapper.toDTO(updatedCard);
    }

    @Override
    @Transactional
    public void deleteCard(Integer id) {
        Integer userId = paymentCardRepository.findUserIdByCardId(id)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND_MESSAGE + id));
        paymentCardRepository.deleteById(id);
        evictCardCaches(id, userId);
    }

    private void evictCardCaches(Integer cardId, Integer userId) {
        Cache cardsCache = cacheManager.getCache("cards");
        if (cardsCache != null) {
            cardsCache.evict(cardId);
        }
        Cache cardsPageCache = cacheManager.getCache("cardsPage");
        if (cardsPageCache != null) {
            cardsPageCache.clear();
        }
        Cache userCardsCache = cacheManager.getCache("userCards");
        if (userCardsCache != null) {
            userCardsCache.evict(userId);
        }
        Cache userCardsListCache = cacheManager.getCache("userCardsList");
        if (userCardsListCache != null) {
            userCardsListCache.evict(userId);
        }
    }
}
