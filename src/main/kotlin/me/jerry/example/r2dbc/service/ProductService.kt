package me.jerry.example.r2dbc.service

import me.jerry.example.r2dbc.domain.Product
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ProductService {

    fun getProduct(id: Long): Mono<Product>

    fun getProducts(ids: List<Long>): Flux<Product>

    fun saveProduct(product: Product): Mono<Product>

    fun saveExamplesProducts(): Flux<Product>

}
