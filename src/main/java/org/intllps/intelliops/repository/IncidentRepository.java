package org.intllps.intelliops.repository;

import org.intllps.intelliops.entity.Incident;
import org.intllps.intelliops.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByStatus(IncidentStatus status);
}

