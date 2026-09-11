package order.domain.graph

interface OrderGraphSyncRepository {
    /**
     * MySQL(order_system) 의 주문 데이터를 Neo4j 그래프로 적재한다.
     * 기존 그래프를 비우고 다시 쌓는 전체 재적재 방식.
     */
    fun syncAll(): GraphSyncResult
}
