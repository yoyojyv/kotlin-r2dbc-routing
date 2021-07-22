package me.jerry.example.r2dbc.repository

import me.jerry.example.r2dbc.domain.Product
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface ProductRepository : ReactiveCrudRepository<Product, Long> {

    @Query("SELECT * FROM product WHERE supplier_id = :supplierId AND supplier_product_id = :supplierProductId ")
    fun findBySupplierIdAndSupplierProductId(supplierId: Long, supplierProductId: String): Mono<Product>

}
