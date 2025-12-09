package tn.esprit.traiteurprojet.services;

import tn.esprit.traiteurprojet.entities.Plat;

import java.util.List;

public interface PlatService {
    public void ajouterPlatAffecterClientEtCuisinier (Plat plat, Integer idClient, Integer idCuisinier);
    List<Plat> AfficherListePlatsParClient (String nom, String prenom);
}
