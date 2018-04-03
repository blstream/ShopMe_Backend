package com.intive.shopme.service;

import com.intive.shopme.model.Offer;
import com.intive.shopme.repository.OfferRepository;
import com.intive.shopme.util.validation.error.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class OfferService {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    private final OfferRepository repository;

    @Autowired
    public OfferService(OfferRepository repository) {
        this.repository = repository;
    }

    public Page<Offer> getAll(Pageable pageable, Specification<Offer> filter) {
        return repository.findAll(filter, pageable);
    }

    public Offer get(UUID id) {
        return repository.findById(id).orElseThrow(()->new NotFoundException("Offer not found"));
    }

    public void add(Offer offer) {
        validate(offer);
        repository.save(offer);
    }

    public void update(Offer offer) {
        if (!repository.existsById(offer.getId())) {
            throw new NotFoundException("Offer not found");
        }
        repository.save(offer);
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Offer not found");
        }
        repository.deleteById(id);
    }

    private void validate(Offer offer) {
        final Validator validator = VALIDATOR_FACTORY.getValidator();
        Set<ConstraintViolation<Offer>> violations = validator.validate(offer);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

}
