package tn.esprit.traiteurprojet.implementations;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.traiteurprojet.entities.Client;
import tn.esprit.traiteurprojet.entities.Cuisinier;
import tn.esprit.traiteurprojet.entities.Plat;
import tn.esprit.traiteurprojet.repositories.ClientRepo;
import tn.esprit.traiteurprojet.repositories.CuisinierRepo;
import tn.esprit.traiteurprojet.repositories.PlatRepo;
import tn.esprit.traiteurprojet.services.PlatService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class PlatImpl implements PlatService {
    PlatRepo platRepo;
    CuisinierRepo cuisinierRepo;
    ClientRepo clientRepo;

    @Override
    public void ajouterPlatAffecterClientEtCuisinier(Plat plat, Integer idClient, Integer idCuisinier) {
        Client cl = clientRepo.findById(idClient).orElse(null);
        Cuisinier cu = cuisinierRepo.findById(idCuisinier).orElse(null);
        plat.setClient(cl);
        plat.getCuisiniers().add(cu);
        platRepo.save(plat);
    }

    @Override
    public List<Plat> AfficherListePlatsParClient(String nom, String prenom) {
        log.info("Recherche des plats pour le client: {} {}", nom, prenom);

        // 1. Trouver le client par nom et prénom
        Client cc = clientRepo.findByNomAndPrenom(nom, prenom);

        // 2. Vérifier si le client existe
        if (cc == null) {
            log.warn("Client non trouvé: {} {}", nom, prenom);
            throw new EntityNotFoundException("Client non trouvé: " + nom + " " + prenom);
        }

        // 3. Récupérer les plats du client
        Set<Plat> tousPlats = cc.getPlats();
        List<Plat> ll = new ArrayList<>();

        // 4. Vérifier si le client a des plats
        if (tousPlats == null || tousPlats.isEmpty()) {
            log.info("Aucun plat trouvé pour le client: {} {}", nom, prenom);
            return ll; // Retourne une liste vide
        }

        // 5. Ajouter tous les plats à la liste
        for (Plat pp : tousPlats) {
            ll.add(pp);
        }

        log.info("Nombre de plats trouvés pour {} {}: {}", nom, prenom, ll.size());

        // 6. CORRECTION: Retourner la liste (pas d'appel de méthode)
        return ll; // CORRECTION ICI: remplacer ll() par ll
    }
}
