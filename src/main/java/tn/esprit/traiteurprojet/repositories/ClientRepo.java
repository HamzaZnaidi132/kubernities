package tn.esprit.traiteurprojet.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import tn.esprit.traiteurprojet.entities.Client;
@Repository
public interface ClientRepo extends JpaRepository<Client, Integer> {

    Client findByNomAndPrenom(String a, String b);
}
