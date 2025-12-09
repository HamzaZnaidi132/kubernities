package tn.esprit.traiteurprojet.implementations;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.traiteurprojet.entities.Client;
import tn.esprit.traiteurprojet.entities.Imc;
import tn.esprit.traiteurprojet.entities.Plat;
import tn.esprit.traiteurprojet.repositories.ClientRepo;

import tn.esprit.traiteurprojet.repositories.PlatRepo;
import tn.esprit.traiteurprojet.services.ClientService;

import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor

public class ClientImpl implements ClientService {
    @Autowired
    ClientRepo clientRepo;

    @Override
    public Client ajouterClient(Client client){
        return clientRepo.save(client);
    }


    @Override
    public float MontantApayerParClient(Integer idClient) {
        float total = 0;

        Client ccc = clientRepo.findById(idClient).orElse(null);
        Set<Plat> liste = ccc.getPlats();
        for (Plat p : liste) {
            total += p.getPrix();

        }
        return total;
    }

    @Override
    public void ModifierImc(Integer idclient) {
        Client client = clientRepo.findById(idclient).orElse(null);
        float totalCal= client.getPlats().stream().map(c-> c.getCalories()).reduce(0.0f,Float::sum);
        if(totalCal<2000){
            client.setImc(Imc.valueOf("FAIBLE"));
        } else if (totalCal==2000) {
            client.setImc(Imc.valueOf("IDEAL"));
        }else{
            client.setImc(Imc.valueOf("FORT"));
        }
        clientRepo.save(client);
    }

}
