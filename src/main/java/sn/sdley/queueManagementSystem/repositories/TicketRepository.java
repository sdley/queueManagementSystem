package sn.sdley.queueManagementSystem.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.sdley.queueManagementSystem.model.Ticket;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    /** Récupérer la liste des clients ayant pris un ticket pour un service donné
     * findClientsByService_Nom : Service_Nom et ServiceNom sont identiques
     * Ils se traduits par Service.Nom correspondant à la colonne nom dans la table Service
     * @param nomService
     * @return
     */
    List<Ticket> findClientsByServiceNom(String nomService);

    List<Ticket> findByServiceNomAndLocalisation(String nomService, String localisation);


    /**
     * // Récupérer la liste des tickets pour un service et une localisation
     *     List<Ticket> findByService_NomAndService_Localisations_Nom(String nomService,
     *                                                                String nomLocalisation);
     * Service_Localisations est une liste, donc il peut y avoir plusieurs localisations pour un service.
     * Spring Data JPA ne sait pas comment filtrer une seule localisation correctement dans une liste.
     * Utilisation d'une requête JPQL pour faire une jointure correcte entre Ticket, Service et Localisation.


    @Query("SELECT t FROM Ticket t JOIN t.service s JOIN s.localisations l WHERE
     s.nom = :nomService AND l.nom = :nomLocalisation")
    List<Ticket> findByServiceAndLocalisation(@Param("nomService") String nomService,
                                              @Param("nomLocalisation") String nomLocalisation);

    @Query("SELECT t FROM Ticket t WHERE t.service.nom = :nomService AND EXISTS
    (SELECT l FROM Localisation l WHERE l.service = t.service AND l.nom = :nomLocalisation)")
    List<Ticket> findByServiceAndLocalisation(@Param("nomService") String nomService,
                                              @Param("nomLocalisation") String nomLocalisation);
        */

    // Methode pour recuperer le dernier ticket pour un service donné
    @Query(value = "SELECT * FROM ticket WHERE nom_service = " +
            "(SELECT nom FROM service WHERE nom = :serviceName) ORDER BY id DESC LIMIT 1",
            nativeQuery = true)
    Optional<Ticket> findLastTicketByService(@Param("serviceName") String serviceName);

    // Methode pour recuperer le ticket en cours de traitement
    Ticket findByServiceNomAndLocalisationAndStatus(String serviceName, String location, String status);
}
