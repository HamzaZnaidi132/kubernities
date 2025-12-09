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
public class Cuisinier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private int idCuisinier;
    private String nom;
    private String prenom;

    @ManyToMany(mappedBy="cuisiniers", cascade = CascadeType.ALL)
    private Set<Plat> plats = new HashSet<>();
}
