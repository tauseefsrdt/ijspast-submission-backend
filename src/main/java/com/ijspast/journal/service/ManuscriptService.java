package com.ijspast.journal.service;

import com.ijspast.journal.dto.request.AssignReviewerRequest;
import com.ijspast.journal.dto.request.EditorialDecisionRequest;
import com.ijspast.journal.dto.request.ManuscriptSubmissionRequest;
import com.ijspast.journal.dto.request.RevisionSubmissionRequest;
import com.ijspast.journal.dto.response.ManuscriptResponseDTO;
import com.ijspast.journal.dto.response.SubmissionResponseDTO;
import com.ijspast.journal.entity.ManuscriptFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ManuscriptService {

    SubmissionResponseDTO submitManuscript(ManuscriptSubmissionRequest request, MultipartFile file);

    List<ManuscriptResponseDTO> getAuthorManuscripts(String status, String search);

    ManuscriptResponseDTO getAuthorManuscriptById(String id);

    ManuscriptResponseDTO submitAuthorRevision(String id, RevisionSubmissionRequest request, MultipartFile file);

    List<ManuscriptResponseDTO> getEditorManuscripts(String status, String domain, String search);

    ManuscriptResponseDTO getEditorManuscriptById(String id);

    ManuscriptResponseDTO assignReviewer(String manuscriptId, AssignReviewerRequest request);

    ManuscriptResponseDTO enableStage2Review(String manuscriptId);

    ManuscriptResponseDTO forwardRevisionToReviewer(String manuscriptId, com.ijspast.journal.dto.request.ForwardRevisionRequest request);

    ManuscriptResponseDTO makeEditorialDecision(String manuscriptId, EditorialDecisionRequest request);

    List<ManuscriptResponseDTO> getPublishedManuscripts(String search, String domain);

    ManuscriptFile getAuthorizedFile(String fileId);

    ManuscriptResponseDTO reassignReviewer(String manuscriptId, com.ijspast.journal.dto.request.ReassignReviewerRequest request);

    com.ijspast.journal.dto.response.CommunicationThreadDTO getCommunications(String manuscriptId);

    com.ijspast.journal.dto.response.CommunicationThreadDTO sendMessage(String manuscriptId, com.ijspast.journal.dto.request.SendMessageRequest request);

    List<com.ijspast.journal.dto.response.AuthorTableDTO> getAuthorsTable();

    List<com.ijspast.journal.dto.response.ReviewerTableDTO> getReviewersTable();
}
