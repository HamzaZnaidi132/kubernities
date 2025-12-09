package tn.esprit.traiteurprojet.controlleurs;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.traiteurprojet.entities.Client;
import tn.esprit.traiteurprojet.entities.Cuisinier;
import tn.esprit.traiteurprojet.services.CuisinierService;

@RestController
@RequestMapping("/cuisinier")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class CuisinierRestControlleur {
    private CuisinierService cuisinierService;
    @PostMapping("/add-cuisinier")
       Cuisinier ajouterCuisinier(@RequestBody Cuisinier cuisinier) {
        return cuisinierService.ajouterCuisinier(cuisinier);
    }
}
