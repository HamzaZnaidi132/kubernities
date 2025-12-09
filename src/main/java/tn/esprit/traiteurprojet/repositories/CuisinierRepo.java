package tn.esprit.traiteurprojet.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.traiteurprojet.entities.Cuisinier;

public interface CuisinierRepo extends JpaRepository<Cuisinier,Integer> {
}
