package com.emc.ecs.loadbalancertests;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableS3Stores(basePackages="com.emc.ecs.support")
public class ECSConfig {

    static {
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");
    }

    @Value("${ecs.url:#{environment.ECS_S3_URL}}")
    private String url;

    @Value("${ecs.accessKey:#{environment.ECS_ACCESS_KEY_ID}}")
    private String accessKey;

    @Value("${ecs.secretKey:#{environment.ECS_SECRET_KEY}}")
    private String secretKey;

    @Value("${ecs.bucket:#{environment.ECS_BUCKET}}")
    private String bucket;

    @Value("${deployment:#{environment.DEPLOYMENT}}")
    private String deployment;

    @Value("${ecs.server.instanceId:#{environment.ECS_SERVER_INSTANCE_ID}}")
    private String instanceId;

    @Bean
    public String bucket() {
        return bucket;
    }

    @Bean
    public String deployment() {
        return deployment;
    }

    @Bean
    public String instanceId() {
        return instanceId;
    }

    @Bean
    public SimpleStorageResourceLoader simpleStorageResourceLoader() {
        return new SimpleStorageResourceLoader(amazonS3Client(basicAWSCredentials()));
    }

    @Bean
    public BasicAWSCredentials basicAWSCredentials() {
        return new BasicAWSCredentials(accessKey, secretKey);
    }

    @Bean
    S3ClientOptions s3ClientOptions() {
        S3ClientOptions opts = new S3ClientOptions();
        opts.setPathStyleAccess(true);
        return opts;
    }

    @Bean
    public AmazonS3Client amazonS3Client(AWSCredentials awsCredentials) {
        AmazonS3Client amazonS3Client = new AmazonS3Client(awsCredentials);
        amazonS3Client.setEndpoint(url);
        amazonS3Client.setS3ClientOptions(s3ClientOptions());
        return amazonS3Client;
    }
}
