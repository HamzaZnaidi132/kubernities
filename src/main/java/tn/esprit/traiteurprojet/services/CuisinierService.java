package tn.esprit.traiteurprojet.services;

import tn.esprit.traiteurprojet.entities.Cuisinier;
import tn.esprit.traiteurprojet.entities.Plat;

public interface CuisinierService {
    public Cuisinier ajouterCuisinier(Cuisinier cuisinier);
    public void AfficherListeCuisinier();
}
