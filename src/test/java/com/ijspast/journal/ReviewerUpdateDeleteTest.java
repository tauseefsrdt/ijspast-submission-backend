package com.ijspast.journal;

import com.ijspast.journal.dto.request.CreateReviewerRequest;
import com.ijspast.journal.dto.request.UpdateReviewerRequest;
import com.ijspast.journal.dto.response.ReviewerProfileDTO;
import com.ijspast.journal.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ReviewerUpdateDeleteTest {

    @Autowired
    private UserService userService;

    @Test
    @Transactional
    public void testCreateUpdateDeleteReviewer() {
        CreateReviewerRequest createReq = new CreateReviewerRequest();
        createReq.setName("Dr. Testing Reviewer");
        createReq.setEmail("test.reviewer.unique@test.edu");
        createReq.setPhone("9876543210");
        createReq.setInstitution("Test University");
        createReq.setDomain("Computer Science");

        Map<String, Object> created = userService.createReviewer(createReq);
        assertNotNull(created);
        ReviewerProfileDTO profile = (ReviewerProfileDTO) created.get("reviewer");
        assertNotNull(profile);
        String reviewerId = profile.getId();

        // Update Reviewer
        UpdateReviewerRequest updateReq = new UpdateReviewerRequest(
                "Dr. Testing Reviewer Updated",
                "test.reviewer.unique@test.edu",
                "9998887776",
                "Updated University",
                "Dept of AI",
                "Artificial Intelligence"
        );
        ReviewerProfileDTO updated = userService.updateReviewer(reviewerId, updateReq);
        assertNotNull(updated);
        assertEquals("Dr. Testing Reviewer Updated", updated.getName());
        assertEquals("Updated University", updated.getInstitution());
        assertEquals("Artificial Intelligence", updated.getDomain());

        // Delete Reviewer
        assertDoesNotThrow(() -> userService.deleteReviewer(reviewerId));
    }
}
