package ma.gestionlocative.locataires.controller;

import jakarta.validation.Valid;
import ma.gestionlocative.locataires.dto.LocataireQuittanceDto;
import ma.gestionlocative.locataires.dto.LocataireRequestDto;
import ma.gestionlocative.locataires.dto.LocataireResponseDto;
import ma.gestionlocative.locataires.service.LocataireService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locataires")
@CrossOrigin(origins = "*")
public class LocataireController {

    private final LocataireService service;

    public LocataireController(LocataireService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LocataireResponseDto> createLocataire(@Valid @RequestBody LocataireRequestDto dto) {
        LocataireResponseDto response = service.createLocataire(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocataireResponseDto> getLocataireById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/cin/{cin}")
    public ResponseEntity<LocataireResponseDto> getLocataireByCin(@PathVariable String cin) {
        return ResponseEntity.ok(service.findByCin(cin));
    }

    @GetMapping
    public ResponseEntity<Page<LocataireResponseDto>> getAllLocataires(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(service.findAll(search, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocataireResponseDto> updateLocataire(
            @PathVariable UUID id,
            @Valid @RequestBody LocataireRequestDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocataire(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/quittance-summary")
    public ResponseEntity<LocataireQuittanceDto> getQuittanceSummary(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getQuittanceSummary(id));
    }
}
