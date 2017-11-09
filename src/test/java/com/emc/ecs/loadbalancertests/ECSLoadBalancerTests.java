package com.emc.ecs.loadbalancertests;

import com.emc.ecs.support.CheckAvailabiltiyThread;
import com.emc.ecs.support.URIResourceStore;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.test.context.ContextConfiguration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jSpringRunner.class)
@ContextConfiguration(classes=ECSConfig.class)
public class ECSLoadBalancerTests {

    @Autowired private URIResourceStore store;
    @Autowired private String cfDeployment;
    @Autowired private String instanceId;

    private CheckAvailabiltiyThread checker;

    {
        Describe("ECSLoadBalancer", () -> {
//            BeforeEach(() -> {
//                javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
//                    new javax.net.ssl.HostnameVerifier(){
//                        public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
//                            return true;
//                        }
//                    });
//
//                CertificateFactory certFactory;
//                InputStream certInputStream = this.getClass().getResourceAsStream("/ecs_cert.pem");
//
//                certFactory = CertificateFactory.getInstance("X.509");
//                Certificate caCert = certFactory.generateCertificate(certInputStream);
//                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//                keyStore.load(null);
//                keyStore.setCertificateEntry("caCert", caCert);
//
//                TrustManagerFactory trustMgrFactory = TrustManagerFactory
//                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
//                trustMgrFactory.init(keyStore);
//
//                SSLContext sslContext = SSLContext.getInstance("TLS");
//                sslContext.init(null, trustMgrFactory.getTrustManagers(), null);
//            });
            Context("given an uploaded object", () -> {
                BeforeEach(() -> {
                    // use store to upload an object
                    WritableResource r = (WritableResource)store.getResource("test-object");
                    InputStream is = this.getClass().getResourceAsStream("/test-object");
                    OutputStream os = null;
                    try {
                        os = r.getOutputStream();
                        IOUtils.copy(is, os);
                    } finally {
                        IOUtils.closeQuietly(os);
                        IOUtils.closeQuietly(is);
                    }
                });
//                AfterEach(() -> {
//                    DeletableResource r = (DeletableResource)store.getResource("test-object");
//                    r.delete();
//                });
                Context("when we watch that object for availability", () -> {
                    BeforeEach(() -> {
                        // kick off a thread that gets the uploaded object over and over
                        checker = new CheckAvailabiltiyThread(store, "test-object");
                        checker.start();
                    });
                    AfterEach(()-> {
                        checker.stopSignal();
                    });
                    Context("when we bosh restart on of the ECS node VMs", () -> {
                        BeforeEach(() -> {
                            // perform a BOSH restart on one of the nodes

                            Process process = new ProcessBuilder("bosh","-d", cfDeployment, "restart", instanceId).start();
                            InputStream is = process.getInputStream();
                            InputStreamReader isr = new InputStreamReader(is);
                            BufferedReader br = new BufferedReader(isr);
                            String line;

                            while ((line = br.readLine()) != null) {
                                System.out.println(line);
                            }

                        });
                        It("should be remain available throughout", () -> {
                            Resource r = store.getResource("test-object");
                            assertThat(r.exists(), is(true));
                            assertThat(IOUtils.contentEquals(r.getInputStream(), this.getClass().getResourceAsStream("/test-object")), is(true));
                            assertThat(checker.isAvailable(), is(true));
                        });
                    });
                });
            });
        });
    }

    @Test
    public void noop() {}
}
