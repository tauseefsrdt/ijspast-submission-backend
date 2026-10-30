package com.ijspast.journal.service;

import com.ijspast.journal.dto.request.AuthorRegistrationRequest;
import com.ijspast.journal.dto.request.CreateEditorRequest;
import com.ijspast.journal.dto.request.CreateReviewerRequest;
import com.ijspast.journal.dto.request.UpdateProfileRequest;
import com.ijspast.journal.dto.request.UpdateReviewerRequest;
import com.ijspast.journal.dto.response.ReviewerProfileDTO;
import com.ijspast.journal.dto.response.UserResponseDTO;
import com.ijspast.journal.entity.User;

import java.util.List;
import java.util.Map;

public interface UserService {

    List<ReviewerProfileDTO> getReviewers(String search, String domain);

    Map<String, Object> createReviewer(CreateReviewerRequest request);

    ReviewerProfileDTO updateReviewer(String id, UpdateReviewerRequest request);

    void deleteReviewer(String id);

    User getCurrentAuthenticatedUser();

    User getUserById(String id);

    UserResponseDTO createEditor(CreateEditorRequest request);

    UserResponseDTO updateProfile(String userId, UpdateProfileRequest request);

    UserResponseDTO registerAuthor(AuthorRegistrationRequest request);
}
