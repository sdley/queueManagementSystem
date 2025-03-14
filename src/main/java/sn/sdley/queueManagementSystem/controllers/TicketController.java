package sn.sdley.queueManagementSystem.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sn.sdley.queueManagementSystem.model.*;
import sn.sdley.queueManagementSystem.service.*;
import java.util.ArrayList;

@Controller
@RequestMapping("/ticket")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private LocalisationService localisationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private FileAttenteService fileAttenteService;

    // Constantes pour la génération de tickets
    private static final int START_NUMBER = 100;

    // Méthodes pour gérer les tickets
    @GetMapping("/ticketDetails")
    public String getTicketDetails(@RequestParam("ticketId") Long ticketId, Model model) {
        Ticket ticket = ticketService.getTicketById(ticketId);

        if (ticket == null) {
            model.addAttribute("error", "Ticket introuvable.");
            return "error";
        }

        // Numero qui est entrain d'etre traite
        Ticket ticketEnCours = ticketService
                .getTicketByServiceAndLocationAndStatus(ticket.getService().getNom(),
                        ticket.getLocalisation(), "En cours");

        if (ticketEnCours == null){
            model.addAttribute("numTicketEnCours",
                    "Le service n'a pas encore demarré !");
        } else {
            model.addAttribute("numTicketEnCours", ticketEnCours.getNumero());
        }

        model.addAttribute("ticket", ticket);
        return "ticketDetails";
    }

    // Generation de Ticket
    @PostMapping("/genererTicket")
    public String genererTicket(@RequestParam String nomService,
                                @RequestParam Long clientId,
                                @RequestParam String nomLocalisation,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        // Retrieve the service
        Service service = serviceService.getServiceByName(nomService);
        if (service == null) {
            model.addAttribute("error", "Service introuvable.");
            return "error";
        }

        if (nomLocalisation == null || nomLocalisation.trim().isEmpty()) {
            model.addAttribute("error",
                    "Veuillez sélectionner une localisation");
            return "error";
        }

        // Retrieve or create the queue for the service
        FileAttente fileAttente = fileAttenteService
                .getFileAttenteByServiceName(service.getNom());
        if (fileAttente == null) {
            fileAttente = new FileAttente();
            fileAttente.setService(service);
            fileAttente.setClients(new ArrayList<>()); // Initialize the clients list
            fileAttente = fileAttenteService.createFileAttente(fileAttente); // Persist it
        }

        // Clients du Service pour une localisation donnée v2
        int positionDansFile = ticketService
                .getTicketsByServiceNameAndLocalisationName(nomService,
                        nomLocalisation).size() + 1;
        int nombreDevant = ticketService
                .getTicketsByServiceNameAndLocalisationName(nomService,
                        nomLocalisation).size();

        // Create a new ticket
        Ticket ticket = new Ticket();
        ticket.setNumero(generateTicketNumber(nomService));
        ticket.setPositionDansFile(positionDansFile);
        ticket.setNombreDevant(nombreDevant);
        ticket.setService(service);
        ticket.setStatus("En attente");
        ticket.setLocalisation(nomLocalisation);

        // Retrieve the client
        Client client = clientService.getClientById(clientId);
        if (client == null) {
            model.addAttribute("error", "Client introuvable.");
            return "error";
        }
        ticket.setClient(client);

        // Add the client to the queue and update the database
        fileAttente.getClients().add(client);
        fileAttenteService.updateFileAttente(fileAttente.getId(), fileAttente);

        // Save the ticket
        ticketService.createTicket(ticket);

        // Redirect to ticket details
        redirectAttributes.addAttribute("ticketId", ticket.getId());
        return "redirect:/ticket/ticketDetails";
    }


    // Méthode pour générer un numéro de ticket unique
    /**
     * Definition d'une politique de génération de numéro de ticket:
     * 1. Génère un préfixe basé sur les trois premières lettres du service en majuscules.
     * 2. Assure un compteur unique par service en commençant à 101.
     * 3. Stocke les compteurs des services dans une Map<String, Integer>
     *     pour suivre le dernier numéro attribué.
     * Cela garantit que chaque service a son propre séquencement cohérent des tickets.
     * Par exemple, le premier client du service Sonatel obtiendra le ticket SON101.
     */
    private String generateTicketNumber(String serviceName) {
        // Extraire les trois premières lettres du service en majuscules
        String servicePrefix = serviceName.substring(0, Math.min(3, serviceName.length())).toUpperCase();

        // Récupérer le dernier ticket attribué pour ce service en base de données
        Integer lastNumber = ticketService.getLastTicketNumberByService(serviceName);

        // Si aucun ticket n'existe encore pour ce service, on commence à START_NUMBER (100)
        int newTicketNumber = (lastNumber != null) ? lastNumber + 1 : START_NUMBER;

        // Retourner le ticket formaté
        return servicePrefix + newTicketNumber;
    }


}
