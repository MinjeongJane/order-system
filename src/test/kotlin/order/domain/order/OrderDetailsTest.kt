package order.domain.order

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime

class OrderDetailsTest : DescribeSpec({

    describe("OrderDetails 생성") {
        context("유효한 값으로 생성할 때") {
            it("올바른 값을 가진 객체가 생성된다") {
                // given
                val now = LocalDateTime.now()
                val orderHistory = OrderHistory(
                    id = 1L,
                    userId = 1L,
                    price = 6000,
                    createdBy = "system",
                    createdAt = now,
                    modifiedBy = "system",
                    modifiedAt = now
                )

                // when
                val orderDetails = OrderDetails(
                    id = 1L,
                    orderId = orderHistory,
                    menuId = 10,
                    count = 2,
                    menuPrice = 3000,
                    createdBy = "system",
                    createdAt = now,
                    modifiedBy = "system",
                    modifiedAt = now
                )

                // then
                orderDetails.id shouldBe 1L
                orderDetails.menuId shouldBe 10
                orderDetails.count shouldBe 2
                orderDetails.menuPrice shouldBe 3000
                orderDetails.orderId shouldBe orderHistory
            }
        }

        context("동일한 값으로 두 객체를 생성할 때") {
            it("두 객체는 동등하다") {
                // given
                val now = LocalDateTime.now()
                val orderHistory = OrderHistory(
                    id = 1L,
                    userId = 1L,
                    price = 3000,
                    createdBy = "system",
                    createdAt = now,
                    modifiedBy = "system",
                    modifiedAt = now
                )
                val details1 = OrderDetails(
                    id = 1L,
                    orderId = orderHistory,
                    menuId = 5,
                    count = 1,
                    menuPrice = 3000,
                    createdBy = "system",
                    createdAt = now,
                    modifiedBy = "system",
                    modifiedAt = now
                )
                val details2 = OrderDetails(
                    id = 1L,
                    orderId = orderHistory,
                    menuId = 5,
                    count = 1,
                    menuPrice = 3000,
                    createdBy = "system",
                    createdAt = now,
                    modifiedBy = "system",
                    modifiedAt = now
                )

                // then
                details1 shouldBe details2
                details1.hashCode() shouldBe details2.hashCode()
            }
        }

        context("다른 값으로 두 객체를 생성할 때") {
            it("두 객체는 동등하지 않다") {
                // given
                val now = LocalDateTime.now()
                val orderHistory = OrderHistory(
                    id = 1L,
                    userId = 1L,
                    price = 3000,
                    createdBy = "system",
                    createdAt = now,
                    modifiedBy = "system",
                    modifiedAt = now
                )
                val details1 = OrderDetails(
                    id = 1L,
                    orderId = orderHistory,
                    menuId = 5,
                    count = 1,
                    menuPrice = 3000,
                    createdBy = "system",
                    createdAt = now,
                    modifiedBy = "system",
                    modifiedAt = now
                )
                val details2 = OrderDetails(
                    id = 2L,
                    orderId = orderHistory,
                    menuId = 5,
                    count = 3,
                    menuPrice = 3000,
                    createdBy = "system",
                    createdAt = now,
                    modifiedBy = "system",
                    modifiedAt = now
                )

                // then
                details1 shouldNotBe details2
            }
        }
    }
})
