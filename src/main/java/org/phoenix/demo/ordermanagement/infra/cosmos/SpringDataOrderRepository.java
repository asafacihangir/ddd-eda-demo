package org.phoenix.demo.ordermanagement.infra.cosmos;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataOrderRepository extends CosmosRepository<OrderCosmosDocument, String> {

    @Query(value = "select * from c where c.id = @id and c.tenantId = @tenantId and c._type = 'order'")
    List<OrderCosmosDocument> findOrderByIdAndTenantId(String id, String tenantId);
}