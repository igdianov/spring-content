package internal.org.springframework.content.rest.links;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;
import javax.sql.DataSource;
import javax.transaction.Transactional;

import org.apache.commons.io.IOUtils;
import org.hibernate.annotations.Formula;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.rest.RestResource;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

import internal.org.springframework.content.rest.support.TestEntityChild;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads = 1)
@WebAppConfiguration
@ContextConfiguration(classes = {
        ContentLinkRelIT.BaseUriConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class,
		HypermediaConfiguration.class })
@Transactional
//@ActiveProfiles("store")
public class ContentLinkRelIT {

    @Autowired
    TestEntityRepository repository;
    @Autowired
    TestEntityContentRepository contentRepository;

    @Autowired
    TestEntity5Repository repository5;
    @Autowired
    TestEntity5LinkrelStore store5;

    @Autowired
    TestEntity2Repository repository2;
    @Autowired
    TestEntity2Store store2;

    @Autowired
    TestEntity10Repository repository10;
    @Autowired
    TestEntity10Store store10;

    @Autowired
    TestEntity3Repository repository3;
    @Autowired
    TestEntity3ContentRepository contentRepository3;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    private TestEntity testEntity;
    private TestEntity3 testEntity3;
    private TestEntity5 testEntity5;
    private TestEntity2 testEntity2;
    private TestEntity10 testEntity10;

	private ContentLinkTests contentLinkTests;

	{
		Describe("given an exporting store specifying a linkRel of foo", () -> {
	        Describe("linkrel", () -> {
	            BeforeEach(() -> {
	                mvc = MockMvcBuilders.webAppContextSetup(context).build();
	            });

	            Context("given a store specifying a linkRel and an entity with a top-level uncorrelated content property", () -> {
	                BeforeEach(() -> {
	                    testEntity = repository.save(new TestEntity());

	                    contentLinkTests.setMvc(mvc);
	                    contentLinkTests.setRepository(repository);
	                    contentLinkTests.setStore(contentRepository);
	                    contentLinkTests.setTestEntity(testEntity);
	                    contentLinkTests.setUrl("/api/testEntities/" + testEntity.getId());
	                    contentLinkTests.setLinkRel("foo/content");
	                    contentLinkTests.setExpectedLinkRegex(format("http://localhost/contentApi/testEntitiesContent/%s/content", testEntity.getId()));
	                });
	                contentLinkTests = new ContentLinkTests();
	            });

	            Context("given a store specifying a linkRel and an entity with top-level correlated content properties", () -> {
	                BeforeEach(() -> {

	                    testEntity5 = repository5.save(new TestEntity5());

	                    contentLinkTests.setMvc(mvc);
	                    contentLinkTests.setRepository(repository5);
	                    contentLinkTests.setStore(store5);
	                    contentLinkTests.setTestEntity(testEntity5);
	                    contentLinkTests.setUrl("/api/testEntity5s/" + testEntity5.getId());
	                    contentLinkTests.setLinkRel("foo/contentProperty");
	                    contentLinkTests.setExpectedLinkRegex(format("http://localhost/contentApi/testEntity5s/%s/contentProperty", testEntity5.getId()));
	                });
	                contentLinkTests = new ContentLinkTests();
	            });

	            Context("given a store specifying a linkrel and an entity a nested content property", () -> {
	              BeforeEach(() -> {

	                  StoreRestResource srr = new DynamicStoreRestResource("foo");
	                  alterAnnotationValueJDK8(TestEntity2Store.class, StoreRestResource.class, srr);

	                  testEntity2 = new TestEntity2();
	                  testEntity2.getChild().setContentId(UUID.randomUUID());
	                  testEntity2.getChild().setContentLen(1L);
	                  testEntity2.getChild().setMimeType("text/plain");
	                  testEntity2 = repository2.save(testEntity2);

	                  contentLinkTests.setMvc(mvc);
	                  contentLinkTests.setRepository(repository2);
	                  contentLinkTests.setStore(store2);
	                  contentLinkTests.setTestEntity(testEntity2);
	                  contentLinkTests.setUrl("/api/files/" + testEntity2.getId());
	                  contentLinkTests.setLinkRel("foo/child");
	                  contentLinkTests.setExpectedLinkRegex(format("http://localhost/contentApi/files/%s/child", testEntity2.getId()));
	              });
	              contentLinkTests = new ContentLinkTests();
	            });

	            Context("given a store specifying a linkrel and an entity with nested content properties", () -> {
	              BeforeEach(() -> {

	                  StoreRestResource srr = new DynamicStoreRestResource("foo");
	                  alterAnnotationValueJDK8(TestEntity10Store.class, StoreRestResource.class, srr);

	                  testEntity10 = new TestEntity10();
	                  testEntity10.getChild().setContentId(UUID.randomUUID());
	                  testEntity10.getChild().setContentLen(1L);
	                  testEntity10.getChild().setContentMimeType("text/plain");
	                  testEntity10.getChild().setContentFileName("test");
	                  testEntity10.getChild().setPreviewId(UUID.randomUUID());
	                  testEntity10.getChild().setPreviewLen(1L);
	                  testEntity10.getChild().setPreviewMimeType("text/plain");
	                  testEntity10 = repository10.save(testEntity10);

	                  contentLinkTests.setMvc(mvc);
	                  contentLinkTests.setRepository(repository10);
	                  contentLinkTests.setStore(store10);
	                  contentLinkTests.setTestEntity(testEntity10);
	                  contentLinkTests.setUrl("/api/testEntity10s/" + testEntity10.getId());
	                  contentLinkTests.setLinkRel("foo/child/content");
	                  contentLinkTests.setExpectedLinkRegex(format("http://localhost/contentApi/testEntity10s/%s/child/content", testEntity10.getId()));
	              });
	              contentLinkTests = new ContentLinkTests();
	            });
	        });
		});
	}

	@Test
	public void noop() {
	}

    private static final String ANNOTATION_METHOD = "annotationData";
    private static final String ANNOTATION_FIELDS = "declaredAnnotations";
    private static final String ANNOTATIONS = "annotations";

    public static void alterAnnotationValueJDK8(Class<?> targetClass, Class<? extends Annotation> targetAnnotation, Annotation targetValue) {
      try {
          Method method = Class.class.getDeclaredMethod(ANNOTATION_METHOD, null);
          method.setAccessible(true);

          Object annotationData = method.invoke(targetClass);

          Field annotations = annotationData.getClass().getDeclaredField(ANNOTATIONS);
          annotations.setAccessible(true);

          Map<Class<? extends Annotation>, Annotation> map = (Map<Class<? extends Annotation>, Annotation>) annotations.get(annotationData);
          map.put(targetAnnotation, targetValue);
      } catch (Exception e) {
          e.printStackTrace();
      }
    }

    public static class DynamicStoreRestResource implements StoreRestResource {

        private String linkRel;

        public DynamicStoreRestResource(String linkRel) {
            this.linkRel = linkRel;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return DynamicStoreRestResource.class;
        }

        @Override
        public String path() {
            return "";
        }

        @Override
        public String linkRel() {
            return linkRel;
        }
    }

    @Entity
    @EntityListeners(AuditingEntityListener.class)
    @Getter
    @Setter
    public static class TestEntity5 {
        public @Id @GeneratedValue Long id;

        public String name;

        private @ContentId UUID contentPropertyId;
        private @ContentLength Long contentPropertyLen;
        private @MimeType String contentPropertyMimeType;

        private @ContentId UUID renditionPropertyId;
        private @ContentLength Long renditionPropertyLen;
        private @MimeType String renditionPropertyMimeType;

        private @Version Long version;
        private @CreatedDate Date createdDate;
        private @LastModifiedDate Date modifiedDate;
    }

    @StoreRestResource(linkRel="foo")
    public interface TestEntity5LinkrelStore extends FilesystemContentStore<TestEntity5, UUID> {

        @RestResource(paths={"rendition"}, exported=false)
        @Override
        InputStream getContent(TestEntity5 entity, PropertyPath propertyPath);
    }

    public interface TestEntity5Repository extends JpaRepository<TestEntity5, Long> {}

    @Entity
    @Getter
    @Setter
    public static class TestEntity {
        private @Id @GeneratedValue Long id;
        private String name;
        private @ContentId UUID contentId;
        private @ContentLength Long len;
        private @MimeType String mimeType;
        private @OriginalFileName String originalFileName;
        private String title;
    }

    @CrossOrigin(origins = "http://www.someurl.com")
    @RepositoryRestResource(path = "testEntities")
    public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
    }

    @CrossOrigin(origins = "http://www.someurl.com")
    @StoreRestResource(path = "testEntitiesContent", linkRel ="foo")
    public interface TestEntityContentRepository extends FilesystemContentStore<TestEntity, Long>, Renderable<TestEntity> {
    }

    @Entity
    @EntityListeners(AuditingEntityListener.class)
    @Getter
    @Setter
    public static class TestEntity2 {
        private @Id @GeneratedValue Long id;

        private @Version Long version;
        private @CreatedDate Date createdDate;
        private @LastModifiedDate Date modifiedDate;

        private @Embedded TestEntityChild child = new TestEntityChild();
        private @RestResource @ElementCollection(fetch = FetchType.EAGER) List<TestEntityChild> children = new ArrayList<>();
    }

    @StoreRestResource(path = "files")
    public interface TestEntity2Store extends FilesystemContentStore<TestEntity2, UUID> {
    }

    @RepositoryRestResource(path = "files")
    public interface TestEntity2Repository extends JpaRepository<TestEntity2, Long> {
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    @EntityListeners(AuditingEntityListener.class)
    public static class TestEntity10 {

        @Id
        @GeneratedValue
        private Long id;

        private @Version Long version;
        private @CreatedDate Date createdDate;
        private @LastModifiedDate Date modifiedDate;

        private @Embedded TestEntity10Child child = new TestEntity10Child();
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class TestEntity10Child {

        @ContentId public UUID contentId;
        @ContentLength public Long contentLen;
        @MimeType public String contentMimeType;
        @OriginalFileName public String contentFileName = "";

        @ContentId public UUID previewId;
        @ContentLength public Long previewLen;
        @MimeType public String previewMimeType;

        // prevent TestEntity8Child from being return by hibernate as null
        @Formula("1")
        private int workaroundForBraindeadJpaImplementation;

    }

    public interface TestEntity10Repository extends CrudRepository<TestEntity10, Long> {
    }

    @StoreRestResource(/*linkRel = "foo"*/)
    public interface TestEntity10Store extends FilesystemContentStore<TestEntity10, UUID>, Renderable<TestEntity10> {
    }

    @Entity
    @Getter
    @Setter
    public class TestEntity3 {
        public @Id @GeneratedValue Long id;
        public String name;
        public @ContentId UUID contentId;
        public @ContentLength Long len;
        public @MimeType String mimeType;
        private @OriginalFileName String originalFileName;
        private String title;
    }


    public interface TestEntity3ContentRepository extends FilesystemContentStore<TestEntity3, Long>, Renderable<TestEntity3> {
        @RestResource(exported=false)
        @Override
        TestEntity3 setContent(TestEntity3 property, InputStream content);
    }

    public interface TestEntity3Repository extends JpaRepository<TestEntity3, Long> {
    }

    @Configuration
    @EnableJpaRepositories(considerNestedRepositories=true)
    @EnableTransactionManagement
    // @Import(RepositoryRestMvcConfiguration.class)
    @EnableFilesystemStores()
//    @Profile("store")
    public static class BaseUriConfig extends JpaInfrastructureConfig {

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
        }

        @Bean
        public File filesystemRoot() {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            File filesystemRoot = new File(baseDir, "content-links-linkrel-it");
            filesystemRoot.mkdirs();
            return filesystemRoot;
        }

        @Bean
        public RepositoryRestConfigurer repositoryRestConfigurer() {
            return new RepositoryRestConfigurer() {

                @Override
                public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
                    config.setBasePath("/api");
                }
            };
        }

        @Bean
        public ContentRestConfigurer configurer() {
            return new ContentRestConfigurer() {
                @Override
                public void configure(RestConfiguration config) {
                    config.setBaseUri(URI.create("/contentApi"));
                }
            };
        }

        @Bean
        public RenditionProvider textToHtml() {
            return new RenditionProvider() {

                @Override
                public String consumes() {
                    return "text/plain";
                }

                @Override
                public String[] produces() {
                    return new String[] { "text/html" };
                }

                @Override
                public InputStream convert(InputStream fromInputSource, String toMimeType) {
                    String input = null;
                    try {
                        input = IOUtils.toString(fromInputSource);
                    }
                    catch (IOException e) {
                    }
                    return new ByteArrayInputStream(
                            String.format("<html><body>%s</body></html>", input).getBytes());
                }
            };
        }
    }

    @Configuration
    @EnableJpaAuditing
    public static class JpaInfrastructureConfig {

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            return builder.setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.H2);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan(ContentLinkRelIT.class.getPackage().getName());
            factory.setPersistenceUnitName("content-links-linkrel");
            factory.setDataSource(dataSource());
            factory.afterPropertiesSet();

            return factory;
        }
//
//        protected String[] packagesToScan() {
//            return new String[] {
//                "internal.org.springframework.content.rest.support"
//            };
//        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return new JpaTransactionManager();
        }
    }
}
