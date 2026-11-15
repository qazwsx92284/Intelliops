package org.intllps.intelliops.controller;

import jakarta.validation.Valid;
import org.intllps.intelliops.dto.IncidentDetailDto;
import org.intllps.intelliops.dto.IncidentSummaryDto;
import org.intllps.intelliops.exception.ApiException;
import org.intllps.intelliops.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public ResponseEntity<List<IncidentSummaryDto>> getAllIncidents() {
        return ResponseEntity.ok(incidentService.getAllIncidents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentDetailDto> getIncidentById(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getIncidentById(id));
    }

    @PostMapping
    public ResponseEntity<IncidentDetailDto> createIncident(@Valid @RequestBody IncidentDetailDto request) {
        throw new ApiException("Incident creation is not implemented in Phase 1.");
    }
}

