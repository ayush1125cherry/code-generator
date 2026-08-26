package com.ayushrawat.projects.lovable_clone.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
public class PaymentConfig {
    @Value("${stripe.api.secret}")
    private String stripeSecretKey;

        @PostConstruct
        public void init(){
            Stripe.apiKey=stripeSecretKey;
        }

}
