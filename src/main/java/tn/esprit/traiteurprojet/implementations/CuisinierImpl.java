package tn.esprit.traiteurprojet.implementations;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.esprit.traiteurprojet.entities.Categorie;
import tn.esprit.traiteurprojet.entities.Cuisinier;
import tn.esprit.traiteurprojet.entities.Plat;
import tn.esprit.traiteurprojet.repositories.CuisinierRepo;
import tn.esprit.traiteurprojet.repositories.PlatRepo;
import tn.esprit.traiteurprojet.services.CuisinierService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
public class CuisinierImpl implements CuisinierService {
    CuisinierRepo cuisinierRepo;
    PlatRepo platRepo;

    @Override
    public  Cuisinier ajouterCuisinier (Cuisinier cuisinier) {
        return cuisinierRepo.save(cuisinier);
    }

@Override
 @Scheduled(fixedDelay = 15000)
 @Transactional
public void AfficherListeCuisinier(){
        List<Cuisinier> list = new ArrayList<>();
        List<Cuisinier> cuisinier = cuisinierRepo.findAll();
      for(Cuisinier c : cuisinier){
          Set<Plat> plats = c.getPlats();
          long p = plats.size();
          for(Plat p1 : plats){
              if(p>=2 && p1.getCategorie()== Categorie.PRINCIPAL)
                  list.add(c);
          }

      }
      for(Cuisinier c : list){
          log.info(c.getNom());
      }
}

}

