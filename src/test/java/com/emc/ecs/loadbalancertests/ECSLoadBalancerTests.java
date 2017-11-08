package com.emc.ecs.loadbalancertests;

import com.emc.ecs.support.URIResourceStore;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.test.context.ContextConfiguration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Ginkgo4jSpringRunner.class)
@ContextConfiguration(classes=ECSConfig.class)
public class ECSLoadBalancerTests {

    @Autowired private URIResourceStore store;

    {
        Describe("ECSLoadBalancer", () -> {
            Context("given an uploaded object", () -> {
                BeforeEach(() -> {
                    // use store to upload an object
                    WritableResource r = (WritableResource)store.getResource("test-object");
                    IOUtils.copy(this.getClass().getResourceAsStream("/test-object"), r.getOutputStream());
                });
//                AfterEach(() -> {
//                    DeletableResource r = (DeletableResource)store.getResource("test-object");
//                    r.delete();
//                });
                Context("when we watch that object for availability", () -> {
                    BeforeEach(() -> {
                        // kick off a thread that gets the uploaded object over and over
                    });
                    Context("when we bosh restart on of the ECS node VMs", () -> {
                        BeforeEach(() -> {
                            // perform a BOSH restart on one of the nodes
                        });
                        It("should be remain available throughout", () -> {
                            Resource r = store.getResource("test-object");
                            assertThat(IOUtils.contentEquals(r.getInputStream(), this.getClass().getResourceAsStream("/test-object")), is(true));
                        });
                    });
                });
            });
        });
    }

    @Test
    public void noop() {}
}
