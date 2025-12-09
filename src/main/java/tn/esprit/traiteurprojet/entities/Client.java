package tn.esprit.traiteurprojet.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private int idClient;
    private String nom;
    private String prenom;
    @Enumerated(EnumType.STRING)
    private Imc imc;

    @OneToMany(cascade = CascadeType.ALL, mappedBy="client")
    private Set<Plat> Plats = new HashSet<>();
}