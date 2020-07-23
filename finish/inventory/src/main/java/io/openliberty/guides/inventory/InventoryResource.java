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
package io.openliberty.guides.inventory;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;

import io.openliberty.guides.models.PropertyMessage;
import io.openliberty.guides.models.SystemLoad;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;


@ApplicationScoped
@Path("/inventory")
public class InventoryResource {

    private static Logger logger = Logger.getLogger(InventoryResource.class.getName());
    private FlowableEmitter<Message<String>> propertyNameEmitter;

    @Inject
    private InventoryManager manager;
    
    @GET
    @Path("/systems")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystems() {
        List<Properties> systems = manager.getSystems()
                .values()
                .stream()
                .collect(Collectors.toList());
        return Response
                .status(Response.Status.OK)
                .entity(systems)
                .build();
    }

    @GET
    @Path("/systems/{hostname}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystem(@PathParam("hostname") String hostname) {
        Optional<Properties> system = manager.getSystem(hostname);
        if (system.isPresent()) {
            return Response
                    .status(Response.Status.OK)
                    .entity(system)
                    .build();
        }
        return Response
                .status(Response.Status.NOT_FOUND)
                .entity("hostname does not exist.")
                .build();
    }

    // tag::updateSystemProperty[]
    @PUT
    @Path("/data")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    // tag::updateSystemPropertyHeader[]
    public CompletionStage<Response> updateSystemProperty(String propertyName) {
    // tag::updateSystemPropertyHeader[]
        logger.info("updateSystemProperty: " + propertyName);
        // tag::CompletableFuture[]
        CompletableFuture<Void> result = new CompletableFuture<>();
        // tag::CompletableFuture[]

        Message<String> message = Message.of(propertyName,
            // tag::acknowledgeAction[]
            () -> {
                result.complete(null);
                return CompletableFuture.completedFuture(null);
            }
            // end::acknowledgeAction[]
        );

        propertyNameEmitter.onNext(message);
        // tag::returnResult[]
        return result.thenApply(a -> Response
                .status(Response.Status.OK)
                .entity("Request successful for the " + propertyName + " property\n")
                .build());
        // end::returnResult[]
    }
    // end::updateSystemProperty[]

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetSystems() {
        manager.resetSystems();
        return Response
                .status(Response.Status.OK)
                .build();
    }

    // tag::updateStatus[]
    // tag::systemLoadIncoming[]
    @Incoming("systemLoad")
    // end::systemLoadIncoming[]
    public void updateStatus(SystemLoad sl)  {
        String hostname = sl.hostname;
        if (manager.getSystem(hostname).isPresent()) {
            manager.updateCpuStatus(hostname, sl.loadAverage);
            logger.info("Host " + hostname + " was updated: " + sl);
        } else {
            manager.addSystem(hostname, sl.loadAverage);
            logger.info("Host " + hostname + " was added: " + sl);
        }
    }
    // end::updateStatus[]

    // tag::getPropertyMessage[]
    // tag::addSystemPropertyIncoming[]
    @Incoming("addSystemProperty")
    // end::addSystemPropertyIncoming[]
    public void getPropertyMessage(PropertyMessage pm)  {
        logger.info("getPropertyMessage: " + pm);
        String hostId = pm.hostname;
        if (manager.getSystem(hostId).isPresent()) {
            manager.updatePropertyMessage(hostId, pm.key, pm.value);
            logger.info("Host " + hostId + " was updated: " + pm);
        } else {
            manager.addSystem(hostId, pm.key, pm.value);
            logger.info("Host " + hostId + " was added: " + pm);
        }
    }
    // end::getPropertyMessage[]

    // tag::sendPropertyName[]
    @Outgoing("requestSystemProperty")
    // tag::sendPropertyNameHeader[]
    public Publisher<Message<String>> sendPropertyName() {
    // end::sendPropertyNameHeader[]
        Flowable<Message<String>> flowable = Flowable.create(emitter ->
                this.propertyNameEmitter = emitter, BackpressureStrategy.BUFFER);
        return flowable;
    }
    // end::sendPropertyName[]
}