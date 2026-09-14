package com.house.houseviewing.api.query.repository;

import com.house.houseviewing.api.query.dto.AnalysisHistoryRow;
import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import com.house.houseviewing.domain.common.RiskLevel;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.house.houseviewing.domain.analysis.postanalysis.entity.QPostAnalysisEntity.postAnalysisEntity;
import static com.house.houseviewing.domain.analysis.preanalysis.entity.QPreAnalysisEntity.preAnalysisEntity;

@Repository
public class AnalysisHistoryQueryRepository {

    private static final Expression<String> POST = Expressions.constant("POST");
    private static final Expression<String> PRE = Expressions.constant("PRE");

    private final JPAQueryFactory queryFactory;

    public AnalysisHistoryQueryRepository(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    public List<AnalysisHistoryRow> findPostAnalyses(Long userId, RiskLevel riskLevel, long offset, long limit) {
        BooleanBuilder where = postWhere(userId, riskLevel);

        return queryFactory
                .select(Projections.constructor(
                        AnalysisHistoryRow.class,
                        postAnalysisEntity.id,
                        postAnalysisEntity.createdAt,
                        POST,
                        postAnalysisEntity.pdfReport.id,
                        postAnalysisEntity.house.nickname,
                        postAnalysisEntity.house.address.addressName,
                        postAnalysisEntity.mainReason,
                        postAnalysisEntity.riskLevel,
                        postAnalysisEntity.ltvScore
                ))
                .from(postAnalysisEntity)
                .join(postAnalysisEntity.house)
                .join(postAnalysisEntity.house.user)
                .leftJoin(postAnalysisEntity.pdfReport)
                .where(where)
                .orderBy(postAnalysisEntity.createdAt.desc(), postAnalysisEntity.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public List<AnalysisHistoryRow> findPreAnalyses(Long userId, RiskLevel riskLevel, long offset, long limit) {
        BooleanBuilder where = preWhere(userId, riskLevel);

        return queryFactory
                .select(Projections.constructor(
                        AnalysisHistoryRow.class,
                        preAnalysisEntity.id,
                        preAnalysisEntity.createdAt,
                        PRE,
                        preAnalysisEntity.preReportEntity.id,
                        preAnalysisEntity.nickname,
                        preAnalysisEntity.address.addressName,
                        preAnalysisEntity.mainReason,
                        preAnalysisEntity.riskLevel,
                        preAnalysisEntity.ltvScore
                ))
                .from(preAnalysisEntity)
                .join(preAnalysisEntity.user)
                .leftJoin(preAnalysisEntity.preReportEntity)
                .where(where)
                .orderBy(preAnalysisEntity.createdAt.desc(), preAnalysisEntity.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public List<AnalysisHistoryRow> findDiffAnalyses(Long userId, RiskLevel riskLevel, long offset, long limit) {
        BooleanBuilder where = postWhere(userId, riskLevel)
                .and(postAnalysisEntity.analysisType.eq(AnalysisType.DIFF));

        return queryFactory
                .select(Projections.constructor(
                        AnalysisHistoryRow.class,
                        postAnalysisEntity.id,
                        postAnalysisEntity.createdAt,
                        POST,
                        postAnalysisEntity.pdfReport.id,
                        postAnalysisEntity.house.nickname,
                        postAnalysisEntity.house.address.addressName,
                        postAnalysisEntity.mainReason,
                        postAnalysisEntity.riskLevel,
                        postAnalysisEntity.ltvScore
                ))
                .from(postAnalysisEntity)
                .join(postAnalysisEntity.house)
                .join(postAnalysisEntity.house.user)
                .leftJoin(postAnalysisEntity.pdfReport)
                .where(where)
                .orderBy(postAnalysisEntity.createdAt.desc(), postAnalysisEntity.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    private BooleanBuilder postWhere(Long userId, RiskLevel riskLevel) {
        BooleanBuilder where = new BooleanBuilder(postAnalysisEntity.house.user.id.eq(userId));
        if (riskLevel != null) {
            where.and(postAnalysisEntity.riskLevel.eq(riskLevel));
        }
        return where;
    }

    private BooleanBuilder preWhere(Long userId, RiskLevel riskLevel) {
        BooleanBuilder where = new BooleanBuilder(preAnalysisEntity.user.id.eq(userId));
        if (riskLevel != null) {
            where.and(preAnalysisEntity.riskLevel.eq(riskLevel));
        }
        return where;
    }
}
