package com.gestionlocative.quittances.suivi;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quittances")
public class SuiviQuittanceController {
    private final TableauBordService tableau;
    private final ExportCsvService csv;
    private final ReconciliationPaiementsService reconciliation;
    public SuiviQuittanceController(TableauBordService tableau, ExportCsvService csv, ReconciliationPaiementsService reconciliation) {
        this.tableau = tableau; this.csv = csv; this.reconciliation = reconciliation;
    }
    @GetMapping("/tableau-de-bord")
    public ResponseEntity<TableauBordService.Tableau> tableau(Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tableau.consulter(auth));
    }
    @GetMapping(value = "/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exporter(Authentication auth,
            @RequestParam(name="tri", defaultValue="creeLe") String tri,
            @RequestParam(name="sens", defaultValue="desc") String sens,
            @RequestParam(name="bailId", required=false) String bailId,
            @RequestParam(name="numero", required=false) String numero,
            @RequestParam(name="statut", required=false) String statut,
            @RequestParam(name="debut", required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(name="fin", required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quittances.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.exporter(auth, tri, sens, bailId, numero, statut, debut, fin));
    }
    @GetMapping("/{id}/controle-paiements")
    public ResponseEntity<ReconciliationPaiementsService.Rapport> controler(Authentication auth, @PathVariable("id") UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reconciliation.controler(auth, id));
    }
}
