package com.ijspast.journal.repository;

import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.ManuscriptCommunication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManuscriptCommunicationRepository extends JpaRepository<ManuscriptCommunication, String> {

    List<ManuscriptCommunication> findByManuscriptOrderBySentAtAsc(Manuscript manuscript);

    List<ManuscriptCommunication> findByManuscriptIdOrderBySentAtAsc(String manuscriptId);

    long countByManuscript(Manuscript manuscript);

    long countByManuscriptId(String manuscriptId);
}
