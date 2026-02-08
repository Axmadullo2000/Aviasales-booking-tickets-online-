package com.aviasales.booking.booking.repo;

import com.aviasales.booking.booking.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository для работы с правилами ценообразования
 */
@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    /**
     * Найти все активные правила определённого типа
     */
    List<PricingRule> findByRuleTypeAndIsActiveTrueOrderByPriorityDesc(String ruleType);

    /**
     * Найти все активные правила
     */
    List<PricingRule> findByIsActiveTrueOrderByPriorityDesc();

    /**
     * Найти правило по типу, ключу и значению
     */
    @Query("SELECT pr FROM PricingRule pr WHERE pr.ruleType = :ruleType " +
            "AND pr.conditionKey = :conditionKey " +
            "AND pr.conditionValue = :conditionValue " +
            "AND pr.isActive = true")
    List<PricingRule> findMatchingRules(
            @Param("ruleType") String ruleType,
            @Param("conditionKey") String conditionKey,
            @Param("conditionValue") String conditionValue
    );

    /**
     * Найти правила заблаговременной покупки
     */
    @Query("SELECT pr FROM PricingRule pr WHERE pr.ruleType = 'ADVANCE_PURCHASE' " +
            "AND pr.isActive = true ORDER BY pr.priority DESC")
    List<PricingRule> findAdvancePurchaseRules();

    /**
     * Найти правила загруженности
     */
    @Query("SELECT pr FROM PricingRule pr WHERE pr.ruleType = 'OCCUPANCY' " +
            "AND pr.isActive = true ORDER BY pr.priority DESC")
    List<PricingRule> findOccupancyRules();

    /**
     * Найти правила дня недели
     */
    @Query("SELECT pr FROM PricingRule pr WHERE pr.ruleType = 'DAY_OF_WEEK' " +
            "AND pr.isActive = true ORDER BY pr.priority DESC")
    List<PricingRule> findDayOfWeekRules();
}
