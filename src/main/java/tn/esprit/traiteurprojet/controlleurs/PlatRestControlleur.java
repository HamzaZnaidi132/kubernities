package tn.esprit.traiteurprojet.controlleurs;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.traiteurprojet.entities.Cuisinier;
import tn.esprit.traiteurprojet.entities.Plat;
import tn.esprit.traiteurprojet.services.CuisinierService;
import tn.esprit.traiteurprojet.services.PlatService;

import java.util.List;

@RestController

@RequestMapping("/plat")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class PlatRestControlleur {
    @Autowired
    private PlatService platService;
    private CuisinierService cuisinierService;

    @PostMapping("/ajouter-plat/{icl}/{icu}")
   public void ajouterplat(@RequestBody Plat c, @PathVariable("icl") Integer icl, @PathVariable("icu") Integer icu) {
         platService.ajouterPlatAffecterClientEtCuisinier(c,icu,icl);
    }
    @GetMapping("/afficher-plat/{nom}/{prenom}")
    public ResponseEntity<List<Plat>> AfficherListePlatsParClient(
            @PathVariable("nom") String nom,
            @PathVariable("prenom") String prenom) {

        try {
            // Appel du service qui retourne List<Plat>
            List<Plat> plats = platService.AfficherListePlatsParClient(nom, prenom);
            return ResponseEntity.ok(plats);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
