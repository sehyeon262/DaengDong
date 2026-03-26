package com.e108.be.domain.chat.entity;

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id1", nullable = false)
    private Long memberId1;

    @Column(name = "member_id2", nullable = false)
    private Long memberId2;

    @Column(name = "walk_record_id1", nullable = false)
    private Long walkRecordId1;

    @Column(name = "walk_record_id2", nullable = false)
    private Long walkRecordId2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomStatus status;

    @Builder
    public ChatRoom(Long memberId1, Long memberId2, Long walkRecordId1, Long walkRecordId2) {
        this.memberId1 = memberId1;
        this.memberId2 = memberId2;
        this.walkRecordId1 = walkRecordId1;
        this.walkRecordId2 = walkRecordId2;
        this.status = ChatRoomStatus.ACTIVE;
    }

    public void close() {
        this.status = ChatRoomStatus.CLOSED;
    }
}
