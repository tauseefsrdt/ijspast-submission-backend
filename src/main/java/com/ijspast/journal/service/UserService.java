package com.ijspast.journal.service;

import com.ijspast.journal.dto.request.CreateReviewerRequest;
import com.ijspast.journal.dto.response.ReviewerProfileDTO;
import com.ijspast.journal.entity.User;

import java.util.List;
import java.util.Map;

public interface UserService {

    List<ReviewerProfileDTO> getReviewers(String search, String domain);

    Map<String, Object> createReviewer(CreateReviewerRequest request);

    ReviewerProfileDTO updateReviewer(String id, com.ijspast.journal.dto.request.UpdateReviewerRequest request);

    void deleteReviewer(String id);

    User getCurrentAuthenticatedUser();

    User getUserById(String id);
}
