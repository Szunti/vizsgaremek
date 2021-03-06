package hu.progmasters.servicebooker.repository;

import hu.progmasters.servicebooker.domain.entity.Boose;
import hu.progmasters.servicebooker.domain.entity.SpecificPeriod;
import hu.progmasters.servicebooker.domain.entity.SpecificPeriodType;
import hu.progmasters.servicebooker.util.interval.Interval;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class SpecificPeriodRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public SpecificPeriod save(SpecificPeriod toSave) {
        entityManager.persist(toSave);
        return toSave;
    }

    public List<SpecificPeriod> findOverlappingPeriods(Boose boose, SpecificPeriod specificPeriod) {
        // Let the period be [s,e) (closed on s, open on e).
        // [s1, e1) intersects [s2, e2) when:
        // s1 < e2 AND s2 < e1
        return entityManager.createQuery("SELECT sp FROM SpecificPeriod sp " +
                        "WHERE sp.boose = :boose " +
                        "  AND :start < sp.end AND sp.start < :end", SpecificPeriod.class)
                .setParameter("boose", boose)
                .setParameter("start", specificPeriod.getStart())
                .setParameter("end", specificPeriod.getEnd())
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .getResultList();
    }

    public Optional<SpecificPeriod> findById(int id) {
        return Optional.ofNullable(entityManager.find(SpecificPeriod.class, id));
    }

    public List<SpecificPeriod> findAllOrderedFor(Boose boose, Interval<LocalDateTime> interval,
                                                  SpecificPeriodType type, boolean lock) {
        TypedQuery<SpecificPeriod> query = entityManager.createQuery(
                "SELECT sp FROM SpecificPeriod sp WHERE sp.boose = :boose " +
                                "AND sp.start < :intervalEnd AND sp.end > :intervalStart " +
                                "AND (:type IS NULL OR sp.type = :type) " +
                                "ORDER BY sp.start",
                        SpecificPeriod.class)
                .setParameter("boose", boose)
                .setParameter("intervalStart", interval.getStart())
                .setParameter("intervalEnd", interval.getEnd())
                .setParameter("type", type);
        if (lock) {
            query.setLockMode(LockModeType.PESSIMISTIC_READ);
        }
        return query.getResultList();
    }
}
