package com.ijspast.journal.repository;

import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.ManuscriptFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManuscriptFileRepository extends JpaRepository<ManuscriptFile, String> {

    List<ManuscriptFile> findByManuscriptOrderByVersionAsc(Manuscript manuscript);

    List<ManuscriptFile> findByManuscriptAndIsRevisedFalse(Manuscript manuscript);

    List<ManuscriptFile> findByManuscriptAndIsRevisedTrueOrderByVersionAsc(Manuscript manuscript);
}
