package order.domain.order

import order.api.dto.OrderRequest
import order.domain.menu.MenuRepository
import order.domain.user.UserCredit
import order.domain.user.UserCreditRepository
import org.springframework.stereotype.Component

@Component
class OrderValidation(
    private val menuRepository: MenuRepository,
    private val userCreditRepository: UserCreditRepository
) {
    fun validate(request: OrderRequest): UserCredit {
        val existsMenu = menuRepository.existsByIds(request.orderDetails.map { it.menuId })
        if (!existsMenu) {
            throw IllegalArgumentException("주문에 포함된 메뉴가 존재하지 않습니다.")
        }

        val user = userCreditRepository.findById(request.userId)
            ?: throw NoSuchElementException("존재하지 않는 사용자입니다.")
        if (!user.canPay(request.price)) {
            throw IllegalArgumentException("사용자의 크레딧이 부족합니다.")
        }

        return user
    }
}