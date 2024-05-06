package com.example.querymicroservice.QueryService;


import com.example.querymicroservice.KeycloakTokenExchangeService;
import com.example.querymicroservice.QueryRepository.EncounterRepository;
import com.example.querymicroservice.QueryRepository.ObservationRepository;
import com.example.querymicroservice.domain.Encounter;
import com.example.querymicroservice.domain.Observation;
import com.example.querymicroservice.domain.Patient;
import com.example.querymicroservice.dtos.EncounterDTO;
import com.example.querymicroservice.dtos.ObservationDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ObservationQueryService
{
    private static final Logger logger = LoggerFactory.getLogger(ObservationQueryService.class);
    @Autowired
    private ObservationRepository repository;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EncounterRepository encounterRepository;
    @Autowired
    private KeycloakTokenExchangeService keycloakTokenExchangeService;

    public ObservationDTO consumeReadEvent(Long id) {

        Observation observation = repository.findById(id).orElse(null);

        if (observation != null) {
            ObservationDTO observationDTO = convertToDTO(observation);
            logger.info("Getting observation: "+ observation.getId() );
            return observationDTO;

        } else {
            System.out.println("Condition not found for ID: " + id);
            return null;
        }
    }
    @KafkaListener(topics = "create_observation_event", groupId = "observation_group")
    public void handleCreateObservationEvent(String jsonPayload) {
        try {
            ObservationDTO observationDTO = objectMapper.readValue(jsonPayload, ObservationDTO.class);
            observationDTO.setAccessTokenUser(keycloakTokenExchangeService.getLimitedScopeToken(observationDTO.getAccessTokenUser(), "observation"));
            List<String> scopes = observationDTO.getAccessTokenUser().getScopes();
            if(scopes.size() == 1 && scopes.contains("observation")) {
                Patient p = new Patient(observationDTO.getPatientDTO().getId(),observationDTO.getPatientDTO().getFirstName(),observationDTO.getPatientDTO().getLastName(),observationDTO.getPatientDTO().getAge());
                Observation observation = new Observation(observationDTO.getType(),observationDTO.getValue(),p);
                observation.setEncounter(encounterRepository.findById(observationDTO.getEncounterId()).get());
                repository.save(observation);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing ObservationDTO to JSON while creating observation: " + e.getMessage(), e);
        }
    }
    @KafkaListener(topics = "update_observation_event", groupId = "observation_group")
    public void handleUpdateObservationEvent(ObservationDTO observationDTO)
    {
        Long id = observationDTO.getId();
        Patient p = new Patient(observationDTO.getPatientDTO().getId(),observationDTO.getPatientDTO().getFirstName(),observationDTO.getPatientDTO().getLastName(),observationDTO.getPatientDTO().getAge());
        Observation observation = new Observation(observationDTO.getType(), observationDTO.getValue(),p);
        Optional<Observation> existingCondition = repository.findById(id);
        if (existingCondition.isPresent()) {
            observation.setId(id);
            repository.save(observation);
        } else {
            throw new RuntimeException("Can't update observation " + id);
        }
    }
    @KafkaListener(topics = "delete_observation_event", groupId = "observation_group")
    public void handleDeleteObservationEvent(Long id) {
        repository.deleteById(id);
    }


    static ObservationDTO convertToDTO(Observation observation) {

        return new ObservationDTO(observation.getId(), observation.getType(), observation.getValue(), PatientQueryService.convertToDTO(observation.getPatient()), observation.getEncounter().getId());
    }
}
