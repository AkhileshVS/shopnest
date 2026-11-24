package com.akhilesh.shopnest.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(unique = true, nullable = false)
    private String referenceId;

    private String gatewayPaymentId;

    @Column(length = 1000)
    private String paymentLink;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentGatewayType gateway;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
}
