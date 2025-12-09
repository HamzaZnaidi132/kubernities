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
public class Plat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private int idPlat;
    private String label;
    private Float prix;
    private Float calories;
    @Enumerated(EnumType.STRING)
    private Categorie categorie;

    @ManyToMany(cascade = CascadeType.ALL)
    private Set<Cuisinier> cuisiniers = new HashSet<>();

    @ManyToOne
    Client client;
}
