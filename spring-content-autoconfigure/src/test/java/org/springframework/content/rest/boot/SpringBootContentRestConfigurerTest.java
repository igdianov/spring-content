package org.springframework.content.rest.boot;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.runner.RunWith;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.http.MediaType;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration.ContentRestProperties;
import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration.ContentRestProperties.RequestMappings;
import internal.org.springframework.content.rest.boot.autoconfigure.SpringBootContentRestConfigurer;

@RunWith(Ginkgo4jRunner.class)
public class SpringBootContentRestConfigurerTest {

    private SpringBootContentRestConfigurer configurer;

    private ContentRestProperties properties;

    // mocks
    private RestConfiguration restConfig;
    private RestConfiguration.Exclusions exclusions;

    {
        Describe("SpringBootContentRestConfigurer", () -> {

            Context("#configure", () -> {

                BeforeEach(() -> {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.exclusions()).thenReturn(exclusions);
                });

                JustBeforeEach(() -> {
                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);
                });

                Context("given a base uri property", () -> {

                    BeforeEach(() -> {
                        properties.setBaseUri(URI.create("/test"));
                    });

                    It("should set the property on the RestConfiguration", () -> {
                        verify(restConfig).setBaseUri(eq(properties.getBaseUri()));
                    });
                });

                Context("given a fullyQualifiedLinks property setting", () -> {

                    BeforeEach(() -> {
                        properties.setFullyQualifiedLinks(true);
                    });

                    It("should set the property on the RestConfiguration", () -> {
                        verify(restConfig).setFullyQualifiedLinks(eq(true));
                    });
                });

                Context("given excluded request mappings", () -> {

                    BeforeEach(() -> {
                        RequestMappings mappings = new RequestMappings();
                        mappings.setExcludes("GET=a/b,c/d:PUT=*/*");
                        properties.setRequestMappings(mappings);
                    });

                    It("should set the exclusions property on the RestConfiguration", () -> {
                        verify(exclusions).exclude("GET", MediaType.parseMediaType("a/b"));
                        verify(exclusions).exclude("GET", MediaType.parseMediaType("c/d"));
                        verify(exclusions).exclude("PUT", MediaType.parseMediaType("*/*"));
                    });
                });

                Context("given empty excluded request mapping", () -> {

                    BeforeEach(() -> {
                        RequestMappings mappings = new RequestMappings();
                        mappings.setExcludes("");
                        properties.setRequestMappings(mappings);
                    });

                    It("should not set the exclusions property on the RestConfiguration", () -> {
                        verify(exclusions, never()).exclude(any(), any());
                    });
                });

                Context("given empty excluded GET request mapping", () -> {

                    BeforeEach(() -> {
                        RequestMappings mappings = new RequestMappings();
                        mappings.setExcludes("GET=");
                        properties.setRequestMappings(mappings);
                    });

                    It("should not set the exclusions property on the RestConfiguration", () -> {
                        verify(exclusions, never()).exclude(any(), any());
                    });
                });

                Context("given invalid excluded request mapping", () -> {

                    BeforeEach(() -> {
                        RequestMappings mappings = new RequestMappings();
                        mappings.setExcludes("GET=/");
                        properties.setRequestMappings(mappings);
                    });

                    It("should not set the exclusions property on the RestConfiguration", () -> {
                        verify(exclusions, never()).exclude(any(), any());
                    });
                });

                Context("given a null base uri property", () -> {

                    It("should not set the property on the RestConfiguration", () -> {
                        verify(restConfig, never()).setBaseUri(any());
                    });
                });

                Context("given a null properties", () -> {

                    BeforeEach(() -> {
                        properties = null;
                    });

                    It("should not set the property on the RestConfiguration", () -> {
                        verify(restConfig, never()).setBaseUri(any());
                        verify(restConfig, never()).setFullyQualifiedLinks(anyBoolean());
                    });
                });
            });
        });
    }
}
