package tn.esprit.traiteurprojet.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.esprit.traiteurprojet.entities.Plat;

public interface PlatRepo extends JpaRepository<Plat, Integer> {
}
