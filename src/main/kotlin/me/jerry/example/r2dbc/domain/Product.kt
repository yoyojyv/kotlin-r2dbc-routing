package me.jerry.example.r2dbc.domain

import me.jerry.example.webflux.type.YesNoType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("product")
class Product (
    @Id
    var id: Long? = null,
    var productCategoryCode: ProductCategoryType = ProductCategoryType.PROPERTY,
    var supplierId: Long,
    var supplierProductId: String,
    var mainCategoryId: Long,
    var name: String,
    var useYn: YesNoType = YesNoType.N,
    @LastModifiedDate var updatedAt: LocalDateTime? = null,
    @CreatedDate var createdAt: LocalDateTime? = null
) {
    enum class ProductCategoryType {
        PROPERTY,
        COUPON
    }
}
