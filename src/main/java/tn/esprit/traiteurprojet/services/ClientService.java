package tn.esprit.traiteurprojet.services;

import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import tn.esprit.traiteurprojet.entities.Client;
import tn.esprit.traiteurprojet.entities.Plat;
import tn.esprit.traiteurprojet.repositories.ClientRepo;

import java.util.Set;

public interface ClientService {
    public  Client ajouterClient(Client client);


    public float MontantApayerParClient(Integer idClient);
    public void ModifierImc(Integer idclient);



}
