package tz.co.werelay.payment_api.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tz.co.werelay.payment_api.entity.PaymentEntity;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByClientRequestId(String clientRequestId);

    @Query("SELECT p FROM PaymentEntity p " +
            "WHERE p.fromAccount = :fromAccount " +
            "AND p.toAccount = :toAccount " +
            "AND p.amount = :amount " +
            "AND p.createdAt >= :since " +
            "AND p.status = 'SUCCESS'")
    List<PaymentEntity> findRecentDuplicate(@Param("fromAccount") String fromAccount,
            @Param("toAccount") String toAccount,
            @Param("amount") double amount,
            @Param("since") OffsetDateTime since);
}