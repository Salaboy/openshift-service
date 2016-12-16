/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.endpoint.service.impl;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.NotNull;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.ConfigBuilder;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.dsl.ClientResource;
import io.fabric8.kubernetes.client.dsl.ClientRollableScallableResource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import org.kie.endpoint.service.api.ServiceEndpoint;

@ApplicationScoped
public class ServiceEndpointImpl implements ServiceEndpoint {

    private OpenShiftClient client;

    private String serviceName = "defaultService";
    private String label = "apps";
    private Integer internalPort = 8080;
    private String namespace = "default";

    public ServiceEndpointImpl() {
        io.fabric8.kubernetes.client.Config config = new io.fabric8.kubernetes.client.ConfigBuilder().build();
        KubernetesClient kubernetesClient = new DefaultKubernetesClient(config);
        client = kubernetesClient.adapt(OpenShiftClient.class);
    }

    @Override
    public void deployImage(@NotNull String imageName) {
        System.out.println("Deploying Image: " + imageName);
        doDeployment(serviceName, label, internalPort, namespace, imageName);
    }

    @Override
    public List<String> getDeployedImages() {
        List<String> result = new ArrayList<>();
        result.add("Hello there....");
        return result;
    }


    private void doDeployment(String serviceName, String label, Integer internalPort, String namespace, String image) {
        ClientRollableScallableResource<ReplicationController, DoneableReplicationController> resource = client
                .replicationControllers()
                .inNamespace(namespace)
                .withName(serviceName + "-rc");
        if (resource != null) {
            try {
                ReplicationController rc = resource.get();
                if (rc != null) {
                    ReplicationControllerStatus status = rc.getStatus();
                    Integer replicas = status.getReplicas();
                    System.out.println("Replicas at this point: " + replicas);
                    resource.scale(replicas + 1);
                } else {
                    client.replicationControllers().inNamespace(namespace).createNew()
                            .withNewMetadata().withName(serviceName + "-rc").addToLabels("app", label).endMetadata()
                            .withNewSpec().withReplicas(1)
                            .withNewTemplate()
                            .withNewMetadata().withName(serviceName + "-rc").addToLabels("app", label).endMetadata()
                            .withNewSpec()
                            .addNewContainer().withName(label).withImage(image)
                            // .addNewPort().withContainerPort(8080).withHostPort(8080).endPort()
                            .endContainer()
                            .endSpec()
                            .endTemplate()
                            .endSpec().done();
                }
            } catch (Exception ex) {
                ex.printStackTrace();

            }

        }
        ClientResource<Service, DoneableService> serviceResource = client.services().inNamespace(namespace).withName(serviceName);

        try {
            if (serviceResource != null) {
                Service service = serviceResource.get();
                if (service != null) {
                    ServiceStatus status = service.getStatus();
                    // The service already exist, so no need to create a new one
                } else {
                    service = client.services().inNamespace(namespace).createNew()
                            .withNewMetadata().withName(serviceName).endMetadata()
                            .withNewSpec()
                            .addToSelector("app", label)
                            .addNewPort().withPort(80).withNewTargetPort().withIntVal(internalPort).endTargetPort().endPort()
                            .endSpec()
                            .done();
                    OpenShiftClient osClient = client.adapt(OpenShiftClient.class);
                    Route route = osClient.routes().inNamespace(namespace)
                            .createNew()
                            .withNewSpec().withHost(serviceName + ".192.168.64.2.xip.io")
                            .withNewTo().withName(serviceName).withKind("Service").endTo()
                            .endSpec()
                            .withNewMetadata().addToLabels("name", serviceName).withGenerateName(serviceName).endMetadata()
                            .done();
                    String name = route.getMetadata().getName();
                    System.out.println(" Route generated Name: " + name);
                }

            }

        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }
}
