package com.callwrap.review.repository;

import com.callwrap.review.domain.Complainant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ComplainantRepository extends JpaRepository<Complainant, UUID> {

    Optional<Complainant> findByPhone(String phone);

    @Query("SELECT c FROM Complainant c "
         + "WHERE :query = '' "
         + "OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) "
         + "OR LOWER(COALESCE(c.phone, '')) LIKE LOWER(CONCAT('%', :query, '%')) "
         + "OR LOWER(COALESCE(c.org, '')) LIKE LOWER(CONCAT('%', :query, '%')) "
         + "ORDER BY c.updatedAt DESC")
    Page<Complainant> search(@Param("query") String query, Pageable pageable);

    /**
     * 전화번호로 같은 사람을 찾아 upsert한다 — 동시에 같은(신규) 번호로 콜 두 건이 들어와도
     * uq_complainants_phone(UNIQUE) 충돌 없이 하나로 합쳐진다(DB가 원자적으로 처리).
     * name/org는 이번 콜에 값이 있을 때만 갱신한다(COALESCE) — 예전 콜엔 전화번호만 있고
     * 나중 콜에 이름까지 왔다고 해서 그 사이 값이 비어버리면 안 된다.
     */
    @Modifying
    @Query(value = """
        INSERT INTO complainants (id, name, phone, org, created_at, updated_at)
        VALUES (:id, :name, :phone, :org, now(), now())
        ON CONFLICT (phone) DO UPDATE SET
            name = COALESCE(EXCLUDED.name, complainants.name),
            org = COALESCE(EXCLUDED.org, complainants.org),
            updated_at = now()
        """, nativeQuery = true)
    void upsertByPhone(@Param("id") UUID id, @Param("name") String name,
                        @Param("phone") String phone, @Param("org") String org);
}
