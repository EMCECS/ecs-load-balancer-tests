package com.emc.ecs.loadbalancertests;

import com.emc.ecs.support.URIResourceStore;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.junit.Assert.fail;

@RunWith(Ginkgo4jSpringRunner.class)
@ContextConfiguration(classes=ECSConfig.class)
public class ECSLoadBalancerTests {

    @Autowired private URIResourceStore store;

    {
        Describe("ECSLoadBalancerTests", () -> {
            Context("given an uploaded object", () -> {
                BeforeEach(() -> {
                    // use store to upload an object
                });
                Context("when we watch that object for availability", () -> {
                    BeforeEach(() -> {
                        // kick off a thread that gets the uploaded object over and over
                    });
                    Context("when we bosh restart on of the ECS node VMs", () -> {
                        BeforeEach(() -> {
                            // perform a BOSH restart on one of the nodes
                        });
                        It("should be remain available throughout", () -> {
                            fail("badness");
                        });
                    });
                });
            });
        });
    }

    @Test
    public void noop() {}
}
