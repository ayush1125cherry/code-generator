package com.ayushrawat.projects.lovable_clone.repository;

import com.ayushrawat.projects.lovable_clone.entity.Subscription;
import com.ayushrawat.projects.lovable_clone.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;
import java.util.Set;

public interface SubscriptionRepository extends JpaRepository<Subscription , Long> {
    Optional<Subscription> findByUserIdAndStatusIn(Long userId, Set<SubscriptionStatus> statusSet);

    boolean existsByStripeSubscriptionId(String subscriptionId);

    Optional<Subscription> findByStripeSubscriptionId(String gatewaySubscriptionId);
}
