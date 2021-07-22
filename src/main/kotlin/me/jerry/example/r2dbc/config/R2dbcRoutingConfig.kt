package me.jerry.example.r2dbc.config

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.pool.PoolingConnectionFactoryProvider
import io.r2dbc.spi.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy
import org.springframework.r2dbc.connection.lookup.AbstractRoutingConnectionFactory
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.reactive.TransactionSynchronizationManager
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Duration.ofSeconds

@Configuration
class R2dbcRoutingConfig {

    @Configuration
    @EnableR2dbcRepositories(basePackages = ["me.jerry.example"])
    @EnableTransactionManagement
    class RoutingR2dbcConfig : AbstractR2dbcConfiguration() {

        @Bean("readDbPoolSettings")
        @ConfigurationProperties(prefix = "datasource.read")
        fun readDbPoolSettings(): R2dbcPoolSettings {
            return R2dbcPoolSettings()
        }

        @Bean("readConnectionFactory")
        fun readConnectionFactory(): ConnectionFactory {
            return createNewConnectionPool(readDbPoolSettings())
        }

        @Bean("readTransactionManager")
        fun readTransactionManager(): ReactiveTransactionManager {
            val readOnly = R2dbcTransactionManager(readConnectionFactory())
            readOnly.isEnforceReadOnly = true
            return readOnly
        }

        @Bean("writeDbPoolSettings")
        @ConfigurationProperties(prefix = "datasource.write")
        fun writeDbPoolSettings(): R2dbcPoolSettings {
            return R2dbcPoolSettings()
        }

        @Bean("writeConnectionFactory")
        fun writeConnectionFactory(): ConnectionFactory {
            return createNewConnectionPool(writeDbPoolSettings())
        }

        @Bean("writeTransactionManager")
        fun writeTransactionManager(): ReactiveTransactionManager {
            return R2dbcTransactionManager(writeConnectionFactory())
        }

        @Primary
        @Bean("connectionFactory")
        override fun connectionFactory(): ConnectionFactory {

            val routingConnectionFactory = RoutingConnectionFactory()

            val factories = mapOf(
                    RoutingConnectionFactory.TRANSACTION_READ to readConnectionFactory(),
                    RoutingConnectionFactory.TRANSACTION_WRITE to writeConnectionFactory()
            )

            routingConnectionFactory.setLenientFallback(true)
            routingConnectionFactory.setDefaultTargetConnectionFactory(writeConnectionFactory())
            routingConnectionFactory.setTargetConnectionFactories(factories)
            return routingConnectionFactory
        }

        @Primary
        @Bean("defaultTransactionManager")
        fun defaultTransactionManager(): ReactiveTransactionManager {
            return R2dbcTransactionManager(TransactionAwareConnectionFactoryProxy(connectionFactory()))
        }

    }

    class RoutingConnectionFactory : AbstractRoutingConnectionFactory() {

        private val logger = LoggerFactory.getLogger(RoutingConnectionFactory::class.java)

        override fun determineCurrentLookupKey(): Mono<Any> {

            return TransactionSynchronizationManager.forCurrentTransaction()
                    .flatMap { transactionManager ->

                        logger.info("it.getCurrentTransactionName() : {}", transactionManager.currentTransactionName)
                        logger.info("it.isActualTransactionActive() : {}", transactionManager.isActualTransactionActive)
                        logger.info("it.isCurrentTransactionReadOnly() : {}", transactionManager.isCurrentTransactionReadOnly)

                        val dataSourceType = if (transactionManager.isActualTransactionActive) {

                            logger.info("isActualTransactionActive....")

                            if (transactionManager.isCurrentTransactionReadOnly) {
                                TRANSACTION_READ
                            } else {
                                TRANSACTION_WRITE
                            }

                        } else {
                            TRANSACTION_WRITE
                        }
                        logger.info("> current dataSourceType : {}", dataSourceType)
                        Mono.just(dataSourceType)
            }
        }

        companion object {
            const val TRANSACTION_READ = "read"
            const val TRANSACTION_WRITE = "write"
        }
    }

    data class R2dbcPoolSettings(
            var driver: String = "pool",
            var protocol: String = "mysql",
            var host: String = "localhost",
            var port: Int = 3306,
            var username: String = "root",
            var password: String = "password",
            var database: String = "test",
            var connectionTimeout: Duration = Duration.ofSeconds(10),
            var poolName: String = "pool",
            var initialSize: Int = 20,
            var maxSize: Int = 20,
            var maxIdleTime: Duration = Duration.ofSeconds(15),
            var maxLifeTime: Duration = Duration.ofSeconds(20),
            var maxCreateConnectionTime: Duration = Duration.ofSeconds(2),
            var maxAcquireTime: Duration = Duration.ofSeconds(3),
            var acquireRetry: Int = 1
    )

    // ============================= private helper methods  =============================

    companion object {
        private fun createNewConnectionPool(settings: R2dbcPoolSettings): ConnectionPool {
            val connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
                    .option(ConnectionFactoryOptions.DRIVER, settings.driver)
                    .option(ConnectionFactoryOptions.PROTOCOL, settings.protocol)
                    .option(ConnectionFactoryOptions.HOST, settings.host)
                    .option(ConnectionFactoryOptions.PORT, settings.port)
                    .option(ConnectionFactoryOptions.USER, settings.username)
                    .option(ConnectionFactoryOptions.PASSWORD, settings.password)
                    .option(ConnectionFactoryOptions.DATABASE, settings.database)
                    .option(ConnectionFactoryOptions.CONNECT_TIMEOUT, settings.connectionTimeout)
                    .option(ConnectionFactoryOptions.SSL, false)
                    .option(Option.valueOf("zeroDate"), "use_null")

                    .option(PoolingConnectionFactoryProvider.MAX_SIZE, settings.maxSize)
                    .option(PoolingConnectionFactoryProvider.INITIAL_SIZE, settings.initialSize)

                    .option(PoolingConnectionFactoryProvider.MAX_IDLE_TIME, settings.maxIdleTime)
                    .option(PoolingConnectionFactoryProvider.MAX_LIFE_TIME, settings.maxLifeTime)

                    .option(PoolingConnectionFactoryProvider.MAX_CREATE_CONNECTION_TIME, ofSeconds(3))
                    .option(PoolingConnectionFactoryProvider.MAX_ACQUIRE_TIME, ofSeconds(4))
                    .option(PoolingConnectionFactoryProvider.ACQUIRE_RETRY, settings.acquireRetry)

                    .option(PoolingConnectionFactoryProvider.VALIDATION_DEPTH, ValidationDepth.LOCAL)
//                .option(PoolingConnectionFactoryProvider.VALIDATION_QUERY, "select 1")
//                .option(PoolingConnectionFactoryProvider.VALIDATION_DEPTH, ValidationDepth.REMOTE)
                .build()
            )
            val configuration = createNewConnectionPoolBuilder(connectionFactory, settings).build()
            return ConnectionPool(configuration)
        }

        private fun createNewConnectionPoolBuilder(connectionFactory: ConnectionFactory, settings: R2dbcPoolSettings): ConnectionPoolConfiguration.Builder {
            return ConnectionPoolConfiguration.builder(connectionFactory)
                    .name(settings.poolName)

                    .initialSize(settings.initialSize)
                    .maxSize(settings.maxSize)

                    .maxIdleTime(settings.maxIdleTime)
                    .maxLifeTime(settings.maxLifeTime)

                    .maxCreateConnectionTime(settings.maxCreateConnectionTime)
                    .maxAcquireTime(settings.maxAcquireTime)
                    .acquireRetry(settings.acquireRetry)

                    .validationDepth(ValidationDepth.LOCAL)
//                .validationQuery("select 1")
                    .registerJmx(true)
        }
    }

}
