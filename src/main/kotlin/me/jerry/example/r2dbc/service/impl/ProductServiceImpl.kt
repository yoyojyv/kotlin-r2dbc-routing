package me.jerry.example.r2dbc.service.impl

import me.jerry.example.r2dbc.domain.Product
import me.jerry.example.r2dbc.repository.ProductRepository
import me.jerry.example.r2dbc.service.ProductService
import me.jerry.example.webflux.type.YesNoType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.stream.LongStream

@Transactional(readOnly = true)
@Service
class ProductServiceImpl(private val productRepository: ProductRepository) : ProductService {

    private val logger = LoggerFactory.getLogger(ProductServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun getProduct(id: Long): Mono<Product> {
        return productRepository.findById(id)
    }

    override fun getProducts(ids: List<Long>): Flux<Product> {
        return Flux.fromIterable(ids)
                .flatMap { id: Long -> productRepository.findById(id) }
        // return productRepository.findAllById()
    }

    @Transactional
    override fun saveProduct(product: Product): Mono<Product> {
        return productRepository.findBySupplierIdAndSupplierProductId(product.supplierId, product.supplierProductId)
                .switchIfEmpty(productRepository.save(product))
    }

    @Transactional
    override fun saveExamplesProducts(): Flux<Product> {
        return Flux.fromStream(LongStream.rangeClosed(1, 10).boxed())
                .map { it: Long ->
                    Product(null, Product.ProductCategoryType.PROPERTY, 1L,
                            it.toString(), it % 6 + 1, it.toString(), YesNoType.Y,
                            LocalDateTime.now(), LocalDateTime.now())
                }
                .concatMap { it: Product ->
                    productRepository.findBySupplierIdAndSupplierProductId(it.supplierId, it.supplierProductId)
                            .switchIfEmpty(productRepository.save(it))
                }
    }

}
