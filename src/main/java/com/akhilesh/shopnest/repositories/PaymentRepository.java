package com.akhilesh.shopnest.repositories;

import com.akhilesh.shopnest.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReferenceId(String referenceId);
}
