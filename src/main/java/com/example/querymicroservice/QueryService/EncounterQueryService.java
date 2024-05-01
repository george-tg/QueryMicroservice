package com.example.querymicroservice.QueryService;


import com.example.querymicroservice.QueryRepository.EncounterRepository;
import com.example.querymicroservice.domain.Encounter;
import com.example.querymicroservice.domain.Patient;
import com.example.querymicroservice.dtos.EncounterDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EncounterQueryService
{
    private static final Logger logger = LoggerFactory.getLogger(EncounterQueryService.class);
    @Autowired
    private EncounterRepository repository;

    @KafkaListener(topics = "create_encounter_event", groupId = "encounter_group")
    public void handleCreateEncounterEvent(EncounterDTO encounterDTO) {
        Patient p = new Patient(encounterDTO.getPatientDTO().getId(),encounterDTO.getPatientDTO().getFirstName(),encounterDTO.getPatientDTO().getLastName(),encounterDTO.getPatientDTO().getAge());
        Encounter encounter = new Encounter(encounterDTO.getVisitDate(),p);
        repository.save(encounter);
    }
    @KafkaListener(topics = "update_encounter_event", groupId = "encounter_group")
    public void handleUpdateEncounterEvent(EncounterDTO encounterDTO)
    {
       Long id = encounterDTO.getId();
        Patient p = new Patient(encounterDTO.getPatientDTO().getId(),encounterDTO.getPatientDTO().getFirstName(),encounterDTO.getPatientDTO().getLastName(),encounterDTO.getPatientDTO().getAge());
        Encounter encounter = new Encounter(encounterDTO.getVisitDate(),p);
        Optional<Encounter> exisitionEncounter = repository.findById(id);
        if (exisitionEncounter.isPresent()) {
            encounter.setId(id);
            repository.save(encounter);
        } else {
            throw new RuntimeException("Can't update observation " + id);
        }
    }
    static EncounterDTO convertToDTO(Encounter encounter) {
        return new EncounterDTO(encounter.getId(),encounter.getVisitDate(), PatientQueryService.convertToDTO(encounter.getPatient()));
    }


    public EncounterDTO getEncounter(Long id) {

        Encounter encounter = repository.findById(id).orElse(null);

        if (encounter != null) {
            // Process the retrieved patient data
            EncounterDTO encounterDTO = convertToDTO(encounter);
            logger.info("Getting encounter: "+ encounterDTO.getId() );
            return encounterDTO;

        } else {

            System.out.println("Condition not found for ID: " + id);
            return null;
        }
    }

    @KafkaListener(topics = "delete_observation_event", groupId = "observation_group")
    public void handleDeleteEncounterEvent(Long id) {
        repository.deleteById(id);
    }
}
