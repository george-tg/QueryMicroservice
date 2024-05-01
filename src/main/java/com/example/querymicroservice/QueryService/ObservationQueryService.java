package com.example.querymicroservice.QueryService;


import com.example.querymicroservice.QueryRepository.ObservationRepository;
import com.example.querymicroservice.domain.Observation;
import com.example.querymicroservice.domain.Patient;
import com.example.querymicroservice.dtos.ObservationDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ObservationQueryService
{
    private static final Logger logger = LoggerFactory.getLogger(ObservationQueryService.class);
    @Autowired
    private ObservationRepository repository;

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
    public void handleCreateObservationEvent(ObservationDTO observationDTO) {
        Patient p = new Patient(observationDTO.getPatientDTO().getId(),observationDTO.getPatientDTO().getFirstName(),observationDTO.getPatientDTO().getLastName(),observationDTO.getPatientDTO().getAge());
        Observation observation = new Observation(observationDTO.getType(),observationDTO.getValue(),p);
        repository.save(observation);
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

        return new ObservationDTO(observation.getId().toString(), observation.getValue(), PatientQueryService.convertToDTO(observation.getPatient()));
    }
}
