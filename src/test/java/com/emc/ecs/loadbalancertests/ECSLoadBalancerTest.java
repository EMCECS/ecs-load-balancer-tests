package com.emc.ecs.loadbalancertests;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.emc.ecs.support.CheckAvailabilityChecker;
import com.emc.ecs.support.URIResourceStore;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.test.context.ContextConfiguration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jSpringRunner.class)
@ContextConfiguration(classes=ECSConfig.class)
public class ECSLoadBalancerTest {

    private static final Logger logger =LoggerFactory.getLogger(ECSLoadBalancerTest.class);

    @Autowired private AmazonS3Client s3;
    @Autowired private URIResourceStore store;
    @Autowired private String deployment;
    @Autowired private String instanceId;

    private CheckAvailabilityChecker checker;

    private String bucketName = "lbats-bucket";

    {
        Describe("ECSLoadBalancer", () -> {
            Context("given a bucket", () -> {
                BeforeEach(() -> {
                    logger.info("Creating bucket " + bucketName);
                    try {
                        Bucket bucket = s3.createBucket(new CreateBucketRequest(bucketName));
                    } catch (AmazonServiceException e) {
                        logger.error("Error creating bucket " + bucketName, e.getErrorMessage(), e);
                    }
                });
                AfterEach(() -> {
                    try {
                        logger.info("Deleting bucket " + bucketName);
                        s3.deleteBucket(bucketName);
                    } catch (AmazonServiceException e) {
                        logger.error("Error deleting bucket " + bucketName, e.getErrorMessage(), e);
                    }
                });
                Context("given an uploaded object", () -> {
                    BeforeEach(() -> {
                        logger.info("Uploading test object to bucket " + bucketName);

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
                            s3.deleteObject(bucketName, "test-object");
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
                                logger.info("Restarting ecs nodes");
                                // perform a BOSH restart on one of the nodes

//                            Process process = new ProcessBuilder("bosh","-d", deployment, "restart", instanceId).start();
//                            InputStream is = process.getInputStream();
//                            InputStreamReader isr = new InputStreamReader(is);
//                            BufferedReader br = new BufferedReader(isr);
//                            String line;
//
//                            while ((line = br.readLine()) != null) {
//                                System.out.println(line);
//                            }

                                Thread.sleep(20000);

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
        });
    }

    @Test
    public void noop() {}
}
