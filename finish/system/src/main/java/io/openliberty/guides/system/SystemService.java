// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.system;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import io.openliberty.guides.models.PropertyMessage;
import io.openliberty.guides.models.SystemLoad;
import io.reactivex.rxjava3.core.Flowable;

@ApplicationScoped
public class SystemService {
    
    private static Logger logger = Logger.getLogger(SystemService.class.getName());

    private static final OperatingSystemMXBean osMean = 
            ManagementFactory.getOperatingSystemMXBean();
    private static String hostname = null;

    private static String getHostname() {
        if (hostname == null) {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                return System.getenv("HOSTNAME");
            }
        }
        return hostname;
    }

    @Outgoing("systemLoad")
    public Publisher<SystemLoad> sendSystemLoad() {
        return Flowable.interval(15, TimeUnit.SECONDS)
                .map((interval -> new SystemLoad(getHostname(),
                        osMean.getSystemLoadAverage())));
    }

    @Incoming("propertyRequest")
    @Outgoing("propertyResponse")
    // tag::ackAnnotation[]
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    // end::ackAnnotation[]
    // tag::methodSignature[]
    public PublisherBuilder<Message<PropertyMessage>> sendProperty(Message<String> propertyName) {
    // end::methodSignature[]
        logger.info("sendProperty: " + propertyName);
        // tag::propertyValue[]
        String propertyValue = System.getProperty(propertyName.getPayload());
        // end::propertyValue[]
        // tag::invalid[]
        if (propertyValue == null) {
            logger.warning(propertyName + " is not System property.");
            // tag::propertyNameAck[]
            propertyName.ack();
            // end::propertyNameAck[]
            // tag::emptyReactiveStream[]
            return ReactiveStreams.empty();
            // end::emptyReactiveStream[]
        }
        // end::invalid[]
        // tag::returnMessage[]
        return ReactiveStreams.of(Message.of(
                new PropertyMessage(getHostname(),
                    propertyName.getPayload(),
                    System.getProperty(propertyName.getPayload(), "unknown")),
                    propertyName::ack
                ));
        // end::returnMessage[]
    }
}