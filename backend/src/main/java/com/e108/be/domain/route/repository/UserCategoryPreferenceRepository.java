package com.e108.be.domain.route.repository;

import com.e108.be.domain.route.entity.UserCategoryPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCategoryPreferenceRepository extends JpaRepository<UserCategoryPreference, Long> {

    List<UserCategoryPreference> findByMemberId(Long memberId);

    Optional<UserCategoryPreference> findByMemberIdAndCategory_Id(Long memberId, Integer categoryId);
}
