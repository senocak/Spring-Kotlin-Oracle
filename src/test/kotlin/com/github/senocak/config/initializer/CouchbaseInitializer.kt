package com.github.senocak.config.initializer

import com.github.senocak.TestConstants.CONTAINER_WAIT_TIMEOUT
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.testcontainers.couchbase.BucketDefinition
import org.testcontainers.couchbase.CouchbaseContainer

@TestConfiguration
class CouchbaseInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        TestPropertyValues.of(
            "spring.datasource.url=" + couchbaseContainer.connectionString,
            "spring.datasource.username="+couchbaseContainer.username,
            "spring.datasource.password="+couchbaseContainer.password,
            "spring.datasource.bucketName="+bucketDefinition.name,
            "spring.datasource.scope=$SCOPE",
            "spring.datasource.ddl=create"
        ).applyTo(configurableApplicationContext.environment)
        createScope()
        createCollection(name = "user-collection")
        createCollection(name = "todo-collection")
    }

    companion object {
        private const val SCOPE = "integration-scope"
        private val bucketDefinition: BucketDefinition = BucketDefinition("mybucket")
            .withPrimaryIndex(true)

        private val couchbaseContainer: CouchbaseContainer = CouchbaseContainer("couchbase/server:7.6.1")
            .withExposedPorts(8091, 8092, 8093, 8094, 8095, 8096, 11210)
            .withCredentials("Administrator", "password")
            .withBucket(bucketDefinition)
            .withStartupTimeout(CONTAINER_WAIT_TIMEOUT)

        init {
            couchbaseContainer.start()
        }
    }
    private val headers = HttpHeaders().also {
        it.contentType = MediaType.APPLICATION_FORM_URLENCODED
        it.accept = listOf(MediaType.APPLICATION_JSON)
    }
    private fun createScope() {
        TestRestTemplate()
            .withBasicAuth(couchbaseContainer.username, couchbaseContainer.password)
            .postForEntity(
                "http://${couchbaseContainer.host}:${couchbaseContainer.bootstrapHttpDirectPort}/pools/default/buckets/${bucketDefinition.name}/scopes",
                HttpEntity("name=$SCOPE", headers),
                String::class.java
            )
    }
    private fun createCollection(name: String){
        TestRestTemplate()
            .withBasicAuth(couchbaseContainer.username, couchbaseContainer.password)
            .postForEntity(
                "http://${couchbaseContainer.host}:${couchbaseContainer.bootstrapHttpDirectPort}/pools/default/buckets/${bucketDefinition.name}/scopes/$SCOPE/collections",
                HttpEntity("name=$name", headers),
                String::class.java
            )
    }
}