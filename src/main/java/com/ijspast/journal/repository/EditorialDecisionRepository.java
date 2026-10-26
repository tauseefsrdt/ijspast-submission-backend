package com.ijspast.journal.repository;

import com.ijspast.journal.entity.EditorialDecision;
import com.ijspast.journal.entity.Manuscript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EditorialDecisionRepository extends JpaRepository<EditorialDecision, String> {

    List<EditorialDecision> findByManuscriptOrderByDecidedAtDesc(Manuscript manuscript);

    List<EditorialDecision> findByManuscriptIdOrderByDecidedAtDesc(String manuscriptId);
}
