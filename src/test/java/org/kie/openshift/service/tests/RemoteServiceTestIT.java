
package org.kie.openshift.service.tests;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.kie.endpoint.service.api.ServiceEndpoint;

public class RemoteServiceTestIT {

    private Client client;
    private ResteasyWebTarget target;
    private static final String APP_URL = "http://localhost:8080/api";
    private static final String APP_WS_URL = "ws://localhost:8080/txs";

    @Before
    public void setup() throws Exception {
        client = ClientBuilder.newBuilder().build();
        final WebTarget webtarget = client.target( APP_URL );
        target = ( ResteasyWebTarget ) webtarget;

    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void deployImageTest() {

        ServiceEndpoint serviceProxy = target.proxy( ServiceEndpoint.class );

        serviceProxy.deployImage( "hello/world" );

        List<String> deployedImages = serviceProxy.getDeployedImages();

        assertEquals( 1, deployedImages.size() );

    }

}
