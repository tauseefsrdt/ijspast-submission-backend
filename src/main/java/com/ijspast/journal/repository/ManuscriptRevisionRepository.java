package com.ijspast.journal.repository;

import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.ManuscriptRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManuscriptRevisionRepository extends JpaRepository<ManuscriptRevision, String> {

    List<ManuscriptRevision> findByManuscriptOrderByRoundNumberAsc(Manuscript manuscript);

    List<ManuscriptRevision> findByManuscriptIdOrderByRoundNumberAsc(String manuscriptId);

    Optional<ManuscriptRevision> findByManuscriptAndRoundNumber(Manuscript manuscript, int roundNumber);
}
