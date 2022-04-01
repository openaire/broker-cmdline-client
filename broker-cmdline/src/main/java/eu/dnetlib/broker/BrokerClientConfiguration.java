/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package eu.dnetlib.broker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrokerClientConfiguration {

    @Bean
    public BrokerClient brokerClient() {
        return new BrokerClient();
    }

}
