package com.ijspast.journal.mapper;

import com.ijspast.journal.dto.response.ReviewerProfileDTO;
import com.ijspast.journal.dto.response.UserResponseDTO;
import com.ijspast.journal.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDTO toUserResponseDTO(User user) {
        if (user == null) {
            return null;
        }
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().getSimpleName());
        dto.setInstitution(user.getInstitution());
        dto.setDepartment(user.getDepartment());
        dto.setDomain(user.getDomain());
        if (user.getCreatedAt() != null) {
            dto.setCreatedAt(user.getCreatedAt().toString());
        }
        return dto;
    }

    public ReviewerProfileDTO toReviewerProfileDTO(User user, long activeCount, long completedCount) {
        if (user == null) {
            return null;
        }
        ReviewerProfileDTO dto = new ReviewerProfileDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setInstitution(user.getInstitution());
        dto.setDomain(user.getDomain());
        dto.setActiveAssignmentsCount(activeCount);
        dto.setTotalCompletedReviews(completedCount);
        return dto;
    }
}
