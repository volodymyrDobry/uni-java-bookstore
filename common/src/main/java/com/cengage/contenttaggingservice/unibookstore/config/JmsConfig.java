package com.cengage.contenttaggingservice.unibookstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * Shared JMS wiring for both services. Messages travel as JSON {@code TextMessage}s
 * carrying the payload's fully-qualified class name in a {@code _type} header, which
 * both services can resolve because the message contracts live in this common module.
 *
 * <p>Spring Boot's JMS auto-configuration applies the single {@link MessageConverter}
 * bean to both the {@code JmsTemplate} and the {@code @JmsListener} container factory,
 * so no further wiring is required.
 */
@Configuration
public class JmsConfig {

    static final String TYPE_HEADER = "_type";

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        // Jackson 3's JsonMapper auto-registers the java.time module, so Instant and
        // BigDecimal payload fields serialize correctly with no extra configuration.
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTargetType(MessageType.TEXT);
        // With a type-id property but no explicit mappings, the converter writes the
        // payload's fully-qualified class name and resolves it on the other side.
        converter.setTypeIdPropertyName(TYPE_HEADER);
        // Only deserialize our own message contracts named in the type header.
        converter.setTrustedPackages("com.cengage.contenttaggingservice.unibookstore.messaging");
        return converter;
    }
}
