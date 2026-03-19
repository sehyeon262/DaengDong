package com.e108.be.domain.record.repository;

import com.e108.be.domain.walk.entity.WalkRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RecordRepository extends JpaRepository<WalkRecord, Long> {

    /**
     * 월별 날짜별 산책 횟수 (캘린더 마킹용)
     * 반환: [java.sql.Date, Long] 배열 리스트
     */
    @Query(value = "SELECT CAST(w.start_time AS DATE) AS walk_date, COUNT(*) AS walk_count " +
            "FROM walk_records w " +
            "WHERE w.dog_id = :dogId AND w.walk_status = 'COMPLETED' " +
            "AND w.start_time >= :start AND w.start_time < :end " +
            "GROUP BY CAST(w.start_time AS DATE) " +
            "ORDER BY walk_date",
            nativeQuery = true)
    List<Object[]> findDailyWalkCounts(@Param("dogId") Long dogId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    /**
     * 월별 통계 (총 시간(초), 총 거리(m))
     * 반환: List<Object[]> - [0]: total_seconds, [1]: total_meters
     */
    @Query(value = "SELECT COALESCE(SUM(w.total_duration), 0) AS total_seconds, " +
            "COALESCE(SUM(w.total_distance), 0) AS total_meters " +
            "FROM walk_records w " +
            "WHERE w.dog_id = :dogId AND w.walk_status = 'COMPLETED' " +
            "AND w.start_time >= :start AND w.start_time < :end",
            nativeQuery = true)
    List<Object[]> findMonthlySummary(@Param("dogId") Long dogId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    /**
     * 날짜별 산책 기록 목록
     */
    @Query(value = "SELECT w.* FROM walk_records w " +
            "WHERE w.dog_id = :dogId AND w.walk_status = 'COMPLETED' " +
            "AND CAST(w.start_time AS DATE) = CAST(:date AS DATE) " +
            "ORDER BY w.start_time DESC",
            nativeQuery = true)
    List<WalkRecord> findByDogIdAndDate(@Param("dogId") Long dogId,
                                        @Param("date") LocalDateTime date);
}
