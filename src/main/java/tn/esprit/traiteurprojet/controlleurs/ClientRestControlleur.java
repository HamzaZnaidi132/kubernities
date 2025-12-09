package tn.esprit.traiteurprojet.controlleurs;


import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.traiteurprojet.entities.Client;
import tn.esprit.traiteurprojet.entities.Plat;
import tn.esprit.traiteurprojet.services.ClientService;

@RestController
@RequestMapping("/client")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class ClientRestControlleur {
    private ClientService clientService;

    @PostMapping("/add-client")
    Client ajouterClient(@RequestBody Client client) {
        return clientService.ajouterClient(client);
    }


    @PostMapping("/total-prix/{id}")
    public float totalprix(@PathVariable("id") Integer p) {
        return clientService.MontantApayerParClient(p);

    }


    @PutMapping("/Modifier-Imc/{id}")
    public void ModifierImc(@PathVariable("id") Integer idClient) {
        clientService.ModifierImc(idClient);

    }
}
