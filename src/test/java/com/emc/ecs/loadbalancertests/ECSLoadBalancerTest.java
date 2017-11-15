package com.emc.ecs.loadbalancertests;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.emc.ecs.support.CheckAvailabilityChecker;
import com.emc.ecs.support.URIResourceStore;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.test.context.ContextConfiguration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jSpringRunner.class)
@ContextConfiguration(classes=ECSConfig.class)
public class ECSLoadBalancerTest {

    private static final Logger logger =LoggerFactory.getLogger(ECSLoadBalancerTest.class);

    @Autowired private AmazonS3Client s3;
    @Autowired private String bucket = "lbats-bucket";
    @Autowired private URIResourceStore store;
    @Autowired private String deployment;
    @Autowired private String instanceId;

    private CheckAvailabilityChecker checker;

    {
        Describe("ECSLoadBalancer", () -> {
            Context("given a bucket", () -> {
                BeforeEach(() -> {
                    logger.info("Creating bucket " + bucket);
                    try {
                        s3.createBucket(new CreateBucketRequest(bucket));
                    } catch (AmazonServiceException e) {
                        logger.error("Error creating bucket " + bucket, e.getErrorMessage(), e);
                    }
                });
                AfterEach(() -> {
                    try {
                        logger.info("Deleting bucket " + bucket);
                        s3.deleteBucket(bucket);
                    } catch (AmazonServiceException e) {
                        logger.error("Error deleting bucket " + bucket, e.getErrorMessage(), e);
                    }
                });
                Context("given an uploaded object", () -> {
                    BeforeEach(() -> {
                        logger.info("Uploading test object to bucket " + bucket);

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
                    AfterEach(() -> {
                        try {
                            logger.info("Deleting test object");
                            s3.deleteObject(bucket, "test-object");
                        } catch (AmazonServiceException e) {
                            logger.error("Error deleting test object", e);
                        }
                    });
                    Context("when we watch that object for availability", () -> {
                        BeforeEach(() -> {
                            // kick off a thread that gets the uploaded object over and over
                            logger.info("Starting availability checker");

                            checker = new CheckAvailabilityChecker(store, "test-object");
                            checker.start();
                        });
                        AfterEach(()-> {
                            checker.stopSignal();
                        });
                        Context("when we bosh restart on of the ECS node VMs", () -> {
                            BeforeEach(() -> {
                                logger.info("Restarting ECS nodes");
                                // perform a BOSH restart on one of the nodes

                                ProcessBuilder pb = new ProcessBuilder("bosh", "-n", "-d", deployment, "restart", instanceId);
                                pb.redirectErrorStream(true);
                                logger.info("Executing command `bosh -n -d " + deployment+ " restart " + instanceId + "`");

                                Process process = pb.start();
                                InputStream is = process.getInputStream();
                                InputStreamReader isr = new InputStreamReader(is);
                                BufferedReader br = new BufferedReader(isr);
                                String line;

                                while ((line = br.readLine()) != null) {
                                    logger.info(line);
                                }

                                // Wait for a while to ensure that all the nodes came back up
                                Thread.sleep(5000);

                                logger.info("ECS nodes restarted");
                            });
                            It("should be remain available throughout", () -> {
                                Resource r = null;
                                try {
                                    r = store.getResource("test-object");
                                } catch (Exception e) {
                                    logger.error("Unable to fetch resource", e);
                                }
                                assertThat(r, is(not(nullValue())));
                                assertThat(r.exists(), is(true));
                                assertThat(IOUtils.contentEquals(r.getInputStream(), this.getClass().getResourceAsStream("/test-object")), is(true));
                                assertThat(checker.isAvailable(), is(true));
                                assertThat(checker.hasDowntime(), is(false));
                            });
                        });
                    });
                });
            });
        });
    }

    @Test
    public void noop() {}
}
