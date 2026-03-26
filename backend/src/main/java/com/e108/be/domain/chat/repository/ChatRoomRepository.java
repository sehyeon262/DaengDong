package com.e108.be.domain.chat.repository;

import com.e108.be.domain.chat.entity.ChatRoom;
import com.e108.be.domain.chat.entity.ChatRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT r FROM ChatRoom r WHERE (r.walkRecordId1 = :walkRecordId OR r.walkRecordId2 = :walkRecordId) AND r.status = :status")
    List<ChatRoom> findByWalkRecordIdAndStatus(@Param("walkRecordId") Long walkRecordId,
                                               @Param("status") ChatRoomStatus status);
}
